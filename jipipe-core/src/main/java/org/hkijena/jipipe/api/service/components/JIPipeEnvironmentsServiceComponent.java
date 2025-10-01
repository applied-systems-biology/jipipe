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

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.environments.*;
import org.hkijena.jipipe.api.parameters.JIPipeDefaultMutableParameterTypeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;
import org.hkijena.jipipe.plugins.parameters.api.optional.JIPipeOptionalParameter;
import org.hkijena.jipipe.plugins.parameters.ui.library.JIPipeDesktopExternalEnvironmentParameterEditorUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import java.util.*;

/**
 * A registry for external environments
 */
public final class JIPipeEnvironmentsServiceComponent extends JIPipeServiceComponent {
    private final Map<String, EnvironmentInfo> infosById = new HashMap<>();
    private final Map<Class<? extends JIPipeEnvironment>, EnvironmentInfo> infosByClass = new HashMap<>();
    private final Map<Class<? extends JIPipeOptionalParameter<? extends JIPipeEnvironment>>, EnvironmentInfo> infosByOptionalClass = new HashMap<>();
    private final Map<Class<? extends JIPipeListParameter<? extends JIPipeEnvironment>>, EnvironmentInfo> infosByListClass = new HashMap<>();
    private final List<JIPipeEnvironmentSetupTool> setupTools = new ArrayList<>();

