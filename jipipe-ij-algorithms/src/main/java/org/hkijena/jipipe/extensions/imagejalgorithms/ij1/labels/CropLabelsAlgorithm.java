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

import gnu.trove.set.hash.TIntHashSet;
import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerRange;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Crop labels", description = "Crops all or a specific subset of labels from the image.")
@JIPipeNode(menuPath = "Labels", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscaleData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusGreyscaleData.class, slotName = "Output", autoCreate = true)
public class CropLabelsAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private OptionalIntegerRange labelIdLimit = new OptionalIntegerRange();
    private int border = 0;
    private OptionalAnnotationNameParameter labelIdAnnotation = new OptionalAnnotationNameParameter("Label ID", true);

    public CropLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public CropLabelsAlgorithm(CropLabelsAlgorithm other) {
        super(other);
        this.labelIdLimit = new OptionalIntegerRange(other.labelIdLimit);
        this.border = other.border;
        this.labelIdAnnotation = new OptionalAnnotationNameParameter(other.labelIdAnnotation);
    }

    @JIPipeDocumentation(name = "Limit label IDs", description = "Allows to determine which label IDs should be extracted")
    @JIPipeParameter("label-id-limit")
    public OptionalIntegerRange getLabelIdLimit() {
        return labelIdLimit;
    }

    @JIPipeParameter("label-id-limit")
    public void setLabelIdLimit(OptionalIntegerRange labelIdLimit) {
        this.labelIdLimit = labelIdLimit;
    }

    @JIPipeDocumentation(name = "Border size", description = "Border size in pixels around each extracted label")
    @JIPipeParameter("border")
    public int getBorder() {
        return border;
    }

    @JIPipeParameter("border")
    public void setBorder(int border) {
        this.border = border;
    }

    @JIPipeDocumentation(name = "Annotate with label ID", description = "If enabled, an annotation with the label ID is created for each output")
    @JIPipeParameter("label-id-annotation")
    public OptionalAnnotationNameParameter getLabelIdAnnotation() {
        return labelIdAnnotation;
    }

    @JIPipeParameter("label-id-annotation")
    public void setLabelIdAnnotation(OptionalAnnotationNameParameter labelIdAnnotation) {
        this.labelIdAnnotation = labelIdAnnotation;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();
        TIntHashSet knownLabels = new TIntHashSet(LabelImages.findAllLabels(inputImage));
        if (labelIdLimit.isEnabled()) {
            for (Integer i : labelIdLimit.getContent().getIntegers(0, 0)) {
                if (!knownLabels.contains(i))
                    continue;
                ImagePlus cropped = LabelImages.cropLabel(inputImage, i, border);
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                labelIdAnnotation.addAnnotationIfEnabled(annotations, "" + i);
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(cropped), annotations, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
            }
        } else {
            for (int i : knownLabels.toArray()) {
                ImagePlus cropped = LabelImages.cropLabel(inputImage, i, border);
                List<JIPipeTextAnnotation> annotations = new ArrayList<>();
                labelIdAnnotation.addAnnotationIfEnabled(annotations, "" + i);
                dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusGreyscaleData(cropped), annotations, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
            }
        }
    }
}
