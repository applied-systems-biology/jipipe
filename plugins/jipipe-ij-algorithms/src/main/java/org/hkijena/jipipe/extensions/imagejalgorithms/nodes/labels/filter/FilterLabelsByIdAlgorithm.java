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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels.filter;

import com.google.common.primitives.Ints;
import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
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
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejalgorithms.utils.ImageJAlgorithmUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.library.primitives.BooleanParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;

@SetJIPipeDocumentation(name = "Filter labels", description = "Allows to keep only a specific set of labels.")
@ConfigureJIPipeNode(menuPath = "Labels\nFilter", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images")
public class FilterLabelsByIdAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntegerRange values = new IntegerRange();
    private boolean keepValues = true;

    public FilterLabelsByIdAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public FilterLabelsByIdAlgorithm(FilterLabelsByIdAlgorithm other) {
        super(other);
        this.values = new IntegerRange(other.values);
        this.keepValues = other.keepValues;
    }

    @SetJIPipeDocumentation(name = "Label values", description = "The label values to be kept/to be removed")
    @JIPipeParameter(value = "values", important = true)
    public IntegerRange getValues() {
        return values;
    }

    @JIPipeParameter("values")
    public void setValues(IntegerRange values) {
        this.values = values;
    }

    @SetJIPipeDocumentation(name = "Mode", description = "Determines if labels are removed or kept.")
    @BooleanParameterSettings(comboBoxStyle = true, trueLabel = "Keep labels", falseLabel = "Remove labels")
    @JIPipeParameter("keep-values")
    public boolean isKeepValues() {
        return keepValues;
    }

    @JIPipeParameter("keep-values")
    public void setKeepValues(boolean keepValues) {
        this.keepValues = keepValues;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus outputImage = ImageJUtils.duplicate(inputImage);
        outputImage.setTitle(inputImage.getTitle());
        int[] ints = Ints.toArray(values.getIntegers(0, 0, new JIPipeExpressionVariablesMap()));
        if (keepValues) {
            ImageJUtils.forEachIndexedZCTSlice(outputImage, (ip, index) -> ImageJAlgorithmUtils.removeLabelsExcept(ip, ints), progressInfo);
        } else {
            LabelImages.replaceLabels(outputImage, ints, 0);
        }
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }
}
