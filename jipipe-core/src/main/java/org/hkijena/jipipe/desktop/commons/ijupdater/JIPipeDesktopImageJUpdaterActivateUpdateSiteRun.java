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
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.api.AbstractJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.run.JIPipeRunnable;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

public class JIPipeDesktopImageJUpdaterActivateUpdateSiteRun extends AbstractJIPipeRunnable {

    private final FilesCollection filesCollection;
    private final List<UpdateSite> updateSites;

    public JIPipeDesktopImageJUpdaterActivateUpdateSiteRun(FilesCollection filesCollection, List<UpdateSite> updateSites) {
        this.filesCollection = filesCollection;
        this.updateSites = updateSites;
    }

    @Override
    public void run() {
        JIPipeProgressInfo progressInfo = getProgressInfo();
        try {
            for (UpdateSite updateSite : updateSites) {
                filesCollection.activateUpdateSite(updateSite, new JIPipeDesktopImageJUpdaterProgressAdapter2(progressInfo));
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getTaskLabel() {
        return "ImageJ updater: Activate site";
    }
}
