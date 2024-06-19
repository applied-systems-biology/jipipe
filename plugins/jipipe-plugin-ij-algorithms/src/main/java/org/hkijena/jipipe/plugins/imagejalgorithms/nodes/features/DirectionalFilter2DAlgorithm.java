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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.features;

import ij.ImagePlus;
import inra.ijpb.algo.AlgoEvent;
import inra.ijpb.algo.AlgoListener;
import inra.ijpb.morphology.directional.DirectionalFilter;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Directional filter 2D", description = "Filter that enhances curvilinear structures by applying oriented filters, while preserving their thickness. More information: https://imagej.net/plugins/morpholibj. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@AddJIPipeCitation("https://imagej.net/plugins/morpholibj")
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeCitation("Soille et al., 20012, Heneghan et al., 20023; Hendriks et al., 2003")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nFiltering", aliasName = "Directional Filtering")
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

    @SetJIPipeDocumentation(name = "Operation", description = "The operation to apply using each oriented structuring element")
    @JIPipeParameter(value = "operation", important = true)
    public DirectionalFilter.Operation getOperation() {
        return operation;
    }

    @JIPipeParameter("operation")
    public void setOperation(DirectionalFilter.Operation operation) {
        this.operation = operation;
    }

    @SetJIPipeDocumentation(name = "Type", description = "Specifies how to combine the results for each oriented filter")
    @JIPipeParameter("type")
    public DirectionalFilter.Type getType() {
        return type;
    }

    @JIPipeParameter("type")
    public void setType(DirectionalFilter.Type type) {
        this.type = type;
    }

    @SetJIPipeDocumentation(name = "Line length", description = "The approximated length of the structuring element")
    @JIPipeParameter("line-length")
    public int getLineLength() {
        return lineLength;
    }

    @JIPipeParameter("line-length")
    public void setLineLength(int lineLength) {
        this.lineLength = lineLength;
    }

    @SetJIPipeDocumentation(name = "Number of directions", description = "The number of oriented structuring elements to consider. To be increased if the length of line is large.")
    @JIPipeParameter("n-directions")
    public int getnDirections() {
        return nDirections;
    }

    @JIPipeParameter("n-directions")
    public void setnDirections(int nDirections) {
        this.nDirections = nDirections;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
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
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus resultImage = ImageJUtils.generateForEachIndexedZCTSlice(inputImage, (ip, index) -> filter.process(ip), progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(resultImage), progressInfo);
    }
}
