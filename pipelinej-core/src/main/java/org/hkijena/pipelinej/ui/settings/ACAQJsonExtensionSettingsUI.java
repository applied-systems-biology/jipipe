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

package org.hkijena.pipelinej.ui.settings;

import org.hkijena.pipelinej.ui.ACAQJsonExtensionWorkbench;
import org.hkijena.pipelinej.ui.ACAQJsonExtensionWorkbenchPanel;
import org.hkijena.pipelinej.ui.components.MarkdownDocument;
import org.hkijena.pipelinej.ui.parameters.ParameterPanel;

import java.awt.*;

/**
 * Panel containing algorithm settings when algorithms are edited in a {@link org.hkijena.pipelinej.ACAQJsonExtension}
 */
public class ACAQJsonExtensionSettingsUI extends ACAQJsonExtensionWorkbenchPanel {
    /**
     * @param workbenchUI The workbench UI
     */
    public ACAQJsonExtensionSettingsUI(ACAQJsonExtensionWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        ParameterPanel metadataUI = new ParameterPanel(getExtensionWorkbenchUI(),
                getProject(),
                MarkdownDocument.fromPluginResource("documentation/project-settings.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING | ParameterPanel.WITHOUT_LABEL_SEPARATION);
        add(metadataUI, BorderLayout.CENTER);
    }
}
