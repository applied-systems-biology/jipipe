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

package org.hkijena.jipipe.api.service.components;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.optional.OptionalParameter;
import org.hkijena.jipipe.utils.DocumentationUtils;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A registry for external environments
 */
public final class JIPipeEnvironmentsServiceComponent extends JIPipeServiceComponent {
    private final Multimap<Class<? extends JIPipeEnvironment>, InstallerEntry> installers = HashMultimap.create();
    private final Map<String, EnvironmentInfo> infosById = new HashMap<>();
    private final Map<Class<? extends JIPipeEnvironment>, EnvironmentInfo> infosByClass = new HashMap<>();
    private final Map<Class<? extends OptionalParameter<? extends JIPipeEnvironment>>, EnvironmentInfo> infosByOptionalClass = new HashMap<>();
    private final Map<Class<? extends ListParameter<? extends JIPipeEnvironment>>, EnvironmentInfo> infosByListClass = new HashMap<>();

    public JIPipeEnvironmentsServiceComponent(JIPipeService service) {
        super(service);
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
        getProgressInfo().log("Registered environment installer for " + environmentClass + " with installer class " + installerClass);
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
     * Gets the presets of an environment
     *
     * @param environmentClass the environment class
     * @return list of presets
     */
    public List<JIPipeEnvironment> getPresets(Class<?> environmentClass) {
//        JIPipeExternalEnvironmentSettings settings = getSettings(environmentClass);
//        if (settings == null)
//            return Collections.emptyList();
//        return settings.getPresetsListInterface(environmentClass).stream()
//                .sorted(Comparator.comparing(JIPipeEnvironment::getName)).collect(Collectors.toList());
        return Collections.emptyList();
    }

    /**
     * Adds a new preset into the storage of the environment class
     *
     * @param environmentClass the environment class
     * @param preset           the preset
     */
    public void addPreset(Class<?> environmentClass, JIPipeEnvironment preset) {
//        JIPipeExternalEnvironmentSettings settings = getSettings(environmentClass);
//        List<JIPipeEnvironment> presets = new ArrayList<>(settings.getPresetsListInterface(environmentClass));
//        presets.add(preset);
//        settings.setPresetsListInterface(presets, environmentClass);
//        settings.emitParameterChangedEvent("presets");
    }

    public <T extends JIPipeEnvironment, V extends OptionalParameter<T>, U extends ListParameter<T>> void registerEnvironment(String id, String artifactQuery, Class<T> environmentClass, Class<V> optionalEnvironmentClass, Class<U> environmentListClass, String name, String description, Icon icon) {
        getProgressInfo().log("Registering environment type '" + id + "' (" + environmentClass + ", " + optionalEnvironmentClass + ", " + environmentListClass + ") as '" + name + "'");
        if (infosById.containsKey(id)) {
            throw new RuntimeException("Unable to register environment " + environmentClass + " as '" + id + "': duplicate key!");
        }
        EnvironmentInfo info = new EnvironmentInfo(id, artifactQuery, environmentClass, optionalEnvironmentClass, environmentListClass, name, description, icon);
        infosById.put(id, info);
        infosByClass.put(environmentClass, info);
        infosByOptionalClass.put(optionalEnvironmentClass, info);
        infosByListClass.put(environmentListClass, info);
    }

    public EnvironmentInfo getInfoByClass(Class<? extends JIPipeEnvironment> klass) {
        return infosByClass.get(klass);
    }

    public EnvironmentInfo getInfoByOptionalClass(Class<? extends OptionalParameter<? extends JIPipeEnvironment>> klass) {
        return infosByOptionalClass.get(klass);
    }

    public EnvironmentInfo getInfoByListClass(Class<? extends ListParameter<? extends JIPipeEnvironment>> klass) {
        return infosByListClass.get(klass);
    }

    public EnvironmentInfo getInfoById(String id) {
        return infosById.get(id);
    }

    public static class EnvironmentInfo {
        private final String id;
        private final String artifactQuery;
        private final Class<? extends JIPipeEnvironment> environmentClass;
        private final Class<? extends OptionalParameter<? extends JIPipeEnvironment>> optionalEnvironmentClass;
        private final Class<? extends ListParameter<? extends JIPipeEnvironment>> environmentListClass;
        private final String name;
        private final String description;
        private final Icon icon;

        public EnvironmentInfo(String id, String artifactQuery, Class<? extends JIPipeEnvironment> environmentClass, Class<? extends OptionalParameter<? extends JIPipeEnvironment>> optionalEnvironmentClass, Class<? extends ListParameter<? extends JIPipeEnvironment>> environmentListClass, String name, String description, Icon icon) {
            this.id = id;
            this.artifactQuery = artifactQuery;
            this.environmentClass = environmentClass;
            this.optionalEnvironmentClass = optionalEnvironmentClass;
            this.environmentListClass = environmentListClass;
            this.name = name;
            this.description = description;
            this.icon = icon;
        }

        public String getId() {
            return id;
        }

        public Class<? extends JIPipeEnvironment> getEnvironmentClass() {
            return environmentClass;
        }

        public Class<? extends OptionalParameter<? extends JIPipeEnvironment>> getOptionalEnvironmentClass() {
            return optionalEnvironmentClass;
        }

        public Class<? extends ListParameter<? extends JIPipeEnvironment>> getEnvironmentListClass() {
            return environmentListClass;
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

        public String getArtifactQuery() {
            return artifactQuery;
        }
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
