/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.progress.JIPipeLogLevel;

/**
 * A {@link JIPipeProgressInfo} that logs the progress of 0 to 100 percent.
 * Only reports integer changes in percentage: each threshold pass writes a progress record
 * (percentage value, maximum 100) into the node's store and the compatibility percentage line
 * (an INFO log record with the percentage as message) for the node's chain.
 */
public class JIPipePercentageProgressInfo extends JIPipeProgressInfo {

    private int lastPercentage;
    private long lastTime;
    private long notifyInterval = 1000;

    public JIPipePercentageProgressInfo() {
    }

    public JIPipePercentageProgressInfo(JIPipeProgressInfo other) {
        super(other);
    }

    public long getNotifyInterval() {
        return notifyInterval;
    }

    public void setNotifyInterval(long notifyInterval) {
        this.notifyInterval = notifyInterval;
    }

    public void logPercentage(double value, double max) {


        int currentPercentage = (int) (value / max * 100);
        if (currentPercentage != lastPercentage) {
            long time = System.currentTimeMillis();
            lastPercentage = currentPercentage;
            if (time - lastTime > notifyInterval || currentPercentage == 50 || currentPercentage == 100) {
                // Compatibility percentage line: an INFO log record with the percentage as message
                // (the pre-store text mechanism, restored by ruling R15). Emitted before the
                // progress record so the rendered prefix shows the progress as of before the report
                log(JIPipeLogLevel.INFO, currentPercentage + "%");
                recordProgress(currentPercentage, 100);
                lastTime = time;
            }
        }
    }
}
