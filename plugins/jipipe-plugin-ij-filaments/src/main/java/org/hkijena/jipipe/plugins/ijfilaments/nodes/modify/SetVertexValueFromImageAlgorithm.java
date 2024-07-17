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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.modify;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.ijfilaments.parameters.VertexMaskParameter;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalStringParameter;
import org.hkijena.jipipe.utils.StringUtils;

@SetJIPipeDocumentation(name = "Set filament vertex value from image", description = "Sets the value/intensity of each vertex from the given input image. Please note that if the C/T coordinates are set to zero, the value is extracted from the 0/0 slice.")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Modify")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Filaments", create = true)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Intensity", description = "The value/intensity is sourced from the pixels in this image", create = true)
@AddJIPipeOutputSlot(value = Filaments3DGraphData.class, name = "Output", create = true)
public class SetVertexValueFromImageAlgorithm extends JIPipeIteratingAlgorithm {

    private final VertexMaskParameter vertexMask;
    private OptionalStringParameter backupOldValue = new OptionalStringParameter("old_value", false);

    public SetVertexValueFromImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.vertexMask = new VertexMaskParameter();
        registerSubParameter(vertexMask);
    }

    public SetVertexValueFromImageAlgorithm(SetVertexValueFromImageAlgorithm other) {
        super(other);
        this.backupOldValue = other.backupOldValue;
        this.vertexMask = new VertexMaskParameter(other.vertexMask);
        registerSubParameter(vertexMask);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData filaments = new Filaments3DGraphData(iterationStep.getInputData("Filaments", Filaments3DGraphData.class, progressInfo));
        ImagePlus intensity = iterationStep.getInputData("Intensity", ImagePlusGreyscaleData.class, progressInfo).getImage();
        JIPipeExpressionVariablesMap variablesMap = new JIPipeExpressionVariablesMap();
        variablesMap.putAnnotations(iterationStep.getMergedTextAnnotations());

        for (FilamentVertex vertex : vertexMask.filter(filaments, filaments.vertexSet(), variablesMap)) {
            int z = (int) Math.max(0, vertex.getSpatialLocation().getZ());
            int c = Math.max(0, vertex.getNonSpatialLocation().getChannel());
            int t = Math.max(0, vertex.getNonSpatialLocation().getFrame());
            ImageProcessor ip = ImageJUtils.getSliceZero(intensity, c, z, t);
            double d = ip.getf((int) vertex.getSpatialLocation().getX(), (int) vertex.getSpatialLocation().getY());
            if(backupOldValue.isEnabled()) {
                vertex.getValueBackups().put(StringUtils.nullToEmpty(backupOldValue.getContent()), d);
            }
            vertex.setValue(d);
        }

        iterationStep.addOutputData(getFirstOutputSlot(), filaments, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Backup old value", description = "If enabled, backup the value to the specified storage")
    @JIPipeParameter("backup-old-value")
    @StringParameterSettings(monospace = true)
    public OptionalStringParameter getBackupOldValue() {
        return backupOldValue;
    }

    @JIPipeParameter("backup-old-value")
    public void setBackupOldValue(OptionalStringParameter backupOldValue) {
        this.backupOldValue = backupOldValue;
    }

    @SetJIPipeDocumentation(name = "Vertex mask", description = "Used to filter vertices")
    @JIPipeParameter("vertex-filter")
    public VertexMaskParameter getVertexMask() {
        return vertexMask;
    }
}
