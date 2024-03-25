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

package org.hkijena.jipipe.plugins.utils.display;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.filesystem.dataypes.PathData;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

public class ImportJIPipeProjectDataOperation implements JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        Path folderPath = ((PathData) data).toPath();
        JIPipeDesktopProjectWindow window = (JIPipeDesktopProjectWindow) desktopWorkbench.getWindow();
        window.openProject(folderPath, false);
    }

    @Override
    public String getId() {
        return "jipipe:open-analysis-output-in-jipipe";
    }

    @Override
    public String getName() {
        return "Open analysis output in JIPipe";
    }

    @Override
    public String getDescription() {
        return "Opens the output in the JIPipe GUI";
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/albumfolder-importdir.png");
    }
}
