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

package org.hkijena.jipipe.desktop.app.grapheditor.addnodepanel;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;

/**
 * A synthetic data class that the {@link NodeBrowserModelTest} registers
 * with a stub {@code InputProfileProvider} that derives
 * {@code PLANE_2D} for it. It is a plain marker type: nothing ever
 * instantiates it, so {@link #duplicate(JIPipeProgressInfo)} and
 * {@link #exportData(JIPipeWriteDataStorage, String, boolean, JIPipeProgressInfo)}
 * are never expected to do real work.
 */
@SetJIPipeDocumentation(name = "Browser Test Plane Data", description = "Synthetic data type of the node browser model test")
public class NodeBrowserModelTestPlaneData implements JIPipeData {
    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        // No-op: synthetic marker type, never exported
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return this;
    }
}
