package org.hkijena.jipipe.api.registries;

import com.google.common.collect.*;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A registry for external environments
 */
public class JIPipeExternalEnvironmentRegistry {
    private Multimap<Class<? extends ExternalEnvironment>, InstallerEntry> installers = HashMultimap.create();
    private BiMap<Class<? extends ExternalEnvironment>, ExternalEnvironmentSettings> settings = HashBiMap.create();

    /**
     * Registers an environment and its corresponding settings
     *
     * @param environmentClass the environment
     * @param settings         the settings
     */
    public void registerEnvironment(Class<? extends ExternalEnvironment> environmentClass, ExternalEnvironmentSettings settings) {
        this.settings.put(environmentClass, settings);
    }

    /**
     * Registers an installer
     *
     * @param environmentClass the environment
     * @param installerClass   the installer
     * @param icon             icon for the installer
     */
    public void registerInstaller(Class<? extends ExternalEnvironment> environmentClass, Class<? extends ExternalEnvironmentInstaller> installerClass, Icon icon) {
        installers.put(environmentClass, new InstallerEntry(installerClass, icon));
    }

    /**
     * Returns a sorted list of installer items for the environment
     *
     * @param environmentClass the environment
     * @return list of installers
     */
    public List<InstallerEntry> getInstallers(Class<? extends ExternalEnvironment> environmentClass) {
        return installers.get(environmentClass).stream().sorted(Comparator.comparing(InstallerEntry::getName)).collect(Collectors.toList());
    }

    /**
     * Returns the settings instance
     *
     * @param environmentClass the environment class
     * @return settings
     */
    public ExternalEnvironmentSettings getSettings(Class<?> environmentClass) {
        return settings.get(environmentClass);
    }

    /**
     * Gets the presets of an environment
     *
     * @param environmentClass the environment class
     * @return list of presets
     */
    public List<ExternalEnvironment> getPresets(Class<?> environmentClass) {
        ExternalEnvironmentSettings settings = getSettings(environmentClass);
        if (settings == null)
            return Collections.emptyList();
        return settings.getPresetsListInterface().stream()
                .sorted(Comparator.comparing(ExternalEnvironment::getName)).collect(Collectors.toList());
    }

    /**
     * Adds a new preset into the storage of the environment class
     *
     * @param environmentClass the environment class
     * @param preset           the preset
     */
    public void addPreset(Class<?> environmentClass, ExternalEnvironment preset) {
        ExternalEnvironmentSettings settings = getSettings(environmentClass);
        List<ExternalEnvironment> presets = new ArrayList<>(settings.getPresetsListInterface());
        presets.add(preset);
        settings.setPresetsListInterface(presets);
        settings.getEventBus().post(new JIPipeParameterCollection.ParameterChangedEvent(settings, "presets"));
    }

    /**
     * An entry describing an installer
     */
    public static class InstallerEntry {
        private final Class<? extends ExternalEnvironmentInstaller> installerClass;
        private final String name;
        private final String description;
        private final Icon icon;

        public InstallerEntry(Class<? extends ExternalEnvironmentInstaller> installerClass, Icon icon) {
            this.installerClass = installerClass;
            this.icon = icon;
            JIPipeDocumentation documentation = installerClass.getAnnotation(JIPipeDocumentation.class);
            if (documentation != null) {
                name = documentation.name();
                description = documentation.description();
            } else {
                name = installerClass.getName();
                description = "";
            }
        }

        public Class<? extends ExternalEnvironmentInstaller> getInstallerClass() {
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
