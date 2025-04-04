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
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import org.apache.commons.lang3.StringUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotDetectorData;
import org.hkijena.jipipe.plugins.ijtrackmate.io.DefaultSettingsIO;
import org.hkijena.jipipe.plugins.ijtrackmate.io.SettingsIO;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;

import java.util.*;

public class CreateSpotDetectorNodeInfo implements JIPipeNodeInfo {

    private static final AddJIPipeOutputSlot OUTPUT_SLOT = new DefaultAddJIPipeOutputSlot(SpotDetectorData.class, "Spot detector", "The generated spot detector", null, true, JIPipeDataSlotRole.Data);
    private final String id;
    private final String name;
    private final HTMLText description;
    private final SpotDetectorFactory<?> spotDetectorFactory;
    private final JIPipeDynamicParameterCollection parameters = new JIPipeDynamicParameterCollection();

    private final BiMap<String, String> settingsToParameterMap = HashBiMap.create();
    private final Map<String, SettingsIO> settingsIOMap = new HashMap<>();

    public CreateSpotDetectorNodeInfo(SpotDetectorFactory<?> spotDetectorFactory) {
        this.id = "trackmate-create-spot-detector-" + spotDetectorFactory.getKey().toLowerCase(Locale.ROOT).replace('_', '-');
        this.name = spotDetectorFactory.getName();
        this.description = new HTMLText(spotDetectorFactory.getInfoText());
        this.spotDetectorFactory = spotDetectorFactory;
        for (Map.Entry<String, Object> entry : spotDetectorFactory.getDefaultSettings().entrySet()) {
            JIPipeParameterTypeInfo parameterTypeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getClass());
            if (parameterTypeInfo == null) {
                throw new UnsupportedOperationException("Cannot resolve parameter " + entry.getKey() + "=" + entry.getValue());
            }
            String key = entry.getKey().toLowerCase(Locale.ROOT).replace('_', '-');
            String name = StringUtils.capitalize(entry.getKey().replace('_', ' ').toLowerCase(Locale.ROOT));
            JIPipeMutableParameterAccess parameterAccess = parameters.addParameter(key, entry.getValue().getClass(), name, description.getBody());
            parameterAccess.set(parameterTypeInfo.duplicate(entry.getValue()));

            settingsToParameterMap.put(entry.getKey(), key);
            settingsIOMap.put(entry.getKey(), new DefaultSettingsIO(parameterTypeInfo.getFieldClass()));
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return CreateSpotDetectorNode.class;
    }

    public SpotDetectorFactory<?> getSpotDetectorFactory() {
        return spotDetectorFactory;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new CreateSpotDetectorNode(this);
    }

    public JIPipeDynamicParameterCollection getParameters() {
        return parameters;
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new CreateSpotDetectorNode((CreateSpotDetectorNode) algorithm);
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

    @Override
    public boolean isHidden() {
        return false;
    }

    public Map<String, SettingsIO> getSettingsIOMap() {
        return settingsIOMap;
    }

    public BiMap<String, String> getSettingsToParameterMap() {
        return settingsToParameterMap;
    }
}
