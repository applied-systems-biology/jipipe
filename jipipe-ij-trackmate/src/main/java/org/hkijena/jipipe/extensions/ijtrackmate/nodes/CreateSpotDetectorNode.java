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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Map<String, Object> settings = new HashMap<>();
        for (Map.Entry<String, JIPipeParameterAccess> entry : parameters.getParameters().entrySet()) {
            Object parameterValue = JIPipe.duplicateParameter(entry.getValue().get(Object.class));
            Object settingValue = nodeInfo.getSettingsIOMap().get(entry.getKey()).parameterToSetting(parameterValue);
            settings.put(entry.getKey(), settingValue);
        }
        SpotDetectorData spotDetectorData = new SpotDetectorData(nodeInfo.getSpotDetectorFactory(), settings);
        dataBatch.addOutputData(getFirstOutputSlot(), spotDetectorData, progressInfo);
    }

    @JIPipeDocumentation(name = "Spot detector settings")
    @JIPipeParameter("spot-detector-parameters")
    public JIPipeDynamicParameterCollection getParameters() {
        return parameters;
    }
}
