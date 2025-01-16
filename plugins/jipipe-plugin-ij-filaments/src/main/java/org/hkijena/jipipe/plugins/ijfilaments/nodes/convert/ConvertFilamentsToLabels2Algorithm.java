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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.convert;

import ij.ImagePlus;
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
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DGraphData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalIntegerParameter;

@SetJIPipeDocumentation(name = "Convert filaments to labels", description = "Converts filaments into a labels image.")
@AddJIPipeInputSlot(value = Filaments3DGraphData.class, name = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Reference", create = true, optional = true, description = "Optional reference image that determines the size of the mask")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Output", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Convert")
public class ConvertFilamentsToLabels2Algorithm extends JIPipeIteratingAlgorithm {
    private boolean withEdges = true;
    private boolean withVertices = true;
    private OptionalIntegerParameter forcedLineThickness = new OptionalIntegerParameter(false, 1);
    private OptionalIntegerParameter forcedVertexRadius = new OptionalIntegerParameter(false, 1);
    private boolean ignoreZ = false;
    private boolean ignoreC = false;
    private boolean ignoreT = false;
    private boolean hollowVertices = false;

    public ConvertFilamentsToLabels2Algorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertFilamentsToLabels2Algorithm(ConvertFilamentsToLabels2Algorithm other) {
        super(other);
        this.withEdges = other.withEdges;
        this.withVertices = other.withVertices;
        this.forcedVertexRadius = new OptionalIntegerParameter(other.forcedVertexRadius);
        this.forcedLineThickness = new OptionalIntegerParameter(other.forcedLineThickness);
        this.ignoreZ = other.ignoreZ;
        this.ignoreC = other.ignoreC;
        this.ignoreT = other.ignoreT;
        this.hollowVertices = other.hollowVertices;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DGraphData inputData = iterationStep.getInputData("Input", Filaments3DGraphData.class, progressInfo);
        ImagePlus reference = ImageJUtils.unwrap(iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo));

        ImagePlus mask = inputData.toLabels2(reference,
                withEdges,
                withVertices,
                forcedLineThickness.orElse(-1),
                forcedVertexRadius.orElse(-1),
                ignoreC,
                ignoreZ,
                ignoreT,
                hollowVertices,
                progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(mask), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Ignore Z", description = "If enabled, the Z location of each each vertex will be ignored")
    @JIPipeParameter("ignore-z")
    public boolean isIgnoreZ() {
        return ignoreZ;
    }

    @JIPipeParameter("ignore-z")
    public void setIgnoreZ(boolean ignoreZ) {
        this.ignoreZ = ignoreZ;
    }

    @SetJIPipeDocumentation(name = "Ignore channel", description = "If enabled, ignore the channel (C) location of each vertex")
    @JIPipeParameter("ignore-c")
    public boolean isIgnoreC() {
        return ignoreC;
    }

    @JIPipeParameter("ignore-c")
    public void setIgnoreC(boolean ignoreC) {
        this.ignoreC = ignoreC;
    }

    @SetJIPipeDocumentation(name = "Ignore frame", description = "If enabled, ignore the frame (T) location of each vertex")
    @JIPipeParameter("ignore-t")
    public boolean isIgnoreT() {
        return ignoreT;
    }

    @JIPipeParameter("ignore-t")
    public void setIgnoreT(boolean ignoreT) {
        this.ignoreT = ignoreT;
    }


    @SetJIPipeDocumentation(name = "Hollow vertices", description = "If enabled, draw vertices as hollow spheres")
    @JIPipeParameter("hollow-vertices")
    public boolean isHollowVertices() {
        return hollowVertices;
    }

    @JIPipeParameter("hollow-vertices")
    public void setHollowVertices(boolean hollowVertices) {
        this.hollowVertices = hollowVertices;
    }

    @SetJIPipeDocumentation(name = "Override edge thickness", description = "If enabled, set the thickness of edges. Must be at least zero.")
    @JIPipeParameter("forced-line-thickness")
    public OptionalIntegerParameter getForcedLineThickness() {
        return forcedLineThickness;
    }

    @JIPipeParameter("forced-line-thickness")
    public void setForcedLineThickness(OptionalIntegerParameter forcedLineThickness) {
        this.forcedLineThickness = forcedLineThickness;
    }

    @SetJIPipeDocumentation(name = "Override vertex radius", description = "If enabled, override the radius of vertices. Must be at least one.")
    @JIPipeParameter("forced-vertex-radius")
    public OptionalIntegerParameter getForcedVertexRadius() {
        return forcedVertexRadius;
    }

    @JIPipeParameter("forced-vertex-radius")
    public void setForcedVertexRadius(OptionalIntegerParameter forcedVertexRadius) {
        this.forcedVertexRadius = forcedVertexRadius;
    }

    @SetJIPipeDocumentation(name = "With edges", description = "If enabled, edges are converted to ROI")
    @JIPipeParameter("with-edges")
    public boolean isWithEdges() {
        return withEdges;
    }

    @JIPipeParameter("with-edges")
    public void setWithEdges(boolean withEdges) {
        this.withEdges = withEdges;
    }

    @SetJIPipeDocumentation(name = "With vertices", description = "If enabled, vertices are converted to ROI")
    @JIPipeParameter("with-vertices")
    public boolean isWithVertices() {
        return withVertices;
    }

    @JIPipeParameter("with-vertices")
    public void setWithVertices(boolean withVertices) {
        this.withVertices = withVertices;
    }
}
