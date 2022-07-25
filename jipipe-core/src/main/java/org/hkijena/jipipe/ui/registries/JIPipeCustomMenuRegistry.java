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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.extension.GraphEditorToolBarButtonExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtension;
import org.hkijena.jipipe.ui.extension.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.ui.grapheditor.general.JIPipeGraphEditorUI;
import org.hkijena.jipipe.ui.grapheditor.general.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Registry for menu extensions
 */
public class JIPipeCustomMenuRegistry {
    private final Map<JIPipeMenuExtensionTarget, List<Class<? extends JIPipeMenuExtension>>> registeredMenuExtensions = new HashMap<>();
    private final List<Class<? extends GraphEditorToolBarButtonExtension>> registeredGraphEditorToolBarExtensions = new ArrayList<>();
    private final List<NodeUIContextAction> registeredContextMenuActions = new ArrayList<>();
    private final JIPipe jiPipe;

    public JIPipeCustomMenuRegistry(JIPipe jiPipe) {

        this.jiPipe = jiPipe;
    }

    /**
     * Registers a new extension
     *
     * @param extension the extension
     */
    public void registerMenu(Class<? extends JIPipeMenuExtension> extension) {
        try {
            Constructor<? extends JIPipeMenuExtension> constructor = extension.getConstructor(JIPipeWorkbench.class);
            JIPipeMenuExtension instance = constructor.newInstance((JIPipeWorkbench) null);
            List<Class<? extends JIPipeMenuExtension>> list = registeredMenuExtensions.getOrDefault(instance.getMenuTarget(), null);
            if (list == null) {
                list = new ArrayList<>();
                registeredMenuExtensions.put(instance.getMenuTarget(), list);
            }
            list.add(extension);
            getJIPipe().getProgressInfo().log("Registered menu extension " + extension);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all extensions for a specific menu
     *
     * @param target    the menu
     * @param workbench the workbench
     * @return the extensions
     */
    public List<JIPipeMenuExtension> getMenuExtensionsTargeting(JIPipeMenuExtensionTarget target, JIPipeWorkbench workbench) {
        List<JIPipeMenuExtension> result = new ArrayList<>();
        for (Class<? extends JIPipeMenuExtension> klass : registeredMenuExtensions.getOrDefault(target, Collections.emptyList())) {
            JIPipeMenuExtension extension = (JIPipeMenuExtension) ReflectionUtils.newInstance(klass, workbench);
            result.add(extension);
        }
        result.sort(Comparator.comparing(JMenuItem::getText));
        return result;
    }

    public List<GraphEditorToolBarButtonExtension> graphEditorToolBarButtonExtensionsFor(JIPipeGraphEditorUI graphEditorUI) {
        List<GraphEditorToolBarButtonExtension> result = new ArrayList<>();
        for (Class<? extends GraphEditorToolBarButtonExtension> extension : registeredGraphEditorToolBarExtensions) {
            GraphEditorToolBarButtonExtension instance = (GraphEditorToolBarButtonExtension) ReflectionUtils.newInstance(extension, graphEditorUI);
            if (instance.isVisibleInGraph())
                result.add(instance);
        }
        result.sort(Comparator.comparing(instance -> StringUtils.nullToEmpty(StringUtils.orElse(instance.getText(), instance.getToolTipText()))));
        return result;
    }

    public Map<JIPipeMenuExtensionTarget, List<Class<? extends JIPipeMenuExtension>>> getRegisteredMenuExtensions() {
        return Collections.unmodifiableMap(registeredMenuExtensions);
    }

    /**
     * Registers a new button for the graph editor.
     * The button will appear right next to the search box
     *
     * @param klass the class
     */
    public void registerGraphEditorToolBarButton(Class<? extends GraphEditorToolBarButtonExtension> klass) {
        registeredGraphEditorToolBarExtensions.add(klass);
    }

    /**
     * Registers a new context menu action.
     *
     * @param action the action
     */
    public void registerContextMenuAction(NodeUIContextAction action) {
        registeredContextMenuActions.add(action);
    }

    /**
     * Gets the registered context menu actions
     *
     * @return the actions
     */
    public List<NodeUIContextAction> getRegisteredContextMenuActions() {
        return registeredContextMenuActions;
    }

    public JIPipe getJIPipe() {
        return jiPipe;
    }
}
