package org.hkijena.jipipe.extensions.ijfilaments.nodes.convert;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;

@JIPipeDocumentation(name = "Convert filaments to ROI", description = "Converts filaments into 2D line ImageJ ROI")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Convert")
public class ConvertFilamentsToRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean ignoreNon2DEdges = false;

    private boolean thinLines = false;
    private boolean withEdges = true;
    private boolean withVertices = true;

    public ConvertFilamentsToRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertFilamentsToRoiAlgorithm(ConvertFilamentsToRoiAlgorithm other) {
        super(other);
        this.ignoreNon2DEdges = other.ignoreNon2DEdges;
        this.thinLines = other.thinLines;
        this.withEdges = other.withEdges;
        this.withVertices = other.withVertices;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData inputData = dataBatch.getInputData(getFirstInputSlot(), FilamentsData.class, progressInfo);
        ROIListData outputData = inputData.toRoi(ignoreNon2DEdges, withEdges, withVertices, thinLines);

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

    @JIPipeDocumentation(name = "Non-2D edge behavior", description = "Determines the operation if an edge between two Z, C, or T locations is encountered. Either the vertex is copied to both locations, or " +
            "the edge can be ignored")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Ignore affected edges", falseLabel = "Copy vertices to both locations")
    @JIPipeParameter("ignore-non-2d-edges")
    public boolean isIgnoreNon2DEdges() {
        return ignoreNon2DEdges;
    }

    @JIPipeParameter("ignore-non-2d-edges")
    public void setIgnoreNon2DEdges(boolean ignoreNon2DEdges) {
        this.ignoreNon2DEdges = ignoreNon2DEdges;
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
