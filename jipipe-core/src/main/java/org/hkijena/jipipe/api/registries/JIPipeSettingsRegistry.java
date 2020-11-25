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

package org.hkijena.jipipe.api.registries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.Prefs;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.events.ParameterChangedEvent;
import org.hkijena.jipipe.api.parameters.JIPipeCustomParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for settings.
 * Settings are organized in "sheets" (parameter collections)
 */
public class JIPipeSettingsRegistry implements JIPipeParameterCollection, JIPipeCustomParameterCollection {

    private EventBus eventBus = new EventBus();
    private BiMap<String, Sheet> registeredSheets = HashBiMap.create();
    private boolean isLoading = false;

    /**
     * Registers a new settings sheet
     *
     * @param id                  unique ID of the sheet
     * @param name                sheet name
     * @param icon                sheet icon
     * @param category            sheet category. If left null or empty, it will default to "General"
     * @param categoryIcon        optional icon. If null, a wrench icon is used.
     * @param parameterCollection the object that holds the parameters
     */
    public void register(String id, String name, Icon icon, String category, Icon categoryIcon, JIPipeParameterCollection parameterCollection) {
        if (StringUtils.isNullOrEmpty(category)) {
            category = "General";
        }
        if (icon == null) {
            icon = UIUtils.getIconFromResources("actions/view-paged.png");
        }
        if (categoryIcon == null) {
            categoryIcon = UIUtils.getIconFromResources("actions/wrench.png");
        }
        Sheet sheet = new Sheet(name, icon, category, categoryIcon, parameterCollection);
        parameterCollection.getEventBus().register(this);
        registeredSheets.put(id, sheet);
    }

    /**
     * Gets the settings instance with given ID
     *
     * @param id            the ID
     * @param settingsClass the settings class
     * @param <T>           the settings class
     * @return the settings instance.
     */
    public <T extends JIPipeParameterCollection> T getSettings(String id, Class<T> settingsClass) {
        Sheet sheet = registeredSheets.getOrDefault(id, null);
        if (sheet != null) {
            return (T) sheet.getParameterCollection();
        } else {
            return null;
        }
    }

    public BiMap<String, Sheet> getRegisteredSheets() {
        return ImmutableBiMap.copyOf(registeredSheets);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Map<String, JIPipeParameterAccess> getParameters() {
        Map<String, JIPipeParameterAccess> result = new HashMap<>();
        for (Map.Entry<String, Sheet> entry : registeredSheets.entrySet()) {
            JIPipeParameterTree traversedParameterCollection = new JIPipeParameterTree(entry.getValue().getParameterCollection());
            for (Map.Entry<String, JIPipeParameterAccess> accessEntry : traversedParameterCollection.getParameters().entrySet()) {
                result.put(entry.getKey() + "/" + accessEntry.getKey(), accessEntry.getValue());
            }
        }
        return result;
    }

    /**
     * Saves the settings to the specified file
     *
     * @param file the file path
     */
    public void save(Path file) {
        ObjectNode objectNode = JsonUtils.getObjectMapper().getNodeFactory().objectNode();
        for (Map.Entry<String, JIPipeParameterAccess> entry : getParameters().entrySet()) {
            objectNode.set(entry.getKey(), JsonUtils.getObjectMapper().convertValue(entry.getValue().get(Object.class), JsonNode.class));
        }
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), objectNode);
        } catch (IOException e) {
            IJ.handleException(e);
            e.printStackTrace();
        }
    }

    /**
     * Saves the settings to the default settings file
     */
    public void save() {
        save(getPropertyFile());
    }

    /**
     * Loads settings from the specified file
     *
     * @param file the file
     */
    public void load(Path file) {
        if (!Files.isRegularFile(file))
            return;
        try {
            isLoading = true;
            JsonNode objectNode = JsonUtils.getObjectMapper().readTree(file.toFile());
            for (Map.Entry<String, JIPipeParameterAccess> entry : getParameters().entrySet()) {
                JsonNode node = objectNode.path(entry.getKey());
                if (!node.isMissingNode() && !node.isNull()) {
                    Object value = JsonUtils.getObjectMapper().readerFor(entry.getValue().getFieldClass()).readValue(node);
                    entry.getValue().set(value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isLoading = false;
        }
    }

    /**
     * Triggered when a setting was changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onSettingChanged(ParameterChangedEvent event) {
        if (!isLoading) {
            if (JIPipe.getInstance() != null && JIPipe.getInstance().isInitializing())
                return;
            save();
        }
    }

    /**
     * Reloads the settings from the default file if it exists
     */
    public void reload() {
        load(getPropertyFile());
    }

    /**
     * Gets the raw property files Json node
     *
     * @return the node. Never null.
     */
    public static JsonNode getRawNode() {
        Path propertyFile = getPropertyFile();
        if (Files.exists(propertyFile)) {
            try {
                return JsonUtils.getObjectMapper().readTree(propertyFile.toFile());
            } catch (IOException e) {
                return MissingNode.getInstance();
            }
        }
        return MissingNode.getInstance();
    }

    /**
     * @return The location of the file where the settings are stored
     */
    public static Path getPropertyFile() {
        Path imageJDir = Paths.get(Prefs.getImageJDir());
        if (!Files.isDirectory(imageJDir)) {
            try {
                Files.createDirectories(imageJDir);
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        return imageJDir.resolve("jipipe.properties.json");
    }

    /**
     * A settings sheet
     */
    public static class Sheet {
        private String name;
        private String category;
        private Icon icon;
        private Icon categoryIcon;
        private JIPipeParameterCollection parameterCollection;

        /**
         * Creates a new instance
         *
         * @param name                name shown in UI
         * @param icon                icon for this sheet
         * @param category            category shown in UI
         * @param categoryIcon        category icon
         * @param parameterCollection object that holds the parameter
         */
        public Sheet(String name, Icon icon, String category, Icon categoryIcon, JIPipeParameterCollection parameterCollection) {
            this.name = name;
            this.icon = icon;
            this.category = category;
            this.categoryIcon = categoryIcon;
            this.parameterCollection = parameterCollection;
        }

        public Icon getIcon() {
            return icon;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public Icon getCategoryIcon() {
            return categoryIcon;
        }

        public JIPipeParameterCollection getParameterCollection() {
            return parameterCollection;
        }
    }
}
