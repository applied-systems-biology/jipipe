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

package org.hkijena.jipipe.extensions.ijtrackmate.nodes;

import fiji.plugin.trackmate.tracking.SpotTrackerFactory;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.nodes.DefaultJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotTrackerData;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateSpotTrackerNodeInfo implements JIPipeNodeInfo {

    private static final JIPipeOutputSlot OUTPUT_SLOT = new DefaultJIPipeOutputSlot(SpotTrackerData.class, "Spot tracker", "The generated spot tracker", null, true, JIPipeDataSlotRole.Data);
    private final String id;
    private final String name;
    private final HTMLText description;
    private final SpotTrackerFactory spotTrackerFactory;

    private final JIPipeDynamicParameterCollection parameters = new JIPipeDynamicParameterCollection();

    public CreateSpotTrackerNodeInfo(SpotTrackerFactory spotTrackerFactory) {
        this.id = "trackmate-create-spot-tracker-" + spotTrackerFactory.getKey().toLowerCase().replace('_', '-');
        this.name = spotTrackerFactory.getName();
        this.description = new HTMLText(spotTrackerFactory.getInfoText());
        this.spotTrackerFactory = spotTrackerFactory;
        for (Map.Entry<String, Object> entry : spotTrackerFactory.getDefaultSettings().entrySet()) {
            JIPipeParameterTypeInfo parameterTypeInfo = JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getClass());
            if (parameterTypeInfo == null) {
                throw new UnsupportedOperationException("Cannot resolve parameter " + entry.getKey() + "=" + entry.getValue());
            }
            String key = entry.getKey().toLowerCase().replace('_', '-');
            String name = StringUtils.capitalize(entry.getKey().replace('_', ' ').toLowerCase());
            JIPipeMutableParameterAccess parameterAccess = parameters.addParameter(key, entry.getValue().getClass(), name, description.getBody());
            parameterAccess.set(parameterTypeInfo.duplicate(entry.getValue()));
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
    public List<JIPipeInputSlot> getInputSlots() {
        return Collections.emptyList();
    }

    @Override
    public List<JIPipeOutputSlot> getOutputSlots() {
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

}