    public JIPipeEnvironmentsServiceComponent(JIPipeService service) {
        super(service);
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

    public <T extends JIPipeEnvironment, V extends JIPipeOptionalParameter<T>, U extends JIPipeListParameter<T>> void registerEnvironment(String id, String artifactQuery, JIPipeEnvironmentArchetype archetype, Class<T> environmentClass, Class<V> optionalEnvironmentClass, Class<U> environmentListClass, String name, String description, Icon icon) {
        getProgressInfo().log("Registering environment type '" + id + "' (" + environmentClass + ", " + optionalEnvironmentClass + ", " + environmentListClass + ") as '" + name + "'");
        if (infosById.containsKey(id)) {
            throw new RuntimeException("Unable to register environment " + environmentClass + " as '" + id + "': duplicate key!");
        }

        // Register into this environment service
        EnvironmentInfo info = new EnvironmentInfo(id, artifactQuery, archetype, environmentClass, optionalEnvironmentClass, environmentListClass, name, description, icon);
        infosById.put(id, info);
        infosByClass.put(environmentClass, info);
        infosByOptionalClass.put(optionalEnvironmentClass, info);
        infosByListClass.put(environmentListClass, info);

        // Register as parameter
        registerEnvironmentParameter("env:" + id, environmentClass, name, description, JIPipeParameterArchetype.Value);
        registerEnvironmentParameter("env: " + id + ":optional", optionalEnvironmentClass, name, description, JIPipeParameterArchetype.OptionalValue);
        registerEnvironmentParameter("env: " + id + ":list", environmentListClass, name, description, JIPipeParameterArchetype.List);
        getService().getParameterTypes().registerParameterEditor(environmentClass, JIPipeDesktopExternalEnvironmentParameterEditorUI.class);
    }

    private void registerEnvironmentParameter(String id, Class<?> klass, String name, String description, JIPipeParameterArchetype archetype) {
        JIPipeDefaultMutableParameterTypeInfo info = new JIPipeDefaultMutableParameterTypeInfo(id,
                klass,
                () -> ReflectionUtils.newInstance(klass),
                o -> ReflectionUtils.newInstance(klass, o),
                name,
                description, archetype);
        JIPipeParameterTypesServiceComponent parameterTypes = getService().getParameterTypes();
        parameterTypes.register(info);
    }

    public EnvironmentInfo getInfoByClass(Class<? extends JIPipeEnvironment> klass) {
        return infosByClass.get(klass);
    }

    public EnvironmentInfo getInfoByOptionalClass(Class<? extends JIPipeOptionalParameter<? extends JIPipeEnvironment>> klass) {
        return infosByOptionalClass.get(klass);
    }

    public EnvironmentInfo getInfoByListClass(Class<? extends JIPipeListParameter<? extends JIPipeEnvironment>> klass) {
        return infosByListClass.get(klass);
    }

    public EnvironmentInfo getInfoById(String id) {
        return infosById.get(id);
    }

    public Map<String, EnvironmentInfo> getInfosById() {
        return Collections.unmodifiableMap(infosById);
    }

    public Map<Class<? extends JIPipeEnvironment>, EnvironmentInfo> getInfosByClass() {
        return Collections.unmodifiableMap(infosByClass);
    }

    public Map<Class<? extends JIPipeOptionalParameter<? extends JIPipeEnvironment>>, EnvironmentInfo> getInfosByOptionalClass() {
        return Collections.unmodifiableMap(infosByOptionalClass);
    }

    public Map<Class<? extends JIPipeListParameter<? extends JIPipeEnvironment>>, EnvironmentInfo> getInfosByListClass() {
        return Collections.unmodifiableMap(infosByListClass);
    }

    public List<JIPipeEnvironmentSetupTool> getSetupTools() {
        return Collections.unmodifiableList(setupTools);
    }

    public void registerSetupTool(JIPipeEnvironmentSetupTool tool) {
        setupTools.add(Objects.requireNonNull(tool));
    }

    /**
     * Gets a fully configured environment
     *
     * @param klass              the environment class
     * @param configurationCache the environment cache
     * @param progressInfo       the progress info
     * @param <T>                the environment class
     * @return the environment
     */
    public <T extends JIPipeEnvironment> T getEnvironment(Class<T> klass, JIPipeEnvironmentConfigurationCache configurationCache, JIPipeProgressInfo progressInfo) {
        return getEnvironmentConfigurator(klass, configurationCache).get(progressInfo);
    }

    /**
     * Returns the environment reference for the environment class
     *
     * @param klass the environment class
     * @param <T>   the environment type
     * @return the environment reference
     */
    public <T extends JIPipeEnvironment> JIPipeEnvironmentConfigurator<T> getEnvironmentConfigurator(Class<T> klass, JIPipeEnvironmentConfigurationCache configurationCache) {
        return new JIPipeEnvironmentConfigurator<>(klass, configurationCache, null, null);
    }

    /**
     * Returns a sorted list of all compatible setup tools
     * @param environmentClass the environment class
     * @return the list of compatible tools, sorted
     */
    public List<JIPipeEnvironmentSetupTool> getCompatibleSetupTools(Class<?> environmentClass) {
        if(JIPipeEnvironment.class.isAssignableFrom(environmentClass)) {
            return setupTools.stream().filter(tool -> tool.accepts((Class<? extends JIPipeEnvironment>) environmentClass))
                    .sorted(Comparator.comparing(JIPipeEnvironmentSetupTool::getName))
                    .toList();
        }
        return Collections.emptyList();
    }

    public static class EnvironmentInfo {
        private final String id;
        private final String artifactQuery;
        private final JIPipeEnvironmentArchetype archetype;
        private final Class<? extends JIPipeEnvironment> environmentClass;
        private final Class<? extends JIPipeOptionalParameter<? extends JIPipeEnvironment>> optionalEnvironmentClass;
        private final Class<? extends JIPipeListParameter<? extends JIPipeEnvironment>> environmentListClass;
        private final String name;
        private final String description;
        private final Icon icon;

        public EnvironmentInfo(String id, String artifactQuery, JIPipeEnvironmentArchetype archetype, Class<? extends JIPipeEnvironment> environmentClass, Class<? extends JIPipeOptionalParameter<? extends JIPipeEnvironment>> optionalEnvironmentClass, Class<? extends JIPipeListParameter<? extends JIPipeEnvironment>> environmentListClass, String name, String description, Icon icon) {
            this.id = id;
            this.artifactQuery = artifactQuery;
            this.archetype = archetype;
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

        public Class<? extends JIPipeOptionalParameter<? extends JIPipeEnvironment>> getOptionalEnvironmentClass() {
            return optionalEnvironmentClass;
        }

        public Class<? extends JIPipeListParameter<? extends JIPipeEnvironment>> getEnvironmentListClass() {
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

        public JIPipeEnvironmentArchetype getArchetype() {
            return archetype;
        }

        public boolean isArtifact() {
            return JIPipeArtifactEnvironment.class.isAssignableFrom(environmentClass);
        }

        public boolean hasArtifactQuery() {
            return !StringUtils.isNullOrEmpty(artifactQuery);
        }
    }
}
