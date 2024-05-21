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

package org.hkijena.jipipe.desktop.commons.ijupdater;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.run.JIPipeRunnable;

public class JIPipeDesktopImageJUpdaterApplyRun extends AbstractJIPipeRunnable {

    private final FilesCollection filesCollection;

    public JIPipeDesktopImageJUpdaterApplyRun(FilesCollection filesCollection) {
        this.filesCollection = filesCollection;
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        final Installer installer =
                new Installer(filesCollection, new JIPipeDesktopImageJUpdaterProgressAdapter2(progressInfo));
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
    public String getTaskLabel() {
        return "ImageJ updater: Apply";
    }
}
