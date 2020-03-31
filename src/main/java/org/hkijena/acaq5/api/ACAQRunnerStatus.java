package org.hkijena.acaq5.api;

/**
 * Status of a runner
 */
public class ACAQRunnerStatus {
    private int progress;
    private int maxProgress;
    private String message;

    /**
     * @param progress Progress
     * @param maxProgress Maximum progress
     * @param message Message
     */
    public ACAQRunnerStatus(int progress, int maxProgress, String message) {
        this.progress = progress;
        this.maxProgress = maxProgress;
        this.message = message;
    }

    /**
     * @return The current progress
     */
    public int getProgress() {
        return progress;
    }

    /**
     * @return The maximum progress
     */
    public int getMaxProgress() {
        return maxProgress;
    }

    /**
     * @return The message
     */
    public String getMessage() {
        return message;
    }
}
