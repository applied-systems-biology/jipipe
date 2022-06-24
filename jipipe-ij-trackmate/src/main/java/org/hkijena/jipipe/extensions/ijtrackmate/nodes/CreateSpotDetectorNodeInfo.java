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

import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.nodes.DefaultJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotDetectorData;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.ReflectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CreateSpotDetectorNodeInfo implements JIPipeNodeInfo {

    private static final JIPipeOutputSlot OUTPUT_SLOT = new DefaultJIPipeOutputSlot(SpotDetectorData.class, "Spot detector", "The generated spot detector", null, true, JIPipeDataSlotRole.Data);
    private final String id;
    private final String name;

    private final HTMLText description;
    private final Class<? extends SpotDetectorFactory> spotDetectorClass;

    public CreateSpotDetectorNodeInfo(SpotDetectorFactory<?> detectorFactory) {
        spotDetectorClass = detectorFactory.getClass();
        this.id = "trackmate-create-spot-detector-" + detectorFactory.getKey().toLowerCase().replace('_', '-');
        this.name = detectorFactory.getName();
        this.description = new HTMLText(detectorFactory.getInfoText());
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return CreateSpotDetectorNode.class;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new CreateSpotDetectorNode(this);
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new CreateSpotDetectorNode((CreateSpotDetectorNode)algorithm);
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
