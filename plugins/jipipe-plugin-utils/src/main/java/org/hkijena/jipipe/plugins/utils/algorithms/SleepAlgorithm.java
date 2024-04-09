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

package org.hkijena.jipipe.plugins.utils.algorithms;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeParameterSlotAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.List;

@SetJIPipeDocumentation(name = "Wait", description = "Waits/sleeps the specified time and proceeds to pass the input to the output unchanged")
@ConfigureJIPipeNode(nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@AddJIPipeInputSlot(value = JIPipeData.class, slotName = "Input", create = true, optional = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, slotName = "Output", create = true)
public class SleepAlgorithm extends JIPipeParameterSlotAlgorithm {

    private int timeout = 5000;

    public SleepAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public SleepAlgorithm(SleepAlgorithm other) {
        super(other);
        this.timeout = other.timeout;
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        if (timeout >= 0) {
            progressInfo.log("Waiting for " + timeout + "ms");
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        getFirstOutputSlot().addDataFromSlot(getFirstInputSlot(), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Timeout (ms)", description = "The time the algorithm waits in milliseconds. Zero or negative values disable the sleep.")
    @JIPipeParameter("timeout")
    public int getTimeout() {
        return timeout;
    }

    @JIPipeParameter("timeout")
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
