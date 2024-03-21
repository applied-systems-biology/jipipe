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

package org.hkijena.jipipe.extensions.ijtrackmate.utils;

import fiji.plugin.trackmate.Logger;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.awt.*;

public class JIPipeLogger extends Logger {

    private final JIPipeProgressInfo progressInfo;

    public JIPipeLogger(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo.detachProgress();
        progressInfo.setProgress(0, 100);
    }

    @Override
    public void log(String message, Color color) {
        progressInfo.log(message);
    }

    @Override
    public void error(String message) {
        progressInfo.log("ERROR: " + message);
    }

    @Override
    public void setProgress(double val) {
        progressInfo.setProgress((int) (val * 100), 100);
    }

    @Override
    public void setStatus(String status) {
        progressInfo.log("STATUS: " + status);
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }
}
