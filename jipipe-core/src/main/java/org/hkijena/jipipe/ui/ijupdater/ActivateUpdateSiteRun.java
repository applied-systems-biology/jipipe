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

package org.hkijena.jipipe.ui.ijupdater;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeRunnerStatus;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ActivateUpdateSiteRun implements JIPipeRunnable {

    private final FilesCollection filesCollection;
    private final List<UpdateSite> updateSites;

    public ActivateUpdateSiteRun(FilesCollection filesCollection, List<UpdateSite> updateSites) {
        this.filesCollection = filesCollection;
        this.updateSites = updateSites;
    }

    @Override
    public void run(Consumer<JIPipeRunnerStatus> onProgress, Supplier<Boolean> isCancelled) {
        try {
            for (UpdateSite updateSite : updateSites) {
                filesCollection.activateUpdateSite(updateSite, new ProgressAdapter(onProgress));
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
