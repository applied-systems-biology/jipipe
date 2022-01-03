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

package org.hkijena.jipipe.ui.settings;

import org.hkijena.jipipe.ui.JIPipeJsonExtensionWorkbench;
import org.hkijena.jipipe.ui.JIPipeJsonExtensionWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;

import java.awt.BorderLayout;
import java.util.HashMap;

/**
 * Panel containing algorithm settings when algorithms are edited in a {@link org.hkijena.jipipe.JIPipeJsonExtension}
 */
public class JIPipeJsonExtensionSettingsUI extends JIPipeJsonExtensionWorkbenchPanel {
    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeJsonExtensionSettingsUI(JIPipeJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ParameterPanel metadataUI = new ParameterPanel(getExtensionWorkbenchUI(),
                getProject(),
                MarkdownDocument.fromPluginResource("documentation/project-settings.md", new HashMap<>()),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING | ParameterPanel.WITHOUT_LABEL_SEPARATION);
        add(metadataUI, BorderLayout.CENTER);
    }
}
