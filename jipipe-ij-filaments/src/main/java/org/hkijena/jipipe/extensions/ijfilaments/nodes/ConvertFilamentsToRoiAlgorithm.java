package org.hkijena.jipipe.extensions.ijfilaments.nodes;

import ij.gui.Line;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.RoiNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.FilamentsData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentEdge;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentVertex;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;

@JIPipeDocumentation(name = "Convert filaments to ROI", description = "Converts filaments into 2D line ImageJ ROI")
@JIPipeInputSlot(value = FilamentsData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ROIListData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = RoiNodeTypeCategory.class, menuPath = "Filaments\nConvert")
public class ConvertFilamentsToRoiAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private boolean ignoreNon2DEdges = false;

    public ConvertFilamentsToRoiAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ConvertFilamentsToRoiAlgorithm(ConvertFilamentsToRoiAlgorithm other) {
        super(other);
        this.ignoreNon2DEdges = other.ignoreNon2DEdges;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        FilamentsData inputData = dataBatch.getInputData(getFirstInputSlot(), FilamentsData.class, progressInfo);
        ROIListData outputData = new ROIListData();

        for (FilamentEdge edge : inputData.edgeSet()) {
            FilamentVertex edgeSource = inputData.getEdgeSource(edge);
            FilamentVertex edgeTarget = inputData.getEdgeTarget(edge);

            if(edgeSource.getCentroid().getZ() != edgeTarget.getCentroid().getZ() ||
                    edgeSource.getCentroid().getC() != edgeTarget.getCentroid().getC() ||
                    edgeSource.getCentroid().getT() != edgeTarget.getCentroid().getT()) {
                if(ignoreNon2DEdges)
                    continue;
                filamentToLine(inputData, edgeSource, edgeTarget, edge, edgeSource.getCentroid().getZ(), edgeSource.getCentroid().getC(), edgeSource.getCentroid().getT(), outputData);
                filamentToLine(inputData, edgeSource, edgeTarget, edge, edgeTarget.getCentroid().getZ(), edgeTarget.getCentroid().getC(), edgeTarget.getCentroid().getT(), outputData);
            }
            else {
                filamentToLine(inputData, edgeSource, edgeTarget, edge, edgeSource.getCentroid().getZ(), edgeSource.getCentroid().getC(), edgeSource.getCentroid().getT(), outputData);
            }
        }

        dataBatch.addOutputData(getFirstOutputSlot(), outputData, progressInfo);
    }

    private void filamentToLine(FilamentsData graph, FilamentVertex edgeSource, FilamentVertex edgeTarget, FilamentEdge edge, int z, int c, int t, ROIListData roiList) {
        Line roi = new Line(edgeSource.getCentroid().getX(), edgeSource.getCentroid().getY(), edgeTarget.getCentroid().getX(), edgeTarget.getCentroid().getY());
        roi.setStrokeWidth((edgeSource.getThickness() + edgeTarget.getThickness()) / 2);
        roi.setPosition(c + 1, z + 1, t + 1);
        roiList.add(roi);
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
}
