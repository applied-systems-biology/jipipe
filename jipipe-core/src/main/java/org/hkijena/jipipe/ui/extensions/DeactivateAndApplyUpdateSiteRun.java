/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.extensions;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.Installer;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.ui.ijupdater.ProgressAdapter;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

public class DeactivateAndApplyUpdateSiteRun implements JIPipeRunnable {

    private final FilesCollection filesCollection;
    private final List<UpdateSite> updateSites;
    private JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();

    public DeactivateAndApplyUpdateSiteRun(FilesCollection filesCollection, List<UpdateSite> updateSites) {
        this.filesCollection = filesCollection;
        this.updateSites = updateSites;
    }

    @Override
    public void run() {
        for (UpdateSite updateSite : updateSites) {
            filesCollection.deactivateUpdateSite(updateSite);
        }
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
        return "ImageJ updater: Deactivate site";
    }
}
