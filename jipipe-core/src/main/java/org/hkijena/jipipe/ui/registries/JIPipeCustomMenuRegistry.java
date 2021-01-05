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

package org.hkijena.jipipe.ui.registries;

import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.GraphEditorToolBarButtonExtension;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.extension.MenuTarget;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for menu extensions
 */
public class JIPipeCustomMenuRegistry {
    private Map<MenuTarget, List<Class<? extends MenuExtension>>> registeredMenuExtensions = new HashMap<>();
    private List<Class<? extends GraphEditorToolBarButtonExtension>> registeredGraphEditorToolBarExtensions = new ArrayList<>();
    private List<NodeUIContextAction> registeredContextMenuActions = new ArrayList<>();

    /**
     * Registers a new extension
     *
     * @param extension the extension
     */
    public void registerMenu(Class<? extends MenuExtension> extension) {
        JIPipeOrganization organization = extension.getAnnotation(JIPipeOrganization.class);
        List<Class<? extends MenuExtension>> list = registeredMenuExtensions.getOrDefault(organization.menuExtensionTarget(), null);
        if (list == null) {
            list = new ArrayList<>();
            registeredMenuExtensions.put(organization.menuExtensionTarget(), list);
        }
        list.add(extension);
    }

    /**
     * Gets all extensions for a specific menu
     *
     * @param target    the menu
     * @param workbench the workbench
     * @return the extensions
     */
    public List<MenuExtension> getMenuExtensionsTargeting(MenuTarget target, JIPipeWorkbench workbench) {
        List<MenuExtension> result = new ArrayList<>();
        for (Class<? extends MenuExtension> klass : registeredMenuExtensions.getOrDefault(target, Collections.emptyList())) {
            MenuExtension extension = (MenuExtension) ReflectionUtils.newInstance(klass, workbench);
            result.add(extension);
        }
        result.sort(Comparator.comparing(JMenuItem::getText));
        return result;
    }

    public List<GraphEditorToolBarButtonExtension> graphEditorToolBarButtonExtensionsFor(JIPipeGraphEditorUI graphEditorUI) {
        List<GraphEditorToolBarButtonExtension> result = new ArrayList<>();
        for (Class<? extends GraphEditorToolBarButtonExtension> extension : registeredGraphEditorToolBarExtensions) {
            GraphEditorToolBarButtonExtension instance = (GraphEditorToolBarButtonExtension) ReflectionUtils.newInstance(extension, graphEditorUI);
            if(instance.isVisibleInGraph())
                result.add(instance);
        }
        result.sort(Comparator.comparing(instance -> StringUtils.nullToEmpty(StringUtils.orElse(instance.getText(), instance.getToolTipText()))));
        return result;
    }

    public Map<MenuTarget, List<Class<? extends MenuExtension>>> getRegisteredMenuExtensions() {
        return Collections.unmodifiableMap(registeredMenuExtensions);
    }

    /**
     * Registers a new button for the graph editor.
     * The button will appear right next to the search box
     * @param klass the class
     */
    public void registerGraphEditorToolBarButton(Class<? extends GraphEditorToolBarButtonExtension> klass) {
        registeredGraphEditorToolBarExtensions.add(klass);
    }

    /**
     * Registers a new context menu action.
     * @param action the action
     */
    public void registerContextMenuAction(NodeUIContextAction action) {
        registeredContextMenuActions.add(action);
    }

    /**
     * Gets the registered context menu actions
     * @return the actions
     */
    public List<NodeUIContextAction> getRegisteredContextMenuActions() {
        return registeredContextMenuActions;
    }
}
