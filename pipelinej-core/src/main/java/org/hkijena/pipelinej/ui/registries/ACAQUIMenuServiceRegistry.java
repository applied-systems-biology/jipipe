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

package org.hkijena.pipelinej.ui.registries;

import org.hkijena.pipelinej.ACAQDefaultRegistry;
import org.hkijena.pipelinej.api.ACAQOrganization;
import org.hkijena.pipelinej.ui.ACAQWorkbench;
import org.hkijena.pipelinej.ui.extension.MenuExtension;
import org.hkijena.pipelinej.ui.extension.MenuTarget;
import org.hkijena.pipelinej.utils.ReflectionUtils;

import javax.swing.*;
import java.util.*;

/**
 * Registry for menu extensions
 */
public class ACAQUIMenuServiceRegistry {
    private Map<MenuTarget, List<Class<? extends MenuExtension>>> registeredExtensions = new HashMap<>();

    /**
     * Registers a new extension
     *
     * @param extension the extension
     */
    public void register(Class<? extends MenuExtension> extension) {
        ACAQOrganization organization = extension.getAnnotation(ACAQOrganization.class);
        List<Class<? extends MenuExtension>> list = registeredExtensions.getOrDefault(organization.menuExtensionTarget(), null);
        if (list == null) {
            list = new ArrayList<>();
            registeredExtensions.put(organization.menuExtensionTarget(), list);
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
    public List<MenuExtension> getMenuExtensionsTargeting(MenuTarget target, ACAQWorkbench workbench) {
        List<MenuExtension> result = new ArrayList<>();
        for (Class<? extends MenuExtension> klass : registeredExtensions.getOrDefault(target, Collections.emptyList())) {
            MenuExtension extension = (MenuExtension) ReflectionUtils.newInstance(klass, workbench);
            result.add(extension);
        }
        result.sort(Comparator.comparing(JMenuItem::getText));
        return result;
    }

    public Map<MenuTarget, List<Class<? extends MenuExtension>>> getRegisteredExtensions() {
        return Collections.unmodifiableMap(registeredExtensions);
    }

    public static ACAQUIMenuServiceRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getUIMenuServiceRegistry();
    }
}
