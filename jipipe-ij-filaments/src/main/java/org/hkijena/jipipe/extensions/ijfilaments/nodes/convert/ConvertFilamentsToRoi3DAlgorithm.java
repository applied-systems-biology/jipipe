package org.hkijena.jipipe.extensions.ijfilaments.nodes.convert;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ij3d.datatypes.ROI3DListData;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;

@JIPipeDocumentation(name = "Convert filaments to 3D ROI", description = "Converts filaments into a 3D ROI")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROI3DListData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Convert")
public class ConvertFilamentsToRoi3DAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    private boolean thinLines = false;
    private boolean withEdges = true;
    private boolean withVertices = true;

    public ConvertFilamentsToRoi3DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertFilamentsToRoi3DAlgorithm(ConvertFilamentsToRoi3DAlgorithm other) {
        super(other);
        this.thinLines = other.thinLines;
        this.withEdges = other.withEdges;
        this.withVertices = other.withVertices;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Filaments3DData inputData = dataBatch.getInputData(getFirstInputSlot(), Filaments3DData.class, progressInfo);
        ROI3DListData outputData = inputData.toRoi3D(withEdges, withVertices, thinLines, progressInfo);

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    @JIPipeDocumentation(name = "Thin lines", description = "If enabled, the generated ROI will have thin lines instead of utilizing the thickness of the involved vertices")
    @JIPipeParameter("thin-lines")
    public boolean isThinLines() {
        return thinLines;
    }

    @JIPipeParameter("thin-lines")
    public void setThinLines(boolean thinLines) {
        this.thinLines = thinLines;
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
