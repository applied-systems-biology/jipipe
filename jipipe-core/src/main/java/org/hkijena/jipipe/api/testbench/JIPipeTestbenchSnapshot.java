/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.testbench;

import org.hkijena.jipipe.api.JIPipeRunSettings;
import org.hkijena.jipipe.api.algorithm.JIPipeGraphNode;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.events.ParameterStructureChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Used by the quick run to backup output data
 */
public class JIPipeTestbenchSnapshot {
    private JIPipeTestBench testBench;
    private Path outputFolderBackup;
    private LocalDateTime timestamp;
    private String label;
    private Map<JIPipeGraphNode, AlgorithmBackup> algorithmBackups = new HashMap<>();

    /**
     * @param testBench the testbench
     */
    public JIPipeTestbenchSnapshot(JIPipeTestBench testBench) {
        this.testBench = testBench;
        this.outputFolderBackup = testBench.getTestBenchRun().getConfiguration().getOutputPath();
        timestamp = LocalDateTime.now();
        for (JIPipeGraphNode algorithm : testBench.getTestBenchRun().getGraph().traverseAlgorithms()) {
            algorithmBackups.put(algorithm, new AlgorithmBackup(algorithm));
        }
    }

    /**
     * Restores the snapshot
     */
    public void restore() {
        ((JIPipeRunSettings) this.testBench.getTestBenchRun().getConfiguration()).setOutputPath(outputFolderBackup);
        restore(testBench.getBenchedAlgorithm());
        JIPipeTestbenchSnapshot initial = testBench.getInitialBackup();
        if (this != initial) {
            for (Map.Entry<JIPipeGraphNode, AlgorithmBackup> entry : initial.algorithmBackups.entrySet()) {
                if (entry.getKey() != testBench.getBenchedAlgorithm())
                    entry.getValue().restore();
            }
        }
    }

    /**
     * Restores the snapshot into the target algorithm
     *
     * @param algorithm the target algorithm
     */
    public void restore(JIPipeGraphNode algorithm) {
        algorithmBackups.get(algorithm).restore();
    }

    /**
     * @param runAlgorithm algorithm in the {@link org.hkijena.jipipe.api.JIPipeRun}
     * @return Backup for the algorithm
     */
    public AlgorithmBackup getAlgorithmBackup(JIPipeGraphNode runAlgorithm) {
        return algorithmBackups.get(runAlgorithm);
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Backups parameters and storage paths
     */
    public static class AlgorithmBackup {
        private JIPipeGraphNode algorithm;
        private Map<String, Object> parameterBackups = new HashMap<>();
        private Map<String, Path> storagePathBackups = new HashMap<>();

        /**
         * @param algorithm the algorithm the backup is created from
         */
        public AlgorithmBackup(JIPipeGraphNode algorithm) {
            this.algorithm = algorithm;
            backupData();
            backupParameters();
        }

        private void backupParameters() {
            Map<String, JIPipeParameterAccess> parameters = JIPipeParameterTree.getParameters(algorithm);
            for (Map.Entry<String, JIPipeParameterAccess> entry : parameters.entrySet()) {
                parameterBackups.put(entry.getKey(), entry.getValue().get(Object.class));
            }
        }

        private void backupData() {
            for (JIPipeDataSlot slot : algorithm.getOutputSlots()) {
                storagePathBackups.put(slot.getName(), slot.getStoragePath());
            }
        }

        /**
         * Restores the backup to the input algorithm
         */
        public void restore() {
            for (JIPipeDataSlot slot : algorithm.getOutputSlots()) {
                slot.setStoragePath(storagePathBackups.get(slot.getName()));
            }
            restoreParameters(algorithm);
        }

        /**
         * Restores the parameter to a compatible foreign algorithm
         *
         * @param targetAlgorithm the target algorithm
         */
        public void restoreParameters(JIPipeGraphNode targetAlgorithm) {
            Map<String, JIPipeParameterAccess> parameters = JIPipeParameterTree.getParameters(targetAlgorithm);
            for (Map.Entry<String, JIPipeParameterAccess> entry : parameters.entrySet()) {
                entry.getValue().set(parameterBackups.get(entry.getKey()));
            }

            // Developers might "forget" to add the events. Trigger a structural event to force the panels to reload
            targetAlgorithm.getEventBus().post(new ParameterStructureChangedEvent(targetAlgorithm));
        }
    }
}
