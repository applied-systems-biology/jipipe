package org.hkijena.jipipe.api;

/**
 * Abstract base class for implementing {@link JIPipeRunnable}
 */
public abstract class AbstractJIPipeRunnable implements JIPipeRunnable {
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }
}
