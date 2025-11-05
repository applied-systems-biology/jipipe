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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeSettingsSheet;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.hkijena.jipipe.utils.json.PathMetadataStore;

import javax.swing.*;
import javax.swing.Timer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Registry for settings.
 * Settings are organized in "sheets" (parameter collections)
 */
public final class JIPipeApplicationSettingsServiceComponent extends JIPipeServiceComponent {

    private final BiMap<String, JIPipeApplicationSettingsSheet> registeredSheets = HashBiMap.create();
    private final Map<Class<? extends JIPipeApplicationSettingsSheet>, JIPipeApplicationSettingsSheet> registeredSheetsByType = new HashMap<>();
    private final Map<String, PathMetadataStore> registryDatabases = new HashMap<>();
    private final Timer saveLaterTimer;
    private final ChangedEventEmitter changedEventEmitter = new ChangedEventEmitter();

    public JIPipeApplicationSettingsServiceComponent(JIPipeService service) {
        super(service);
        this.saveLaterTimer = new Timer(250, (e) -> {
            save();
        });
        this.saveLaterTimer.setRepeats(false);
    }


    /**
     * Gets the raw property files Json node
     *
     * @return the node. Never null.
     */
    public static JsonNode getRawSheetsNode() {
        Path propertyFile = getSheetsFile(false);
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
     * Return the global settings sheets file
     * @param loadFromOldProfile if the settings can be loaded from an old profile
     * @return the path
     */
    public static Path getSheetsFile(boolean loadFromOldProfile) {
        return JIPipe.getJIPipeUserDir(loadFromOldProfile).resolve("settings.json");
    }

    /**
     * Return the registry settings file for the specific key
     * @param databaseKey the database key
     * @param loadFromOldProfile if the settings can be loaded from an old profile
     * @return the path
     */
    public static Path getRegistryFile(String databaseKey, boolean loadFromOldProfile) {
        if(isValidRegistryDatabaseKey(databaseKey)) {
            throw new IllegalArgumentException("Invalid database key: " + databaseKey);
        }
        return JIPipe.getJIPipeUserDir(loadFromOldProfile).resolve("settings-db-" +  databaseKey + ".json");
    }

    public static boolean isValidRegistryDatabaseKey(String databaseKey) {
        return databaseKey.equals(databaseKey.toLowerCase(Locale.ROOT)) && StringUtils.isFilesystemCompatible(databaseKey);
    }


    /**
     * Registers a new settings sheet
     *
     * @param sheet the sheet
     */
    public void register(JIPipeApplicationSettingsSheet sheet) {
        if (StringUtils.isNullOrEmpty(sheet.getId())) {
            throw new IllegalArgumentException("Invalid ID for settings sheet " + sheet);
        }
        if (StringUtils.isNullOrEmpty(sheet.getIcon())) {
            throw new IllegalArgumentException("Invalid icon for settings sheet " + sheet);
        }
        if (StringUtils.isNullOrEmpty(sheet.getCategory()) || sheet.getCategoryIcon() == null) {
            throw new IllegalArgumentException("Invalid category for settings sheet " + sheet);
        }
        registeredSheets.put(sheet.getId(), sheet);
        registeredSheetsByType.put(sheet.getClass(), sheet);
        getProgressInfo().log("Registered application settings sheet id=" + sheet.getId() + " in category '" + sheet.getCategory() + "' object=" + sheet);
    }

    /**
     * Gets the settings instance with given ID
     *
     * @param id    the ID
     * @param klass the settings class
     * @param <T>   the settings class
     * @return the settings instance.
     */
    public <T extends JIPipeSettingsSheet> T getById(String id, Class<T> klass) {
        JIPipeApplicationSettingsSheet sheet = registeredSheets.getOrDefault(id, null);
        if (sheet != null) {
            return (T) sheet;
        } else {
            return null;
        }
    }

    /**
     * Gets the settings instance with given ID
     *
     * @param klass the settings class
     * @param <T>   the settings class
     * @return the settings instance.
     */
    public <T extends JIPipeApplicationSettingsSheet> T getByType(Class<T> klass) {
        JIPipeApplicationSettingsSheet sheet = registeredSheetsByType.getOrDefault(klass, null);
        if (sheet != null) {
            return (T) sheet;
        } else {
            return null;
        }
    }

    public BiMap<String, JIPipeApplicationSettingsSheet> getRegisteredSheets() {
        return ImmutableBiMap.copyOf(registeredSheets);
    }


    /**
     * Saves the settings to the specified file
     *
     * @param file the file path
     */
    private void saveSheets(Path file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile()))) {
            JsonFactory factory = JsonUtils.getObjectMapper().getFactory();
            JsonGenerator generator = factory.createGenerator(writer);
            generator.useDefaultPrettyPrinter();
            generator.writeStartObject();
            for (Map.Entry<String, JIPipeApplicationSettingsSheet> entry : registeredSheets.entrySet()) {
                generator.writeObjectFieldStart(entry.getKey());
                entry.getValue().serializeToJsonGenerator(generator);
                generator.writeEndObject();
            }
            generator.writeEndObject();
            generator.close();
        } catch (IOException e) {
            IJ.handleException(e);
            e.printStackTrace();
        }
    }

    /**
     * Saves the settings to the default settings file
     */
    public void save() {
        saveSheets(getSheetsFile(false));
        saveRegistries();
        changedEventEmitter.emit(new ChangedEvent(this));
    }

    private void saveRegistries() {
        for (Map.Entry<String, PathMetadataStore> entry : registryDatabases.entrySet()) {
            try {
                Path path = getRegistryFile(entry.getKey(), false);
                JsonUtils.saveToFile(entry.getValue(), path);
            } catch (Exception e) {
                IJ.handleException(e);
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads settings from the specified file
     *
     * @param file the file
     */
    private void loadSheets(Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonNode objectNode = JsonUtils.getObjectMapper().readTree(file.toFile());
            for (Map.Entry<String, JIPipeApplicationSettingsSheet> entry : registeredSheets.entrySet()) {
                if (objectNode.has(entry.getKey())) {
                    try {
                        entry.getValue().deserializeFromJsonNode(objectNode.get(entry.getKey()));
                    } catch (Exception e) {
                        IJ.handleException(e);
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            changedEventEmitter.emit(new ChangedEvent(this));
        }
    }

    /**
     * Puts an object into the registry
     * @param databaseKey the database key
     * @param registryKey the registry key
     * @param value the value
     */
    public void putIntoRegistry(String databaseKey, Path registryKey, Object value) {
        if(!isValidRegistryDatabaseKey(databaseKey)) {
            throw new IllegalArgumentException("Invalid database key: " + databaseKey);
        }
        PathMetadataStore store = registryDatabases.get(databaseKey);
        if(store == null) {
            store = new PathMetadataStore();
            registryDatabases.put(databaseKey, store);
        }

        store.putObject(registryKey, value);

        saveLater();
    }

    /**
     * Gets an object from the registry
     * @param databaseKey the database key
     * @param registryKey the registry key
     * @param type the type
     * @param defaultValue the default value
     * @param destructive if true and the current value is null, always replace it with the default value
     * @param <T> the type
     */
    public <T> T getFromRegistry(String databaseKey, Path registryKey, Class<T> type, T defaultValue, boolean destructive) {
        if(!isValidRegistryDatabaseKey(databaseKey)) {
            throw new IllegalArgumentException("Invalid database key: " + databaseKey);
        }
        boolean changed =false;
        PathMetadataStore store = registryDatabases.get(databaseKey);
        if(store == null) {
            store = new PathMetadataStore();
            registryDatabases.put(databaseKey, store);
            changed = true;
        }

        T object = store.getObject(registryKey, type);
        if(object == null) {
            object = defaultValue;

            if(destructive || !store.containsKey(registryKey)) {
                store.putObject(registryKey, object);
                changed = true;
            }
        }

        if(changed) {
            saveLater();
        }

        return object;
    }

    /**
     * Gets a list object from the registry
     * @param databaseKey the database key
     * @param registryKey the registry key
     * @param type the type
     * @param destructive if true and the current value is null, always replace it with the default value
     * @param <T> the type
     */
    public <T> List<T> getListFromRegistry(String databaseKey, Path registryKey, Class<T> type, boolean destructive) {
        if(!isValidRegistryDatabaseKey(databaseKey)) {
            throw new IllegalArgumentException("Invalid database key: " + databaseKey);
        }
        boolean changed =false;
        PathMetadataStore store = registryDatabases.get(databaseKey);
        if(store == null) {
            store = new PathMetadataStore();
            registryDatabases.put(databaseKey, store);
            changed = true;
        }

        List<T> object = store.getList(registryKey, type);
        if(object == null) {
            object = new ArrayList<>();

            if(destructive || !store.containsKey(registryKey)) {
                store.putObject(registryKey, object);
                changed = true;
            }
        }

        if(changed) {
            saveLater();
        }

        return object;
    }

    /**
     * Reloads the settings from the default file if it exists
     */
    public void reload() {
        loadSheets(getSheetsFile(false));
        registryDatabases.clear();
    }

    public void saveLater() {
        saveLaterTimer.restart();
    }

    public ChangedEventEmitter getChangedEventEmitter() {
        return changedEventEmitter;
    }

    public interface ChangedEventListener {
        void onApplicationSettingsChanged();
    }

    public static class ChangedEvent extends AbstractJIPipeEvent {
        public ChangedEvent(Object source) {
            super(source);
        }
    }

    public static class ChangedEventEmitter extends JIPipeEventEmitter<ChangedEvent, ChangedEventListener> {

        @Override
        protected void call(ChangedEventListener changedEventListener, ChangedEvent event) {
            changedEventListener.onApplicationSettingsChanged();
        }
    }
}
