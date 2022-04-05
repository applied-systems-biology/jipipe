package org.hkijena.jipipe.api.registries;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.utils.DocumentationUtils;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A registry for external environments
 */
public class JIPipeExternalEnvironmentRegistry {
    private final JIPipe jiPipe;
    private final Multimap<Class<? extends ExternalEnvironment>, InstallerEntry> installers = HashMultimap.create();
    private final Map<Class<? extends ExternalEnvironment>, ExternalEnvironmentSettings> settings = new HashMap<>();

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
    public void registerEnvironment(Class<? extends ExternalEnvironment> environmentClass, ExternalEnvironmentSettings settings) {
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
    public void registerInstaller(Class<? extends ExternalEnvironment> environmentClass, Class<? extends ExternalEnvironmentInstaller> installerClass, Icon icon) {
        installers.put(environmentClass, new InstallerEntry(installerClass, icon));
        getJIPipe().getProgressInfo().log("Registered environment installer for " + environmentClass + " with installer class " + installerClass);
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
        return settings.getPresetsListInterface(environmentClass).stream()
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
        List<ExternalEnvironment> presets = new ArrayList<>(settings.getPresetsListInterface(environmentClass));
        presets.add(preset);
        settings.setPresetsListInterface(presets, environmentClass);
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
                description = DocumentationUtils.getDocumentationDescription(documentation);
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
