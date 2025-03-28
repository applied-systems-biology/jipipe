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

package org.hkijena.jipipe.plugins.tables.display;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class CacheAwareOpenResultsTableInJIPipeDataDisplayOperation implements JIPipeDesktopDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        TableViewerLegacyCacheDataViewerWindow window = new TableViewerLegacyCacheDataViewerWindow(desktopWorkbench, (JIPipeDataTableDataSource) source, displayName, false);
        window.setVisible(true);
    }

    @Override
    public String getId() {
        return "jipipe:open-table-in-jipipe-window";
    }

    @Override
    public String getName() {
        return "Open in JIPipe (new window)";
    }

    @Override
    public String getDescription() {
        return "Opens the table in a new window inside JIPipe. The viewer is aware of the cache.";
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/jipipe.png");
    }
}
