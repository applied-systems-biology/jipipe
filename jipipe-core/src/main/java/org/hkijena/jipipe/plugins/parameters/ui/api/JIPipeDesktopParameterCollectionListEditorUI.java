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

package org.hkijena.jipipe.plugins.parameters.ui.api;

import org.hkijena.jipipe.api.parameters.*;
import org.hkijena.jipipe.desktop.commons.components.panels.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameterItemParameterAccess;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeParameterCollectionList;
import org.hkijena.jipipe.plugins.parameters.api.collections.ParameterCollectionListTemplate;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Map;

/**
 * Generic parameter editor for {@link JIPipeParameterCollectionList} — the
 * thin subclass of the shared list editor base: each item is rendered as a
 * nested mini form panel, and the template is re-created from the
 * {@link ParameterCollectionListTemplate} annotation on every reload.
 */
public class JIPipeDesktopParameterCollectionListEditorUI extends JIPipeListParameterEditorUI {
    public JIPipeDesktopParameterCollectionListEditorUI(InitializationParameters parameters) {
        super(JIPipeParameterCollectionList.class, parameters);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                reload();
                super.componentShown(e);
            }
        });
    }

    @Override
    protected void onBeforeGenerateEntries() {
        JIPipeParameterCollectionList parameter = (JIPipeParameterCollectionList) getParameter();
        // Workaround for ParameterCollectionList template issues
        // Recreates the template on creating the UI to enforce the class-based
        ParameterCollectionListTemplate annotation = getParameterAccess().getAnnotationOfType(ParameterCollectionListTemplate.class);
        if (annotation != null) {
            JIPipeDynamicParameterCollection template = parameter.getTemplate();
            template.clear();
            JIPipeParameterCollection templateCollection = (JIPipeParameterCollection) ReflectionUtils.newInstance(annotation.value());
            JIPipeParameterTree tree = new JIPipeParameterTree(templateCollection);
            for (Map.Entry<String, JIPipeParameterAccess> entry : tree.getParameters().entrySet()) {
                JIPipeMutableParameterAccess parameterAccess = new JIPipeMutableParameterAccess(entry.getValue());
                template.addParameter(parameterAccess);
            }

            // Apply the template to the items
            parameter.applyTemplateToItems();
        }
    }

    @Override
    protected JComponent buildItemContent(int index, JIPipeListParameterItemParameterAccess<?> access) {
        JIPipeParameterCollectionList parameter = (JIPipeParameterCollectionList) getParameter();

        JIPipeDesktopParameterFormPanel ui;
        if (getContextParent() instanceof JIPipeDesktopParameterFormPanel && ((JIPipeDesktopParameterFormPanel) getContextParent()).isWithExternalDocumentation()) {
            ui = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), parameter.get(index), getParameterTree(), null, JIPipeDesktopParameterFormPanel.NO_EMPTY_GROUP_HEADERS | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION | JIPipeDesktopParameterFormPanel.DOCUMENTATION_EXTERNAL);
            ui.setRedirectDocumentationTarget((JIPipeDesktopFormPanel) getContextParent());
        } else {
            ui = new JIPipeDesktopParameterFormPanel(getDesktopWorkbench(), parameter.get(index), getParameterTree(), null, JIPipeDesktopParameterFormPanel.NO_EMPTY_GROUP_HEADERS);
        }

        ui.setBorder(UIUtils.createControlBorder());
        return ui;
    }

    @Override
    public String itemDescription(int index) {
        return JIPipeCustomTextDescriptionParameter.getTextDescriptionOf(getParameter().get(index));
    }
}
