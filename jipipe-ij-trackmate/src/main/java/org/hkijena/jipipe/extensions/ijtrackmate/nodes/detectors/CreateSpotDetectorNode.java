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

package org.hkijena.jipipe.extensions.ijtrackmate.nodes.detectors;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.ijtrackmate.TrackMateExtension;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotDetectorData;

import java.util.HashMap;
import java.util.Map;

public class CreateSpotDetectorNode extends JIPipeSimpleIteratingAlgorithm {

    private final CreateSpotDetectorNodeInfo nodeInfo;

    private final JIPipeDynamicParameterCollection parameters;

    public CreateSpotDetectorNode(JIPipeNodeInfo info) {
        super(info);
        this.nodeInfo = (CreateSpotDetectorNodeInfo) info;
        this.parameters = new JIPipeDynamicParameterCollection(((CreateSpotDetectorNodeInfo) info).getParameters());
    }

    public CreateSpotDetectorNode(CreateSpotDetectorNode other) {
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
        SpotDetectorData spotDetectorData = new SpotDetectorData(nodeInfo.getSpotDetectorFactory(), settings);
        iterationStep.addOutputData(getFirstOutputSlot(), spotDetectorData, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Spot detector settings")
    @JIPipeParameter(value = "spot-detector-parameters", resourceClass = TrackMateExtension.class, iconURL = "/org/hkijena/jipipe/extensions/ijtrackmate/icons/trackmate.png")
    public JIPipeDynamicParameterCollection getParameters() {
        return parameters;
    }
}
