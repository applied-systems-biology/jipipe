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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.misc;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.JIPipeProject;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;
import org.hkijena.jipipe.extensions.pipelinerender.RenderPipelineRun;
import org.hkijena.jipipe.extensions.pipelinerender.RenderPipelineRunSettings;

import java.io.IOException;

@SetJIPipeDocumentation(name = "Render JIPipe project pipeline", description = "Creates a single-image render of a whole JIPipe pipeline project. " +
        "This node is equivalent to <code>Tools &gt; Project &gt; Export whole pipeline as *.png</code>. Please note that the pipelines should be arranged " +
        "in a space-efficient way to reduce the file sizes.")
@ConfigureJIPipeNode(menuPath = "Meta run", nodeTypeCategory = MiscellaneousNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, slotName = "Project file", create = true)
@AddJIPipeOutputSlot(value = ImagePlus2DColorRGBData.class, slotName = "Render", create = true)
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

    @SetJIPipeDocumentation(name = "Render settings", description = "Settings for the rendering process.")
    @JIPipeParameter("render-settings")
    public RenderPipelineRunSettings getSettings() {
        return settings;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        try {
            JIPipeProject project = JIPipeProject.loadProject(iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo).toPath(),
                    new UnspecifiedValidationReportContext(),
                    new JIPipeValidationReport(),
                    new JIPipeNotificationInbox());
            RenderPipelineRun run = new RenderPipelineRun(project, null, settings);
            run.setProgressInfo(progressInfo.detachProgress().resolve("Render"));
            run.run();
            iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus2DColorRGBData(new ImagePlus("Render", new ColorProcessor(run.getOutputImage()))), progressInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
