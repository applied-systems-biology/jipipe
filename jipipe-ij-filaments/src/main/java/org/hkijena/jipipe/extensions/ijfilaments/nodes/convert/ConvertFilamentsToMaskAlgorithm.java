package org.hkijena.jipipe.extensions.ijfilaments.nodes.convert;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;

@JIPipeDocumentation(name = "Convert filaments to mask", description = "Converts filaments into a 3D ROI")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true, description = "Optional reference image that determines the size of the mask")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Convert")
public class ConvertFilamentsToMaskAlgorithm extends JIPipeIteratingAlgorithm {
    private boolean withEdges = true;
    private boolean withVertices = true;

    private OptionalIntegerParameter forcedLineThickness = new OptionalIntegerParameter(false, 1);

    private OptionalIntegerParameter forcedVertexRadius = new OptionalIntegerParameter(false, 1);

    public ConvertFilamentsToMaskAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertFilamentsToMaskAlgorithm(ConvertFilamentsToMaskAlgorithm other) {
        super(other);
        this.withEdges = other.withEdges;
        this.withVertices = other.withVertices;
        this.forcedVertexRadius = new OptionalIntegerParameter(other.forcedVertexRadius);
        this.forcedLineThickness = new OptionalIntegerParameter(other.forcedLineThickness);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = dataBatch.getInputData("Input", Filaments3DData.class, progressInfo);
        ImagePlus reference = ImageJUtils.unwrap(dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo));

        ImagePlus mask = inputData.toMask(reference, withEdges, withVertices, forcedLineThickness.orElse(-1), forcedVertexRadius.orElse(-1), progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleMaskData(mask), progressInfo);
    }

    @JIPipeDocumentation(name = "Override edge thickness", description = "If enabled, set the thickness of edges. Must be at least zero.")
    @JIPipeParameter("forced-line-thickness")
    public OptionalIntegerParameter getForcedLineThickness() {
        return forcedLineThickness;
    }

    @JIPipeParameter("forced-line-thickness")
    public void setForcedLineThickness(OptionalIntegerParameter forcedLineThickness) {
        this.forcedLineThickness = forcedLineThickness;
    }

    @JIPipeDocumentation(name = "Override vertex radius", description = "If enabled, override the radius of vertices. Must be at least one.")
    @JIPipeParameter("forced-vertex-radius")
    public OptionalIntegerParameter getForcedVertexRadius() {
        return forcedVertexRadius;
    }

    @JIPipeParameter("forced-vertex-radius")
    public void setForcedVertexRadius(OptionalIntegerParameter forcedVertexRadius) {
        this.forcedVertexRadius = forcedVertexRadius;
    }

    @JIPipeDocumentation(name = "With edges", description = "If enabled, edges are converted to ROI")
    @JIPipeParameter("with-edges")
    public boolean isWithEdges() {
        return withEdges;
    }

    @JIPipeParameter("with-edges")
    public void setWithEdges(boolean withEdges) {
        this.withEdges = withEdges;
    }

    @JIPipeDocumentation(name = "With vertices", description = "If enabled, vertices are converted to ROI")
    @JIPipeParameter("with-vertices")
    public boolean isWithVertices() {
        return withVertices;
    }

    @JIPipeParameter("with-vertices")
    public void setWithVertices(boolean withVertices) {
        this.withVertices = withVertices;
    }
}
