package org.hkijena.acaq5.api;

public class ACAQRunnerStatus {
    private int progress;
    private int maxProgress;
    private String message;

    public ACAQRunnerStatus(int progress, int maxProgress, String message) {
        this.progress = progress;
        this.maxProgress = maxProgress;
        this.message = message;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public String getMessage() {
        return message;
    }
}
