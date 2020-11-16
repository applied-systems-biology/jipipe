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

import net.imagej.ui.swing.updater.SwingAuthenticator;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.util.AvailableSites;
import net.imagej.updater.util.UpdaterUtil;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeRunnable;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.net.Authenticator;

/**
 * A run that is used for the updater
 */
public class RefreshRepositoryRun implements JIPipeRunnable {

    private JIPipeProgressInfo info = new JIPipeProgressInfo();
    private FilesCollection filesCollection;
    private String conflictWarnings;

    @Override
    public void run() {
        UpdaterUtil.useSystemProxies();
        Authenticator.setDefault(new SwingAuthenticator());

        filesCollection = new FilesCollection(JIPipeImageJPluginManager.getImageJRoot().toFile());
        AvailableSites.initializeAndAddSites(filesCollection);

        // Look for conflicts
        try {
            conflictWarnings = filesCollection.downloadIndexAndChecksum(new ProgressAdapter(info));
        } catch (ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }

        filesCollection.markForUpdate(false);
    }

    public FilesCollection getFilesCollection() {
        return filesCollection;
    }

    public String getConflictWarnings() {
        return conflictWarnings;
    }

    @Override
    public JIPipeProgressInfo getProgressInfo() {
        return info;
    }

    @Override
    public String getTaskLabel() {
        return "ImageJ updater: Refresh";
    }

    public void setInfo(JIPipeProgressInfo info) {
        this.info = info;
    }
}
