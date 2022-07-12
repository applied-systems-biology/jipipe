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
 *
 */

package org.hkijena.jipipe.extensions.ijtrackmate.datatypes;

import com.fasterxml.jackson.databind.JsonNode;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.TrackMateUtils;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.parameters.ParameterPanel;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "TrackMate spot detector", description = "A spot detector for TrackMate")
@JIPipeDataStorageDocumentation(humanReadableDescription = "Contains a JSON file that stores the properties of the spot detector", jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public class SpotDetectorData implements JIPipeData {

    private final SpotDetectorFactory<?> spotDetectorFactory;
    private final Map<String, Object> settings;

    public SpotDetectorData(SpotDetectorFactory<?> spotDetectorFactory, Map<String, Object> settings) {
        this.spotDetectorFactory = spotDetectorFactory;
        this.settings = settings;
    }

    public SpotDetectorData(SpotDetectorData other) {
        this.spotDetectorFactory = other.spotDetectorFactory;
        this.settings = other.settings;
    }

    public static SpotDetectorData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        JsonNode node = storage.readJSON(storage.findFileByExtension(".json").get(), JsonNode.class);
        String key = node.get("detector-key").textValue();
        PluginInfo<SpotDetectorFactory> pluginInfo = TrackMateUtils.getSpotDetectors().get(key);
        if (pluginInfo == null) {
            throw new NullPointerException("Unknown spot detector: " + key);
        }
        try {
            Map<String, Object> settings = new HashMap<>();
            SpotDetectorFactory<?> instance = pluginInfo.createInstance();
            JsonNode settingsNode = node.get("settings");
            for (Map.Entry<String, Object> entry : instance.getDefaultSettings().entrySet()) {
                JsonNode entryNode = settingsNode.path(entry.getKey());
                if (!entryNode.isMissingNode()) {
                    try {
                        Object value = JsonUtils.getObjectMapper().readerFor(entry.getValue().getClass()).readValue(entryNode);
                        settings.put(entry.getKey(), value);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    JIPipeParameterTypeInfo parameterTypeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getClass());
                    settings.put(entry.getKey(), parameterTypeInfo.duplicate(entry.getValue()));
                }
            }

            return new SpotDetectorData(instance, settings);

        } catch (InstantiableException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Map<String, Object> data = new HashMap<>();
        data.put("detector-key", spotDetectorFactory.getKey());
        data.put("class", spotDetectorFactory.getClass().getCanonicalName());
        data.put("settings", settings);
        name = StringUtils.orElse(name, "settings") + ".json";
        storage.writeJSON(Paths.get(name), data);
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new SpotDetectorData(this);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        HTMLText description = new HTMLText(spotDetectorFactory.getInfoText());
        JIPipeDynamicParameterCollection parameters = new JIPipeDynamicParameterCollection();
        for (Map.Entry<String, Object> entry : settings.entrySet()) {
            Object value = JIPipe.duplicateParameter(entry.getValue());
            String key = entry.getKey();
            String name = WordUtils.capitalize(entry.getKey().replace('_', ' ').toLowerCase());
            JIPipeMutableParameterAccess parameterAccess = parameters.addParameter(key, entry.getValue().getClass(), name, description.getBody());
            parameterAccess.set(value);
        }

        ParameterPanel.showDialog(workbench,
                parameters,
                new MarkdownDocument("# Spot detector info"),
                "Spot detector",
                ParameterPanel.WITH_SEARCH_BAR | ParameterPanel.WITH_SCROLLING | ParameterPanel.WITH_DOCUMENTATION);
    }

    @Override
    public String toString() {
        return spotDetectorFactory.getName();
    }

    public SpotDetectorFactory<?> getSpotDetectorFactory() {
        return spotDetectorFactory;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    @Override
    public String toDetailedString() {
        return spotDetectorFactory.getName() + ": " + settings.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(", "));
    }
}
