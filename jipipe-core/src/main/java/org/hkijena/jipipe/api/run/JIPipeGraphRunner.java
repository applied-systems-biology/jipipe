package org.hkijena.jipipe.api.run;

import org.hkijena.jipipe.api.AbstractJIPipeRunnable;

public class JIPipeGraphRunner extends AbstractJIPipeRunnable {
    @Override
    public String getTaskLabel() {
        return "Pipeline run";
    }

    @Override
    public void run() {
        setProgressInfo(getProgressInfo().resolve("〈⚙〉"));


    }
}
