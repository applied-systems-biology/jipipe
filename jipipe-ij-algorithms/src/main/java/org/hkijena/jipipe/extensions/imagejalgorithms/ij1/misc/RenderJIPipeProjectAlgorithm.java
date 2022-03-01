package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.misc;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.extensions.pipelinerender.RenderPipelineRun;
import org.hkijena.jipipe.extensions.pipelinerender.RenderPipelineRunSettings;

import java.io.IOException;

@JIPipeDocumentation(name = "Render JIPipe project pipeline", description = "Creates a single-image render of a whole JIPipe pipeline project. " +
        "This node is equivalent to <code>Tools &gt; Project &gt; Export whole pipeline as *.png</code>. Please note that the pipelines should be arranged " +
        "in a space-efficient way to reduce the file sizes.")
@JIPipeNode(menuPath = "Meta run", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@JIPipeInputSlot(value = FileData.class, slotName = "Project file", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus2DColorRGBData.class, slotName = "Render", autoCreate = true)
public class RenderJIPipeProjectAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private final RenderPipelineRunSettings settings;

    public RenderJIPipeProjectAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.settings = new RenderPipelineRunSettings();
    }

    public RenderJIPipeProjectAlgorithm(RenderJIPipeProjectAlgorithm other) {
        super(other);
        this.settings = new RenderPipelineRunSettings(other.settings);
    }

    @JIPipeDocumentation(name = "Render settings", description = "Settings for the rendering process.")
    @JIPipeParameter("render-settings")
    public RenderPipelineRunSettings getSettings() {
        return settings;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        try {
            JIPipeProject project = JIPipeProject.loadProject(dataBatch.getInputData(getFirstInputSlot(), FileData.class, progressInfo).toPath(), new JIPipeIssueReport());
            RenderPipelineRun run = new RenderPipelineRun(project, null, settings);
            run.setProgressInfo(progressInfo.detachProgress().resolve("Render"));
            run.run();
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus2DColorRGBData(new ImagePlus("Render", new ColorProcessor(run.getOutputImage()))), progressInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
