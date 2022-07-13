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
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.IntRangeAndIntegerPairParameter;

@JIPipeDocumentation(name = "Replace label values", description = "Replaces label values by the specified value.")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
@JIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images", aliasName = "Replace/Remove Label(s)")
public class ReplaceLabelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntRangeAndIntegerPairParameter.List replacements = new IntRangeAndIntegerPairParameter.List();

    public ReplaceLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ReplaceLabelsAlgorithm(ReplaceLabelsAlgorithm other) {
        super(other);
        this.replacements = new IntRangeAndIntegerPairParameter.List(other.replacements);
    }

    @JIPipeDocumentation(name = "Replacements", description = "A list of replacement values. You can match multiple labels per rule.")
    @PairParameterSettings(keyLabel = "Current label", valueLabel = "New label")
    @JIPipeParameter(value = "replacements", important = true)
    public IntRangeAndIntegerPairParameter.List getReplacements() {
        return replacements;
    }

    @JIPipeParameter("replacements")
    public void setReplacements(IntRangeAndIntegerPairParameter.List replacements) {
        this.replacements = replacements;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        ImagePlus outputImage = ImageJUtils.duplicate(inputImage);
        outputImage.setTitle(inputImage.getTitle());
        for (IntRangeAndIntegerPairParameter replacement : replacements) {
            LabelImages.replaceLabels(outputImage, Ints.toArray(replacement.getKey().getIntegers(0, 0)), replacement.getValue());
        }
        outputImage.setDimensions(inputImage.getNChannels(), inputImage.getNSlices(), inputImage.getNFrames());
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(outputImage), progressInfo);
    }
}
