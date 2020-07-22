package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeRunnerStatus;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ApplyRun implements JIPipeRunnable {

    private final FilesCollection filesCollection;

    public ApplyRun(FilesCollection filesCollection) {
        this.filesCollection = filesCollection;
    }

    @Override
    public void run(Consumer<JIPipeRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        final Installer installer =
                new Installer(filesCollection, new ProgressAdapter(onProgress));
        try {
            installer.start();
            filesCollection.write();
            onProgress.accept(new JIPipeRunnerStatus(0, 0, "Updated successfully.  Please restart ImageJ!"));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            installer.done();
        }
    }
}
