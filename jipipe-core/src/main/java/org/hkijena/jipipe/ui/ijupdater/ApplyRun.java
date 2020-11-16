package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;

public class ApplyRun implements JIPipeRunnable {

    private final FilesCollection filesCollection;
    private JIPipeProgressInfo info = new JIPipeProgressInfo();

    public ApplyRun(FilesCollection filesCollection) {
        this.filesCollection = filesCollection;
    }

    @Override
    public void run() {
        final Installer installer =
                new Installer(filesCollection, new ProgressAdapter(info));
        try {
            installer.start();
            filesCollection.write();
            info.log("Updated successfully.  Please restart ImageJ!");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            installer.done();
        }
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return info;
    }

    @Override
    public String getTaskLabel() {
        return "ImageJ updater: Apply";
    }

    public void setInfo(JIPipeProgressInfo info) {
        this.info = info;
    }
}
