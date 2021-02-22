package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;

public class ApplyRun implements JIPipeRunnable {

    private final FilesCollection filesCollection;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public ApplyRun(FilesCollection filesCollection) {
        this.filesCollection = filesCollection;
    }

    @Override
    public void run() {
        final Installer installer =
                new Installer(filesCollection, new ProgressAdapter(progressInfo));
        try {
            installer.start();
            filesCollection.write();
            progressInfo.log("Updated successfully.  Please restart ImageJ!");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            installer.done();
        }
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void setProgressInfo(JIPipeProgressInfo progressInfo) {
        this.progressInfo = progressInfo;
    }

    @Override
    public String getTaskLabel() {
        return "ImageJ updater: Apply";
    }
}
