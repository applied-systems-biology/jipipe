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

package org.hkijena.jipipe.plugins.imagejalgorithms.parameters;

import org.hkijena.jipipe.api.parameters.JIPipeDummyParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopPickDynamicEnumValueDialog;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.plugins.imagejalgorithms.ImageJAlgorithmsPlugin;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.OMEAccessorTemplate;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class OMEAccessorDesktopParameterEditorUI extends JIPipeDesktopParameterEditorUI {
    private JIPipeDesktopParameterPanel parameterPanel;
    private JButton currentTemplateButton;

    /**
     * Creates new instance
     *
     * @param workbench       the workbench
     * @param parameterAccess Parameter
     */
    public OMEAccessorDesktopParameterEditorUI(JIPipeDesktopWorkbench workbench, JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
        super(workbench, parameterTree, parameterAccess);
        initialize();
        reload();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        setBorder(UIUtils.createControlBorder());
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.NONE);
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

        parameterPanel = new JIPipeDesktopParameterPanel(getDesktopWorkbench(), new JIPipeDummyParameterCollection(), new MarkdownText(),
                JIPipeDesktopParameterPanel.NO_GROUP_HEADERS | JIPipeDesktopParameterPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterPanel.DOCUMENTATION_NO_UI);
        formPanel.addWideToForm(parameterPanel);
    }

    private void showTemplateHelp() {
        OMEAccessorParameter parameter = getParameter(OMEAccessorParameter.class);
        OMEAccessorTemplate template = ImageJAlgorithmsPlugin.OME_ACCESSOR_STORAGE.getTemplateMap().getOrDefault(parameter.getAccessorId(), null);
        if (template != null) {
            JIPipeDesktopMarkdownReader.showDialog(new MarkdownText(template.getDescription()), true, template.getName(), this, false);
        }
    }

    private void pickTemplate() {
        OMEAccessorParameter parameter = getParameter(OMEAccessorParameter.class);
        String selected = JIPipeDesktopPickDynamicEnumValueDialog.showDialog(getDesktopWorkbench().getWindow(), new OMEAccessorTypeEnumParameter(), parameter.getAccessorId(), "Select value");
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
