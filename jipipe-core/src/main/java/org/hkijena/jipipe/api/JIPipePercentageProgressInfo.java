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

/**
 * A {@link JIPipeProgressInfo} that logs the progress of 0 to 100 percent.
 * Only reports integer changes in percentage.
 */
public class JIPipePercentageProgressInfo extends JIPipeProgressInfo {

    private int lastPercentage;

    public JIPipePercentageProgressInfo() {
    }

    public JIPipePercentageProgressInfo(JIPipeProgressInfo other) {
        super(other);
    }

    public void logPercentage(double value, double max) {
        int currentPercentage = (int) (value / max * 100);
        if (currentPercentage != lastPercentage) {
            log(currentPercentage + "%");
            lastPercentage = currentPercentage;
        }
    }
}
