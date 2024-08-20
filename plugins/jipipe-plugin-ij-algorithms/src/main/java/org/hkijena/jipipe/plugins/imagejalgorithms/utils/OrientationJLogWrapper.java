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

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.StringUtils;
import orientation.LogAbstract;

public class OrientationJLogWrapper implements LogAbstract {

    private final JIPipeProgressInfo progressInfo;
    private String message;
    private int currentPercentage = 0;
    private long lastReportedTime = 0;

    public OrientationJLogWrapper(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public void progress(String msg, int value) {
        message = msg;
        currentPercentage = value;
        sendProgressIfNeeded();
    }

    @Override
    public void increment(double inc) {
        ++currentPercentage;
        sendProgressIfNeeded();
    }

    @Override
    public void setValue(int value) {
        currentPercentage = value;
        sendProgressIfNeeded();
    }

    @Override
    public void setMessage(String msg) {
        message = msg;
        sendProgressIfNeeded();
    }

    @Override
    public void progress(String msg, double value) {
        progress(msg, (int) value);
    }

    @Override
    public void reset() {
        currentPercentage = 0;
    }

    @Override
    public void finish() {
        message = "Finished";
        currentPercentage = 100;
        sendProgressIfNeeded();
        reset();
    }

    @Override
    public void finish(String msg) {
        message = msg;
        currentPercentage = 100;
        sendProgressIfNeeded();
        reset();
    }

    private void sendProgressIfNeeded() {
        long now = System.currentTimeMillis();
        long difference = now - lastReportedTime;
        if (difference > 1000) {
            progressInfo.log("[" + Math.max(0, currentPercentage) + "%] " + StringUtils.nullToEmpty(message));
            lastReportedTime = now;
        }
    }
}
