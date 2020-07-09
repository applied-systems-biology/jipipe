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

package org.hkijena.pipelinej.api;

/**
 * Status of a runner
 */
public class ACAQRunnerStatus {
    private int progress;
    private int maxProgress;
    private String message;

    /**
     * @param progress    Progress
     * @param maxProgress Maximum progress
     * @param message     Message
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
