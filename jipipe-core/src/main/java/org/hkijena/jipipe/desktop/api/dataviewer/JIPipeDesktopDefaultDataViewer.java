package org.hkijena.jipipe.desktop.api.dataviewer;

import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFancyReadOnlyTextArea;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFancyReadOnlyTextField;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopDefaultDataViewer extends JIPipeDesktopDataViewer {

    private final JIPipeDesktopFancyReadOnlyTextField stringField = new JIPipeDesktopFancyReadOnlyTextField(LOADING_PLACEHOLDER_TEXT, false);
    private final JIPipeDesktopFancyReadOnlyTextArea detailedStringField = new JIPipeDesktopFancyReadOnlyTextArea(LOADING_PLACEHOLDER_TEXT, false);

    public JIPipeDesktopDefaultDataViewer(JIPipeDesktopDataViewerWindow dataViewerWindow) {
        super(dataViewerWindow);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);
        formPanel.addToForm(stringField, new JLabel("String representation"));
        formPanel.addToForm(detailedStringField, new JLabel("String representation (detailed)"));
        add(formPanel, BorderLayout.CENTER);
    }

    @Override
    public void rebuildDock(JIPipeDesktopDockPanel dockPanel) {
        if (getDataBrowser() == null) {
            showError(dockPanel, "No data to display", "There is currently no data source");
        } else {
            showError(dockPanel, "Unable to display data", "The viewer does not know how to display this data");
            awaitToSwing(getDataBrowser().getDataAsString(), stringField::setText);
            awaitToSwing(getDataBrowser().getDataAsDetailedString(), detailedStringField::setText);
        }
    }
}
