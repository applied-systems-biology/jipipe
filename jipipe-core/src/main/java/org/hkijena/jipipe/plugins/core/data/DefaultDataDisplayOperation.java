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

package org.hkijena.jipipe.plugins.core.data;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.browser.JIPipeLocalDataBrowser;
import org.hkijena.jipipe.api.data.browser.JIPipeLocalDataTableBrowser;
import org.hkijena.jipipe.api.data.sources.JIPipeDataTableDataSource;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewerWindow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;

public class DefaultDataDisplayOperation implements JIPipeDesktopDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
//        data.display(displayName, desktopWorkbench, source);
        JIPipeDesktopDataViewerWindow window = new JIPipeDesktopDataViewerWindow(desktopWorkbench);
        window.setLocationRelativeTo(desktopWorkbench.getWindow());
        window.setVisible(true);
        if(source instanceof JIPipeDataTableDataSource) {
            JIPipeDataTableDataSource dataSource = (JIPipeDataTableDataSource) source;
            window.browseDataTable(new JIPipeLocalDataTableBrowser(dataSource.getDataTable()), dataSource.getRow(), dataSource.getDataAnnotation(), displayName);
        }
        else {
            window.browseData(new JIPipeLocalDataBrowser(new JIPipeDataItemStore(data)), displayName);
        }
    }

    @Override
    public boolean isIncludeRowInDisplayName() {
        return false;
    }

    @Override
    public String getId() {
        return "jipipe:show";
    }

    @Override
    public String getName() {
        return "Show";
    }

    @Override
    public String getDescription() {
        return "Opens the default data viewer";
    }

    @Override
    public int getOrder() {
        return -1000;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/zoom.png");
    }
}
