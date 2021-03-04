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

import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParametersUI;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbenchPanel;
import org.hkijena.jipipe.ui.components.DocumentTabPane;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * UI around the metadata of an {@link org.hkijena.jipipe.api.JIPipeProject}
 */
public class JIPipeProjectSettingsUI extends JIPipeProjectWorkbenchPanel {
    /**
     * @param workbenchUI The workbench UI
     */
    public JIPipeProjectSettingsUI(JIPipeProjectWorkbench workbenchUI) {
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
                UIUtils.getIconFromResources("actions/configure.png"),
                metadataUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        FormPanel parameterUI = new FormPanel(MarkdownDocument.fromPluginResource("documentation/project-settings-parameters.md"),
                FormPanel.WITH_SCROLLING | FormPanel.WITH_DOCUMENTATION);
        GraphNodeParametersUI graphNodeParametersUI = new GraphNodeParametersUI(getWorkbench(), getPipelineParameters().getExportedParameters(), FormPanel.NONE);
        graphNodeParametersUI.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        parameterUI.addWideToForm(graphNodeParametersUI, null);
        parameterUI.addVerticalGlue();
        tabPane.addTab("Parameters",
                UIUtils.getIconFromResources("data-types/parameters.png"),
                parameterUI,
                DocumentTabPane.CloseMode.withoutCloseButton,
                false);

        add(tabPane, BorderLayout.CENTER);
    }

    private JIPipeProjectInfoParameters getPipelineParameters() {
        Object existing = getProject().getAdditionalMetadata().getOrDefault(JIPipeProjectInfoParameters.METADATA_KEY, null);
        JIPipeProjectInfoParameters result;
        if (existing instanceof JIPipeProjectInfoParameters) {
            result = (JIPipeProjectInfoParameters) existing;
        } else {
            result = new JIPipeProjectInfoParameters();
            getProject().getAdditionalMetadata().put(JIPipeProjectInfoParameters.METADATA_KEY, result);
        }
        result.setProject(getProject());
        return result;
    }
}
