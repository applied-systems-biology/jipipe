package org.hkijena.acaq5.api.testbench;

import org.hkijena.acaq5.api.ACAQMutableRunConfiguration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQTraversedParameterCollection;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Used by the test bench to backup output data
 */
public class ACAQTestbenchSnapshot {
    private ACAQTestbench testbench;
    private Path outputFolderBackup;
    private LocalDateTime timestamp;
    private String label;
    private Map<ACAQAlgorithm, AlgorithmBackup> algorithmBackups = new HashMap<>();

    /**
     * @param testbench the testbench
     */
    public ACAQTestbenchSnapshot(ACAQTestbench testbench) {
        this.testbench = testbench;
        this.outputFolderBackup = testbench.getTestbenchRun().getConfiguration().getOutputPath();
        timestamp = LocalDateTime.now();
        for (ACAQAlgorithm algorithm : testbench.getTestbenchRun().getGraph().traverseAlgorithms()) {
            algorithmBackups.put(algorithm, new AlgorithmBackup(algorithm));
        }
    }

    /**
     * Restores the snapshot
     */
    public void restore() {
        ((ACAQMutableRunConfiguration) this.testbench.getTestbenchRun().getConfiguration()).setOutputPath(outputFolderBackup);
        restore(testbench.getBenchedAlgorithm());
        ACAQTestbenchSnapshot initial = testbench.getInitialBackup();
        if (this != initial) {
            for (Map.Entry<ACAQAlgorithm, AlgorithmBackup> entry : initial.algorithmBackups.entrySet()) {
                if (entry.getKey() != testbench.getBenchedAlgorithm())
                    entry.getValue().restore();
            }
        }
    }

    /**
     * Restores the snapshot into the target algorithm
     *
     * @param algorithm the target algorithm
     */
    public void restore(ACAQAlgorithm algorithm) {
        algorithmBackups.get(algorithm).restore();
    }

    /**
     * @param runAlgorithm algorithm in the {@link org.hkijena.acaq5.api.ACAQRun}
     * @return Backup for the algorithm
     */
    public AlgorithmBackup getAlgorithmBackup(ACAQAlgorithm runAlgorithm) {
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
        private ACAQAlgorithm algorithm;
        private Map<String, Object> parameterBackups = new HashMap<>();
        private Map<String, Path> storagePathBackups = new HashMap<>();

        /**
         * @param algorithm the algorithm the backup is created from
         */
        public AlgorithmBackup(ACAQAlgorithm algorithm) {
            this.algorithm = algorithm;
            backupData();
            backupParameters();
        }

        private void backupParameters() {
            Map<String, ACAQParameterAccess> parameters = ACAQTraversedParameterCollection.getParameters(algorithm);
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
        public void restoreParameters(ACAQAlgorithm targetAlgorithm) {
            Map<String, ACAQParameterAccess> parameters = ACAQTraversedParameterCollection.getParameters(targetAlgorithm);
            for (Map.Entry<String, ACAQParameterAccess> entry : parameters.entrySet()) {
                entry.getValue().set(parameterBackups.get(entry.getKey()));
            }
        }
    }
}
