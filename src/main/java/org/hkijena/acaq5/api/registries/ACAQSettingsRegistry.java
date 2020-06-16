package org.hkijena.acaq5.api.registries;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import ij.IJ;
import ij.Prefs;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.ACAQCustomParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.UIUtils;

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
public class ACAQSettingsRegistry implements ACAQParameterCollection, ACAQCustomParameterCollection {

    private EventBus eventBus = new EventBus();
    private BiMap<String, Sheet> registeredSheets = HashBiMap.create();
    private boolean isLoading = false;

    /**
     * Registers a new settings sheet
     *
     * @param id                  unique ID of the sheet
     * @param name                sheet name
     * @param category            sheet category. If left null or empty, it will default to "General"
     * @param categoryIcon        optional icon. If null, a wrench icon is used.
     * @param parameterCollection the object that holds the parameters
     */
    public void register(String id, String name, String category, Icon categoryIcon, ACAQParameterCollection parameterCollection) {
        if (StringUtils.isNullOrEmpty(category)) {
            category = "General";
        }
        if (categoryIcon == null) {
            categoryIcon = UIUtils.getIconFromResources("wrench.png");
        }
        Sheet sheet = new Sheet(name, category, categoryIcon, parameterCollection);
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
    public <T extends ACAQParameterCollection> T getSettings(String id, Class<T> settingsClass) {
        return (T) registeredSheets.get(id).getParameterCollection();
    }

    public BiMap<String, Sheet> getRegisteredSheets() {
        return ImmutableBiMap.copyOf(registeredSheets);
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Map<String, ACAQParameterAccess> getParameters() {
        Map<String, ACAQParameterAccess> result = new HashMap<>();
        for (Map.Entry<String, Sheet> entry : registeredSheets.entrySet()) {
            ACAQParameterTree traversedParameterCollection = new ACAQParameterTree(entry.getValue().getParameterCollection());
            for (Map.Entry<String, ACAQParameterAccess> accessEntry : traversedParameterCollection.getParameters().entrySet()) {
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
        for (Map.Entry<String, ACAQParameterAccess> entry : getParameters().entrySet()) {
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
            for (Map.Entry<String, ACAQParameterAccess> entry : getParameters().entrySet()) {
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
            save();
        }
    }

    /**
     * Reloads the settings from the default file if it exists
     */
    public void reload() {
        load(getPropertyFile());
    }

    public static ACAQSettingsRegistry getInstance() {
        return ACAQDefaultRegistry.getInstance().getSettingsRegistry();
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
        return imageJDir.resolve("acaq5.properties.json");
    }

    /**
     * A settings sheet
     */
    public static class Sheet {
        private String name;
        private String category;
        private Icon categoryIcon;
        private ACAQParameterCollection parameterCollection;

        /**
         * Creates a new instance
         *
         * @param name                name shown in UI
         * @param category            category shown in UI
         * @param categoryIcon        category icon
         * @param parameterCollection object that holds the parameter
         */
        public Sheet(String name, String category, Icon categoryIcon, ACAQParameterCollection parameterCollection) {
            this.name = name;
            this.category = category;
            this.categoryIcon = categoryIcon;
            this.parameterCollection = parameterCollection;
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

        public ACAQParameterCollection getParameterCollection() {
            return parameterCollection;
        }
    }
}
