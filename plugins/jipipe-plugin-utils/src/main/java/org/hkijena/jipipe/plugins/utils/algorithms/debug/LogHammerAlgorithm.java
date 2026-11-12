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

package org.hkijena.jipipe.plugins.utils.algorithms.debug;


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
import org.hkijena.jipipe.api.nodes.taxonomy.functions.DebuggingFunction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;

import java.util.List;

@SetJIPipeDocumentation(name = "Log hammer", description = "Hammers the log with messages")
@ConfigureJIPipeNode(function = DebuggingFunction.class)
@AddJIPipeInputSlot(value = JIPipeData.class, name = "Input", create = true, optional = true)
@AddJIPipeOutputSlot(value = JIPipeData.class, name = "Output", create = true)
public class LogHammerAlgorithm extends JIPipeParameterSlotAlgorithm {

    private int numEntries = 10000;
    private int waitPerEntry = 0;

    public LogHammerAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public LogHammerAlgorithm(LogHammerAlgorithm other) {
        super(other);
        this.numEntries =  other.numEntries;
        this.waitPerEntry =  other.waitPerEntry;
    }

    @Override
    public void runParameterSet(JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo, List<JIPipeTextAnnotation> parameterAnnotations) {
        for (int i = 0; i < numEntries; i++) {
            progressInfo.log(getClass().toString() + " - log entry " + i);
            if(waitPerEntry > 0) {
                try {
                    Thread.sleep(waitPerEntry);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            if(progressInfo.isCancelled()) {
                return;
            }
        }
        getFirstOutputSlot().addDataFromSlot(getFirstInputSlot(), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Created entries", description = "The number of log items to create")
    @JIPipeParameter("num-entries")
    public int getNumEntries() {
        return numEntries;
    }

    @JIPipeParameter("num-entries")
    public void setNumEntries(int numEntries) {
        this.numEntries = numEntries;
    }

    @SetJIPipeDocumentation(name = "Wait per entry (ms)", description = "Milliseconds to wait after each entry")
    @JIPipeParameter("wait-per-entry-ms")
    public int getWaitPerEntry() {
        return waitPerEntry;
    }

    @JIPipeParameter("wait-per-entry-ms")
    public void setWaitPerEntry(int waitPerEntry) {
        this.waitPerEntry = waitPerEntry;
    }
}
