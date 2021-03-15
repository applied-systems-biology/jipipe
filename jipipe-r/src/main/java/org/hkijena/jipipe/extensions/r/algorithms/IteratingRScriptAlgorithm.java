package org.hkijena.jipipe.extensions.r.algorithms;

import com.github.rcaller.rstuff.RCaller;
import com.github.rcaller.rstuff.RCallerOptions;
import com.github.rcaller.rstuff.RCode;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.r.RExtensionSettings;
import org.hkijena.jipipe.extensions.r.parameters.RScriptParameter;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;

@JIPipeDocumentation(name = "R script (iterating)", description = "Allows to write an R script.")
@JIPipeOrganization(nodeTypeCategory = MiscellaneousNodeTypeCategory.class, menuPath = "R script")
public class IteratingRScriptAlgorithm extends JIPipeIteratingAlgorithm {

    private RScriptParameter script = new RScriptParameter();
    private RCaller rCaller;

    public IteratingRScriptAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
        .restrictInputTo(ResultsTableData.class)
        .restrictOutputTo(ResultsTableData.class, ImagePlusData.class)
        .build());
    }

    public IteratingRScriptAlgorithm(IteratingRScriptAlgorithm other) {
        super(other);
        this.script = new RScriptParameter(other.script);
    }

    @Override
    public void run(JIPipeProgressInfo progressInfo) {
        try {
            if(!isPassThrough()) {
                rCaller = RCaller.create(RExtensionSettings.createRCallerOptions());
            }
            super.run(progressInfo);
        }
        finally {
            rCaller = null;
        }
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        RCode code = RCode.create();
        rCaller.setRCode(code);
        // TODO: RCaller's default methods are not sufficient (cannot return multiple variables)
    }

    @JIPipeDocumentation(name = "Script")
    @JIPipeParameter("script")
    public RScriptParameter getScript() {
        return script;
    }

    @JIPipeParameter("script")
    public void setScript(RScriptParameter script) {
        this.script = script;
    }
}
