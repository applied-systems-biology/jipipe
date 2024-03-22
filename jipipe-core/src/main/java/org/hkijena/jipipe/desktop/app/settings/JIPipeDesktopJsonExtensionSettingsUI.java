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

package org.hkijena.jipipe.desktop.app.settings;

import org.hkijena.jipipe.JIPipeJsonPlugin;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWorkbench;
import org.hkijena.jipipe.desktop.jsonextensionbuilder.JIPipeDesktopJsonExtensionWorkbenchPanel;
import org.hkijena.jipipe.extensions.parameters.library.markup.MarkdownText;

import java.awt.*;
import java.util.HashMap;

/**
 * Panel containing algorithm settings when algorithms are edited in a {@link JIPipeJsonPlugin}
 */
public class JIPipeDesktopJsonExtensionSettingsUI extends JIPipeDesktopJsonExtensionWorkbenchPanel {
    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeDesktopJsonExtensionSettingsUI(JIPipeDesktopJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        JIPipeDesktopParameterPanel metadataUI = new JIPipeDesktopParameterPanel(getExtensionWorkbenchUI(),
                getPluginProject(),
                MarkdownText.fromPluginResource("documentation/project-settings.md", new HashMap<>()),
                JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.WITH_SCROLLING | JIPipeDesktopParameterPanel.WITHOUT_LABEL_SEPARATION);
        add(metadataUI, BorderLayout.CENTER);
    }
}
