package org.hkijena.acaq5.api.testbench;

import org.hkijena.acaq5.api.ACAQRunSettings;
import org.hkijena.acaq5.api.algorithm.ACAQGraphNode;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Used by the quick run to backup output data
 */
public class ACAQTestbenchSnapshot {
    private ACAQTestBench testBench;
    private Path outputFolderBackup;
    private LocalDateTime timestamp;
    private String label;
    private Map<ACAQGraphNode, AlgorithmBackup> algorithmBackups = new HashMap<>();

    /**
     * @param testBench the testbench
     */
    public ACAQTestbenchSnapshot(ACAQTestBench testBench) {
        this.testBench = testBench;
        this.outputFolderBackup = testBench.getTestBenchRun().getConfiguration().getOutputPath();
        timestamp = LocalDateTime.now();
        for (ACAQGraphNode algorithm : testBench.getTestBenchRun().getGraph().traverseAlgorithms()) {
            algorithmBackups.put(algorithm, new AlgorithmBackup(algorithm));
        }
    }

    /**
     * Restores the snapshot
     */
    public void restore() {
        ((ACAQRunSettings) this.testBench.getTestBenchRun().getConfiguration()).setOutputPath(outputFolderBackup);
        restore(testBench.getBenchedAlgorithm());
        ACAQTestbenchSnapshot initial = testBench.getInitialBackup();
        if (this != initial) {
            for (Map.Entry<ACAQGraphNode, AlgorithmBackup> entry : initial.algorithmBackups.entrySet()) {
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
    public void restore(ACAQGraphNode algorithm) {
        algorithmBackups.get(algorithm).restore();
    }

    /**
     * @param runAlgorithm algorithm in the {@link org.hkijena.acaq5.api.ACAQRun}
     * @return Backup for the algorithm
     */
    public AlgorithmBackup getAlgorithmBackup(ACAQGraphNode runAlgorithm) {
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
        private ACAQGraphNode algorithm;
        private Map<String, Object> parameterBackups = new HashMap<>();
        private Map<String, Path> storagePathBackups = new HashMap<>();

        /**
         * @param algorithm the algorithm the backup is created from
         */
        public AlgorithmBackup(ACAQGraphNode algorithm) {
            this.algorithm = algorithm;
            backupData();
            backupParameters();
        }

        private void backupParameters() {
            Map<String, ACAQParameterAccess> parameters = ACAQParameterTree.getParameters(algorithm);
            for (Map.Entry<String, ACAQParameterAccess> entry : parameters.entrySet()) {
                parameterBackups.put(entry.getKey(), entry.getValue().get(Object.class));
            }
        }

        private void backupData() {
            for (ACAQDataSlot slot : algorithm.getOutputSlots()) {
                storagePathBackups.put(slot.getName(), slot.getStoragePath());
            }
        }

        /**
         * Restores the backup to the input algorithm
         */
        public void restore() {
            for (ACAQDataSlot slot : algorithm.getOutputSlots()) {
                slot.setStoragePath(storagePathBackups.get(slot.getName()));
            }
            restoreParameters(algorithm);
        }

        /**
         * Restores the parameter to a compatible foreign algorithm
         *
         * @param targetAlgorithm the target algorithm
         */
        public void restoreParameters(ACAQGraphNode targetAlgorithm) {
            Map<String, ACAQParameterAccess> parameters = ACAQParameterTree.getParameters(targetAlgorithm);
            for (Map.Entry<String, ACAQParameterAccess> entry : parameters.entrySet()) {
                entry.getValue().set(parameterBackups.get(entry.getKey()));
            }

            // Developers might "forget" to add the events. Trigger a structural event to force the panels to reload
            targetAlgorithm.getEventBus().post(new ParameterStructureChangedEvent(targetAlgorithm));
        }
    }
}
