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

package org.hkijena.jipipe.extensions.imagejalgorithms.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.OMEAccessorTemplate;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.FormPanel;
import org.hkijena.jipipe.ui.components.PickDynamicEnumValueDialog;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class OMEAccessorParameterEditorUI extends JIPipeParameterEditorUI {
    private ParameterPanel parameterPanel;
    private JButton currentTemplateButton;

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterAccess Parameter
     */
    public OMEAccessorParameterEditorUI(JIPipeWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createControlBorder());
        FormPanel formPanel = new FormPanel(FormPanel.NONE);
        add(formPanel, BorderLayout.CENTER);

        JPanel templatePanel = new JPanel(new BorderLayout());

        currentTemplateButton = new JButton();
        currentTemplateButton.setHorizontalAlignment(SwingConstants.LEFT);
        currentTemplateButton.addActionListener(e -> pickTemplate());
        UIUtils.setStandardButtonBorder(currentTemplateButton);
        templatePanel.add(currentTemplateButton, BorderLayout.CENTER);

        JPanel templateButtonsPanel = new JPanel();
        templatePanel.add(templateButtonsPanel, BorderLayout.EAST);
        templateButtonsPanel.setLayout(new BoxLayout(templateButtonsPanel, BoxLayout.X_AXIS));

        JButton selectTemplateButton = new JButton(UIUtils.getIconFromResources("actions/edit.png"));
        UIUtils.setStandardButtonBorder(selectTemplateButton);
        selectTemplateButton.setToolTipText("Select value");
        selectTemplateButton.addActionListener(e -> pickTemplate());
        templateButtonsPanel.add(selectTemplateButton);

        JButton showTemplateHelp = new JButton(UIUtils.getIconFromResources("actions/help.png"));
        UIUtils.setStandardButtonBorder(showTemplateHelp);
        showTemplateHelp.addActionListener(e -> showTemplateHelp());
        templateButtonsPanel.add(showTemplateHelp);

        formPanel.addWideToForm(templatePanel);

        parameterPanel = new ParameterPanel(getWorkbench(), new JIPipeDummyParameterCollection(), new MarkdownDocument(),
                ParameterPanel.NO_GROUP_HEADERS | ParameterPanel.WITH_DOCUMENTATION | ParameterPanel.DOCUMENTATION_NO_UI);
        formPanel.addWideToForm(parameterPanel);
    }

    private void showTemplateHelp() {
        OMEAccessorParameter parameter = getParameter(OMEAccessorParameter.class);
        OMEAccessorTemplate template = ImageJAlgorithmsPlugin.OME_ACCESSOR_STORAGE.getTemplateMap().getOrDefault(parameter.getAccessorId(), null);
        if (template != null) {
            MarkdownReader.showDialog(new MarkdownDocument(template.getDescription()), true, template.getName(), this, false);
        }
    }

    private void pickTemplate() {
        OMEAccessorParameter parameter = getParameter(OMEAccessorParameter.class);
        String selected = PickDynamicEnumValueDialog.showDialog(getWorkbench().getWindow(), new OMEAccessorTypeEnumParameter(), parameter.getAccessorId(), "Select value");
        if (selected != null) {
            parameter.setAccessorId(selected);
            parameter.resetParameters();
            reload();
        }
    }

    @Override
    public boolean isUILabelEnabled() {
        return true;
    }

    @Override
    public void reload() {
        OMEAccessorParameter parameter = getParameter(OMEAccessorParameter.class);
        OMEAccessorTemplate template = ImageJAlgorithmsPlugin.OME_ACCESSOR_STORAGE.getTemplateMap().getOrDefault(parameter.getAccessorId(), null);
        if (template != null) {
            currentTemplateButton.setText(template.getName());
        } else {
            currentTemplateButton.setText(parameter.getAccessorId());
        }
        parameterPanel.setDisplayedParameters(parameter.getParameters());
    }
}
