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
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeSettingsSheet;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for settings.
 * Settings are organized in "sheets" (parameter collections)
 */
public class JIPipeApplicationSettingsRegistry {

    private final JIPipe jiPipe;
    private final BiMap<String, JIPipeApplicationSettingsSheet> registeredSheets = HashBiMap.create();
    private final Map<Class<? extends JIPipeApplicationSettingsSheet>, JIPipeApplicationSettingsSheet> registeredSheetsByType = new HashMap<>();
    private final Timer saveLaterTimer;
    private final ChangedEventEmitter changedEventEmitter = new ChangedEventEmitter();

    public JIPipeApplicationSettingsRegistry(JIPipe jiPipe) {
        this.jiPipe = jiPipe;
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
        return JIPipe.getJIPipeUserDir().resolve("settings.json");
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
        getJIPipe().getProgressInfo().log("Registered application settings sheet id=" + sheet.getId() + " in category '" + sheet.getCategory() + "' object=" + sheet);
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
    public <T extends JIPipeSettingsSheet> T getByType(Class<T> klass) {
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
    public void save(Path file) {
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
        save(getPropertyFile());
        changedEventEmitter.emit(new ChangedEvent(this));
    }

    /**
     * Loads settings from the specified file
     *
     * @param file the file
     */
    public void load(Path file) {
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
        }
        finally {
            changedEventEmitter.emit(new ChangedEvent(this));
        }
    }

    /**
     * Reloads the settings from the default file if it exists
     */
    public void reload() {
        load(getPropertyFile());
    }

    public JIPipe getJIPipe() {
        return jiPipe;
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
