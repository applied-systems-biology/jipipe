/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.labels;

import com.google.common.primitives.Ints;
import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.generators.IntegerRange;
import org.hkijena.jipipe.extensions.parameters.primitives.BooleanParameterSettings;

@JIPipeDocumentation(name = "Filter labels", description = "Allows to keep only a specific set of labels.")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
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

    @JIPipeDocumentation(name = "Label values", description = "The label values to be kept/to be removed")
    @JIPipeParameter(value = "values", important = true)
    public IntegerRange getValues() {
        return values;
    }

    @JIPipeParameter("values")
    public void setValues(IntegerRange values) {
        this.values = values;
    }

    @JIPipeDocumentation(name = "Mode", description = "Determines if labels are removed or kept.")
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
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus outputImage = inputImage.duplicate();
        outputImage.setTitle(inputImage.getTitle());
        if (keepValues) {
            LabelImages.keepLabels(outputImage, Ints.toArray(values.getIntegers()));
        } else {
            LabelImages.replaceLabels(outputImage, Ints.toArray(values.getIntegers()), 0);
        }
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }
}
