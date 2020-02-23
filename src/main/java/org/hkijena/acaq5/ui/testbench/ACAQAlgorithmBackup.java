package org.hkijena.acaq5.ui.testbench;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Used by the test bench to backup output data
 */
public class ACAQAlgorithmBackup {
    private ACAQAlgorithm algorithm;
    private Map<String, Object> parameterBackups = new HashMap<>();
    private Map<String, Path> storagePathBackups = new HashMap<>();
    private LocalDateTime timestamp;

    public ACAQAlgorithmBackup(ACAQAlgorithm algorithm) {
        this.algorithm = algorithm;
        backupData();
        backupParameters();
        timestamp = LocalDateTime.now();
    }

    private void backupParameters() {
        Map<String, ACAQParameterAccess> parameters = ACAQParameterAccess.getParameters(algorithm);
        for (Map.Entry<String, ACAQParameterAccess> entry : parameters.entrySet()) {
            parameterBackups.put(entry.getKey(), entry.getValue().get());
        }
    }

    private void backupData() {
        for(ACAQDataSlot<?> slot : algorithm.getOutputSlots()) {
            storagePathBackups.put(slot.getName(), slot.getStoragePath());
        }
    }

    public void restore(ACAQAlgorithm targetAlgorithm) {
        for(ACAQDataSlot<?> slot : targetAlgorithm.getOutputSlots()) {
            slot.setStoragePath(storagePathBackups.get(slot.getName()));
        }
        Map<String, ACAQParameterAccess> parameters = ACAQParameterAccess.getParameters(targetAlgorithm);
        for (Map.Entry<String, ACAQParameterAccess> entry : parameters.entrySet()) {
            entry.getValue().set(parameterBackups.get(entry.getKey()));
        }
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
