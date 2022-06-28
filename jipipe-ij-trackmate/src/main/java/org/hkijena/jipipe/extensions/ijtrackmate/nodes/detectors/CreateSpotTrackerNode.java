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
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.ijtrackmate.TrackMateExtension;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotTrackerData;

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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Map<String, Object> settings = new HashMap<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : parameters.getParameters().entrySet()) {
            String settingsKey = nodeInfo.getSettingsToParameterMap().inverse().get(entry.getKey());
            Object parameterValue = JIPipe.duplicateParameter(entry.getValue().get(Object.class));
            Object settingValue = nodeInfo.getSettingsIOMap().get(settingsKey).parameterToSetting(parameterValue);
            settings.put(settingsKey, settingValue);
        }
        SpotTrackerData spotDetectorData = new SpotTrackerData(nodeInfo.getSpotTrackerFactory(), settings);
        dataBatch.addOutputData(getFirstOutputSlot(), spotDetectorData, progressInfo);
    }

    @JIPipeDocumentation(name = "Spot tracker settings")
    @JIPipeParameter(value = "spot-tracker-parameters", resourceClass = TrackMateExtension.class, iconURL = "/org/hkijena/jipipe/extensions/ijtrackmate/trackmate-16.png")
    public JIPipeDynamicParameterCollection getParameters() {
        return parameters;
    }
}
