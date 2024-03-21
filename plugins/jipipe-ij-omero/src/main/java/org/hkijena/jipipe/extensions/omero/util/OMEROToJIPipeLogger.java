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

package org.hkijena.jipipe.extensions.omero.util;

import omero.log.LogMessage;
import omero.log.Logger;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

public class OMEROToJIPipeLogger implements Logger {

    private final JIPipeProgressInfo progressInfo;

    public OMEROToJIPipeLogger(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public void debug(Object o, String s) {
        progressInfo.log(s);
    }

    @Override
    public void debug(Object o, LogMessage logMessage) {
        progressInfo.log(logMessage.toString());
    }

    @Override
    public void info(Object o, String s) {
        progressInfo.log(s);
    }

    @Override
    public void info(Object o, LogMessage logMessage) {
        progressInfo.log(logMessage.toString());
    }

    @Override
    public void warn(Object o, String s) {
        progressInfo.log(s);
    }

    @Override
    public void warn(Object o, LogMessage logMessage) {
        progressInfo.log(logMessage.toString());
    }

    @Override
    public void error(Object o, String s) {
        progressInfo.log(s);
    }

    @Override
    public void error(Object o, LogMessage logMessage) {
        progressInfo.log(logMessage.toString());
    }

    @Override
    public void fatal(Object o, String s) {
        progressInfo.log(s);
    }

    @Override
    public void fatal(Object o, LogMessage logMessage) {
        progressInfo.log(logMessage.toString());
    }

    @Override
    public String getLogFile() {
        return null;
    }
}
