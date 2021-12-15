package org.hkijena.jipipe.api;

/**
 * A {@link JIPipeProgressInfo} that logs the progress of 0 to 100 percent.
 * Only reports integer changes in percentage.
 */
public class JIPipePercentageProgressInfo extends JIPipeProgressInfo{

    private int lastPercentage;

    public JIPipePercentageProgressInfo() {
    }

    public JIPipePercentageProgressInfo(JIPipeProgressInfo other) {
        super(other);
    }

    public void logPercentage(double value, double max) {
        int currentPercentage = (int)(value / max * 100);
        if(currentPercentage != lastPercentage) {
            log(currentPercentage + "%");
            lastPercentage = currentPercentage;
        }
    }
}
