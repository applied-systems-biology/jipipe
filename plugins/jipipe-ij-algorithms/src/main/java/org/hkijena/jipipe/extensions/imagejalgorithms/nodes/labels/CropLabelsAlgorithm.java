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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.labels;

import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerRange;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Crop labels", description = "Crops all or a specific subset of labels from the image.")
@ConfigureJIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nLabel Images", aliasName = "Crop Label")
public class CropLabelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalIntegerRange labelIdLimit = new OptionalIntegerRange();
    private int border = 0;
    private OptionalTextAnnotationNameParameter labelIdAnnotation = new OptionalTextAnnotationNameParameter("Label ID", true);

    public CropLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CropLabelsAlgorithm(CropLabelsAlgorithm other) {
        super(other);
        this.labelIdLimit = new OptionalIntegerRange(other.labelIdLimit);
        this.border = other.border;
        this.labelIdAnnotation = new OptionalTextAnnotationNameParameter(other.labelIdAnnotation);
    }

    @SetJIPipeDocumentation(name = "Limit label IDs", description = "Allows to determine which label IDs should be extracted")
    @JIPipeParameter("label-id-limit")
    public OptionalIntegerRange getLabelIdLimit() {
        return labelIdLimit;
    }

    @JIPipeParameter("label-id-limit")
    public void setLabelIdLimit(OptionalIntegerRange labelIdLimit) {
        this.labelIdLimit = labelIdLimit;
    }

    @SetJIPipeDocumentation(name = "Border size", description = "Border size in pixels around each extracted label")
    @JIPipeParameter("border")
    public int getBorder() {
        return border;
    }

    @JIPipeParameter("border")
    public void setBorder(int border) {
        this.border = border;
    }

    @SetJIPipeDocumentation(name = "Annotate with label ID", description = "If enabled, an annotation with the label ID is created for each output")
    @JIPipeParameter("label-id-annotation")
    public OptionalTextAnnotationNameParameter getLabelIdAnnotation() {
        return labelIdAnnotation;
    }

    @JIPipeParameter("label-id-annotation")
    public void setLabelIdAnnotation(OptionalTextAnnotationNameParameter labelIdAnnotation) {
        this.labelIdAnnotation = labelIdAnnotation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        TIntHashSet knownLabels = new TIntHashSet(LabelImages.findAllLabels(inputImage));
        if (labelIdLimit.isEnabled()) {
            for (Integer i : labelIdLimit.getContent().getIntegers(0, 0, new JIPipeExpressionVariablesMap())) {
                if (!knownLabels.contains(i))
                    continue;
                ImagePlus cropped = LabelImages.cropLabel(inputImage, i, border);
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                labelIdAnnotation.addAnnotationIfEnabled(annotations, "" + i);
                iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(cropped), annotations, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
            }
        } else {
            for (int i : knownLabels.toArray()) {
                ImagePlus cropped = LabelImages.cropLabel(inputImage, i, border);
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                labelIdAnnotation.addAnnotationIfEnabled(annotations, "" + i);
                iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(cropped), annotations, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
            }
        }
    }
}
