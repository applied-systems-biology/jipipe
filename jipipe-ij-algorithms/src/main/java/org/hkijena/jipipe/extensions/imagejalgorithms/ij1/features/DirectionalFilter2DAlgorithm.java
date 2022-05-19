package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.features;

import ij.ImagePlus;
import inra.ijpb.algo.AlgoEvent;
import inra.ijpb.algo.AlgoListener;
import inra.ijpb.morphology.directional.DirectionalFilter;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Directional filter 2D", description = "Filter that enhances curvilinear structures by applying oriented filters, while preserving their thickness. More information: https://imagej.net/plugins/morpholibj. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@JIPipeCitation("https://imagej.net/plugins/morpholibj")
@JIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@JIPipeCitation("Soille et al., 20012, Heneghan et al., 20023; Hendriks et al., 2003")
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class DirectionalFilter2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    int lineLength = 20;
    int nDirections = 32;
    private DirectionalFilter.Operation operation = DirectionalFilter.Operation.OPENING;
    private DirectionalFilter.Type type = DirectionalFilter.Type.MAX;

    public DirectionalFilter2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public DirectionalFilter2DAlgorithm(DirectionalFilter2DAlgorithm other) {
        super(other);
        this.operation = other.operation;
        this.type = other.type;
        this.lineLength = other.lineLength;
        this.nDirections = other.nDirections;
    }

    @JIPipeDocumentation(name = "Operation", description = "The operation to apply using each oriented structuring element")
    @JIPipeParameter(value = "operation", important = true)
    public DirectionalFilter.Operation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(DirectionalFilter.Operation operation) {
        this.operation = operation;
    }

    @JIPipeDocumentation(name = "Type", description = "Specifies how to combine the results for each oriented filter")
    @JIPipeParameter("type")
    public DirectionalFilter.Type getType() {
        return type;
    }

    @JIPipeParameter("type")
    public void setType(DirectionalFilter.Type type) {
        this.type = type;
    }

    @JIPipeDocumentation(name = "Line length", description = "The approximated length of the structuring element")
    @JIPipeParameter("line-length")
    public int getLineLength() {
        return lineLength;
    }

    @JIPipeParameter("line-length")
    public void setLineLength(int lineLength) {
        this.lineLength = lineLength;
    }

    @JIPipeDocumentation(name = "Number of directions", description = "The number of oriented structuring elements to consider. To be increased if the length of line is large.")
    @JIPipeParameter("n-directions")
    public int getnDirections() {
        return nDirections;
    }

    @JIPipeParameter("n-directions")
    public void setnDirections(int nDirections) {
        this.nDirections = nDirections;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        DirectionalFilter filter = new DirectionalFilter(this.type, this.operation, this.lineLength, this.nDirections);
        filter.addAlgoListener(new AlgoListener() {
            @Override
            public void algoProgressChanged(AlgoEvent evt) {
                progressInfo.log("Directional filter: " + (int) evt.getCurrentProgress() + "/" + (int) evt.getTotalProgress());
            }

            @Override
            public void algoStatusChanged(AlgoEvent evt) {
                progressInfo.log("Directional filter: " + evt.getStatus());
            }
        });
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus resultImage = ImageJUtils.generateForEachIndexedZCTSlice(inputImage, (ip, index) -> filter.process(ip), progressInfo);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(resultImage), progressInfo);
    }
}
