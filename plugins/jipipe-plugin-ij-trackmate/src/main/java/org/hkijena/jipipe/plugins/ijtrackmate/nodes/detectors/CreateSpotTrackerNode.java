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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.plugins.ijtrackmate.TrackMatePlugin;
import org.hkijena.jipipe.plugins.ijtrackmate.datatypes.SpotTrackerData;

import java.util.HashMap;
import java.util.Map;

public class CreateSpotTrackerNode extends JIPipeSimpleIteratingAlgorithm {

    private final CreateSpotTrackerNodeInfo nodeInfo;

    private final JIPipeDynamicParameterCollection parameters;

    public CreateSpotTrackerNode(JIPipeNodeInfo info) {
        super(info);
        this.nodeInfo = (CreateSpotTrackerNodeInfo) info;
        this.parameters = new JIPipeDynamicParameterCollection(((CreateSpotTrackerNodeInfo) info).getParameters());
    }

    public CreateSpotTrackerNode(CreateSpotTrackerNode other) {
        super(other);
        this.nodeInfo = other.nodeInfo;
        this.parameters = new JIPipeDynamicParameterCollection(other.parameters);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Map<String, Object> settings = new HashMap<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : parameters.getParameters().entrySet()) {
            String settingsKey = nodeInfo.getSettingsToParameterMap().inverse().get(entry.getKey());
            Object parameterValue = JIPipe.duplicateParameter(entry.getValue().get(Object.class));
            Object settingValue = nodeInfo.getSettingsIOMap().get(settingsKey).parameterToSetting(parameterValue);
            settings.put(settingsKey, settingValue);
        }
        SpotTrackerData spotDetectorData = new SpotTrackerData(nodeInfo.getSpotTrackerFactory(), settings);
        iterationStep.addOutputData(getFirstOutputSlot(), spotDetectorData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Spot tracker settings")
    @JIPipeParameter(value = "spot-tracker-parameters", resourceClass = TrackMatePlugin.class, iconURL = "/org/hkijena/jipipe/plugins/ijtrackmate/icons/trackmate.png")
    public JIPipeDynamicParameterCollection getParameters() {
        return parameters;
    }
}
