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
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.display.trackscheme;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.extensions.ijtrackmate.TrackMateExtension;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import javax.swing.*;

public class TrackSchemeDataDisplayOperation implements JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        TrackSchemeCachedDataViewerWindow window = new TrackSchemeCachedDataViewerWindow(workbench, JIPipeDataTableDataSource.wrap(data, source), displayName);
        window.setVisible(true);
    }

    @Override
    public String getId() {
        return "trackmate-track-scheme";
    }

    @Override
    public String getName() {
        return "Track Scheme";
    }

    @Override
    public String getDescription() {
        return "Displays the track scheme of the tracks";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return TrackMateExtension.RESOURCES.getIconFromResources("trackscheme.png");
    }
}
