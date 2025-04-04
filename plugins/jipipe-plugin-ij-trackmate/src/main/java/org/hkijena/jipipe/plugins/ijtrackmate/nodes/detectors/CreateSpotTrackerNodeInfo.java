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

package org.hkijena.jipipe.plugins.ijtrackmate.nodes.detectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import org.apache.commons.lang3.StringUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotTrackerData;
import org.hkijena.jipipe.plugins.ijtrackmate.io.DefaultSettingsIO;
import org.hkijena.jipipe.plugins.ijtrackmate.io.SettingsIO;
import org.hkijena.jipipe.plugins.ijtrackmate.io.SpotFeaturePenaltyParameterListSettingsIO;
import org.hkijena.jipipe.plugins.ijtrackmate.parameters.SpotFeaturePenaltyParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;

import java.util.*;

public class CreateSpotTrackerNodeInfo implements JIPipeNodeInfo {

    private static final AddJIPipeOutputSlot OUTPUT_SLOT = new DefaultAddJIPipeOutputSlot(SpotTrackerData.class, "Spot tracker", "The generated spot tracker", null, true, JIPipeDataSlotRole.Data);
    private final String id;
    private final String name;
    private final HTMLText description;
    private final SpotTrackerFactory spotTrackerFactory;
    private final Map<String, SettingsIO> settingsIOMap = new HashMap<>();

    private final BiMap<String, String> settingsToParameterMap = HashBiMap.create();
    private final JIPipeDynamicParameterCollection parameters = new JIPipeDynamicParameterCollection();

    public CreateSpotTrackerNodeInfo(SpotTrackerFactory spotTrackerFactory) {
        this.id = "trackmate-create-spot-tracker-" + spotTrackerFactory.getKey().toLowerCase(Locale.ROOT).replace('_', '-');
        this.name = spotTrackerFactory.getName();
        this.description = new HTMLText(spotTrackerFactory.getInfoText());
        this.spotTrackerFactory = spotTrackerFactory;
        Map<String, Object> defaultSettings = spotTrackerFactory.getDefaultSettings();
        for (Map.Entry<String, Object> entry : defaultSettings.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            JIPipeParameterTypeInfo parameterTypeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getClass());
            String key = entry.getKey().toLowerCase(Locale.ROOT).replace('_', '-');
            String name = StringUtils.capitalize(entry.getKey().replace('_', ' ').toLowerCase(Locale.ROOT));
            Class<?> fieldClass;
            SettingsIO settingsIO;
            if (parameterTypeInfo != null) {
                fieldClass = parameterTypeInfo.getFieldClass();
                settingsIO = new DefaultSettingsIO(fieldClass);
            } else if (entry.getValue() instanceof Map) {
                // Assume it's a penalty
                fieldClass = SpotFeaturePenaltyParameter.List.class;
                settingsIO = new SpotFeaturePenaltyParameterListSettingsIO();
                parameterTypeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(fieldClass);
            } else {
                throw new UnsupportedOperationException("Cannot resolve parameter " + entry.getKey() + "=" + entry.getValue());
            }

            JIPipeMutableParameterAccess parameterAccess = parameters.addParameter(key, fieldClass, name, description.getBody());
            parameterAccess.set(parameterTypeInfo.duplicate(settingsIO.settingToParameter(entry.getValue())));

            settingsToParameterMap.put(entry.getKey(), key);
            settingsIOMap.put(entry.getKey(), settingsIO);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return CreateSpotTrackerNode.class;
    }

    public SpotTrackerFactory getSpotTrackerFactory() {
        return spotTrackerFactory;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new CreateSpotTrackerNode(this);
    }

    public JIPipeDynamicParameterCollection getParameters() {
        return parameters;
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new CreateSpotTrackerNode((CreateSpotTrackerNode) algorithm);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HTMLText getDescription() {
        return description;
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public JIPipeNodeTypeCategory getCategory() {
        return new DataSourceNodeTypeCategory();
    }

    @Override
    public List<AddJIPipeInputSlot> getInputSlots() {
        return Collections.emptyList();
    }

    @Override
    public List<AddJIPipeOutputSlot> getOutputSlots() {
        return Collections.singletonList(OUTPUT_SLOT);
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    public Map<String, SettingsIO> getSettingsIOMap() {
        return settingsIOMap;
    }

    public BiMap<String, String> getSettingsToParameterMap() {
        return settingsToParameterMap;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

}
