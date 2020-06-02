package org.hkijena.acaq5.ui.registries;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.ui.ACAQWorkbench;
import org.hkijena.acaq5.ui.extension.MenuExtension;
import org.hkijena.acaq5.ui.extension.MenuTarget;
import org.hkijena.acaq5.utils.ReflectionUtils;

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
