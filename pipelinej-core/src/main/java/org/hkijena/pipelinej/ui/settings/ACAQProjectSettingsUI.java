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

import org.hkijena.pipelinej.api.grouping.parameters.GraphNodeParametersUI;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbench;
import org.hkijena.pipelinej.ui.ACAQProjectWorkbenchPanel;
import org.hkijena.pipelinej.ui.components.DocumentTabPane;
import org.hkijena.pipelinej.ui.components.FormPanel;
import org.hkijena.pipelinej.ui.components.MarkdownDocument;
import org.hkijena.pipelinej.ui.parameters.ParameterPanel;
import org.hkijena.pipelinej.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI around the metadata of an {@link org.hkijena.pipelinej.api.ACAQProject}
 */
public class ACAQProjectSettingsUI extends ACAQProjectWorkbenchPanel {
    /**
     * @param workbenchUI The workbench UI
     */
    public ACAQProjectSettingsUI(ACAQProjectWorkbench workbenchUI) {
        super(workbenchUI);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());

        DocumentTabPane tabPane = new DocumentTabPane();

        ParameterPanel metadataUI = new ParameterPanel(getProjectWorkbench(),
                getProject().getMetadata(),
                MarkdownDocument.fromPluginResource("documentation/project-settings.md"),
                ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.WITH_SCROLLING);
        tabPane.addTab("General",
                UIUtils.getIconFromResources("cog.png"),
                metadataUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        FormPanel parameterUI = new FormPanel(MarkdownDocument.fromPluginResource("documentation/project-settings-parameters.md"),
                FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION);
        GraphNodeParametersUI graphNodeParametersUI = new GraphNodeParametersUI(getWorkbench(), getPipelineParameters().getExportedParameters());
        graphNodeParametersUI.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        parameterUI.addWideToForm(graphNodeParametersUI, null);
        parameterUI.addVerticalGlue();
        tabPane.addTab("Parameters",
                UIUtils.getIconFromResources("data-types/data-type-parameters.png"),
                parameterUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        add(tabPane, BorderLayout.CENTER);
    }

    private ACAQProjectInfoParameters getPipelineParameters() {
        Object existing = getProject().getAdditionalMetadata().getOrDefault(ACAQProjectInfoParameters.METADATA_KEY, null);
        ACAQProjectInfoParameters result;
        if (existing instanceof ACAQProjectInfoParameters) {
            result = (ACAQProjectInfoParameters) existing;
        } else {
            result = new ACAQProjectInfoParameters();
            getProject().getAdditionalMetadata().put(ACAQProjectInfoParameters.METADATA_KEY, result);
        }
        result.setProject(getProject());
        return result;
    }
}
