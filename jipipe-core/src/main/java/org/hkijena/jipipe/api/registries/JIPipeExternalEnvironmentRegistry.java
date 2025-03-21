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

package org.hkijena.jipipe.api.registries;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironmentSettings;
import org.hkijena.jipipe.utils.DocumentationUtils;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A registry for external environments
 */
public class JIPipeExternalEnvironmentRegistry {
    private final JIPipe jiPipe;
    private final Multimap<Class<? extends JIPipeEnvironment>, InstallerEntry> installers = HashMultimap.create();
    private final Map<Class<? extends JIPipeEnvironment>, JIPipeExternalEnvironmentSettings> settings = new HashMap<>();

    public JIPipeExternalEnvironmentRegistry(JIPipe jiPipe) {

        this.jiPipe = jiPipe;
    }

    public JIPipe getJIPipe() {
        return jiPipe;
    }

    /**
     * Registers an environment and its corresponding settings
     *
     * @param environmentClass the environment
     * @param settings         the settings
     */
    public void registerEnvironment(Class<? extends JIPipeEnvironment> environmentClass, JIPipeExternalEnvironmentSettings settings) {
        this.settings.put(environmentClass, settings);
        getJIPipe().getProgressInfo().log("Registered environment " + environmentClass + " with settings class " + settings);
    }

    /**
     * Registers an installer
     *
     * @param environmentClass the environment
     * @param installerClass   the installer
     * @param icon             icon for the installer
     */
    public void registerInstaller(Class<? extends JIPipeEnvironment> environmentClass, Class<? extends JIPipeExternalEnvironmentInstaller> installerClass, Icon icon) {
        installers.put(environmentClass, new InstallerEntry(installerClass, icon));
        getJIPipe().getProgressInfo().log("Registered environment installer for " + environmentClass + " with installer class " + installerClass);
    }

    /**
     * Returns a sorted list of installer items for the environment
     *
     * @param environmentClass the environment
     * @return list of installers
     */
    public List<InstallerEntry> getInstallers(Class<? extends JIPipeEnvironment> environmentClass) {
        return installers.get(environmentClass).stream().sorted(Comparator.comparing(InstallerEntry::getName)).collect(Collectors.toList());
    }

    /**
     * Returns the settings instance
     *
     * @param environmentClass the environment class
     * @return settings
     */
    public JIPipeExternalEnvironmentSettings getSettings(Class<?> environmentClass) {
        return settings.get(environmentClass);
    }

    /**
     * Gets the presets of an environment
     *
     * @param environmentClass the environment class
     * @return list of presets
     */
    public List<JIPipeEnvironment> getPresets(Class<?> environmentClass) {
        JIPipeExternalEnvironmentSettings settings = getSettings(environmentClass);
        if (settings == null)
            return Collections.emptyList();
        return settings.getPresetsListInterface(environmentClass).stream()
                .sorted(Comparator.comparing(JIPipeEnvironment::getName)).collect(Collectors.toList());
    }

    /**
     * Adds a new preset into the storage of the environment class
     *
     * @param environmentClass the environment class
     * @param preset           the preset
     */
    public void addPreset(Class<?> environmentClass, JIPipeEnvironment preset) {
        JIPipeExternalEnvironmentSettings settings = getSettings(environmentClass);
        List<JIPipeEnvironment> presets = new ArrayList<>(settings.getPresetsListInterface(environmentClass));
        presets.add(preset);
        settings.setPresetsListInterface(presets, environmentClass);
        settings.emitParameterChangedEvent("presets");
    }

    /**
     * An entry describing an installer
     */
    public static class InstallerEntry {
        private final Class<? extends JIPipeExternalEnvironmentInstaller> installerClass;
        private final String name;
        private final String description;
        private final Icon icon;

        public InstallerEntry(Class<? extends JIPipeExternalEnvironmentInstaller> installerClass, Icon icon) {
            this.installerClass = installerClass;
            this.icon = icon;
            SetJIPipeDocumentation documentation = installerClass.getAnnotation(SetJIPipeDocumentation.class);
            if (documentation != null) {
                name = documentation.name();
                description = DocumentationUtils.getDocumentationDescription(documentation);
            } else {
                name = installerClass.getName();
                description = "";
            }
        }

        public Class<? extends JIPipeExternalEnvironmentInstaller> getInstallerClass() {
            return installerClass;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Icon getIcon() {
            return icon;
        }
    }
}
