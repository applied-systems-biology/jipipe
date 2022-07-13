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

import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.JIPipeCitation;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Annotate with label overlap", description = "Compares two label or binary images and calculates " +
        "measurements of their overlap error or agreement. Measurements include: " +
        "<ul>" +
        "<li>Overlap</li>" +
        "<li>Jaccard index</li>" +
        "<li>Dice coefficient</li>" +
        "<li>Volume Similarity</li>" +
        "<li>False Negative Error</li>" +
        "<li>False Positive Error</li>" +
        "</ul>")
@JIPipeCitation("See https://imagej.net/plugins/morpholibj#label-overlap-measures for the formulas")
@JIPipeNode(menuPath = "Generate", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Image 1", autoCreate = true)
@JIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Image 2", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Image 1", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, slotName = "Image 2", autoCreate = true)
@JIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nAnalyze", aliasName = "Label Overlap Measures (to annotations)")
public class AnnotateWithOverlapMeasureLabelsAlgorithm extends JIPipeIteratingAlgorithm {

    private OptionalAnnotationNameParameter totalOverlapAnnotation = new OptionalAnnotationNameParameter("Total overlap", false);
    private OptionalAnnotationNameParameter jaccardIndexAnnotation = new OptionalAnnotationNameParameter("Jaccard index", false);
    private OptionalAnnotationNameParameter diceCoefficientAnnotation = new OptionalAnnotationNameParameter("Dice coefficient", true);
    private OptionalAnnotationNameParameter volumeSimilarityAnnotation = new OptionalAnnotationNameParameter("Volume similarity", false);
    private OptionalAnnotationNameParameter falseNegativeErrorAnnotation = new OptionalAnnotationNameParameter("False negative error", false);
    private OptionalAnnotationNameParameter falsePositiveErrorAnnotation = new OptionalAnnotationNameParameter("False positive error", false);

    public AnnotateWithOverlapMeasureLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateWithOverlapMeasureLabelsAlgorithm(AnnotateWithOverlapMeasureLabelsAlgorithm other) {
        super(other);
        this.totalOverlapAnnotation = new OptionalAnnotationNameParameter(other.totalOverlapAnnotation);
        this.jaccardIndexAnnotation = new OptionalAnnotationNameParameter(other.jaccardIndexAnnotation);
        this.diceCoefficientAnnotation = new OptionalAnnotationNameParameter(other.diceCoefficientAnnotation);
        this.volumeSimilarityAnnotation = new OptionalAnnotationNameParameter(other.volumeSimilarityAnnotation);
        this.falseNegativeErrorAnnotation = new OptionalAnnotationNameParameter(other.falseNegativeErrorAnnotation);
        this.falsePositiveErrorAnnotation = new OptionalAnnotationNameParameter(other.falsePositiveErrorAnnotation);
    }

    @JIPipeDocumentation(name = "Calculate total overlap", description = "If enabled, the total overlap will be annotated to the data.")
    @JIPipeParameter("total-overlap-annotation")
    public OptionalAnnotationNameParameter getTotalOverlapAnnotation() {
        return totalOverlapAnnotation;
    }

    @JIPipeParameter("total-overlap-annotation")
    public void setTotalOverlapAnnotation(OptionalAnnotationNameParameter totalOverlapAnnotation) {
        this.totalOverlapAnnotation = totalOverlapAnnotation;
    }

    @JIPipeDocumentation(name = "Calculate Jaccard Index", description = "If enabled, the Jaccard Index will be annotated to the data.")
    @JIPipeParameter("jaccard-index-annotation")
    public OptionalAnnotationNameParameter getJaccardIndexAnnotation() {
        return jaccardIndexAnnotation;
    }

    @JIPipeParameter("jaccard-index-annotation")
    public void setJaccardIndexAnnotation(OptionalAnnotationNameParameter jaccardIndexAnnotation) {
        this.jaccardIndexAnnotation = jaccardIndexAnnotation;
    }

    @JIPipeDocumentation(name = "Calculate Dice Coefficient", description = "If enabled, the Dice Coefficient will be annotated to the data.")
    @JIPipeParameter("dice-coefficient-annotation")
    public OptionalAnnotationNameParameter getDiceCoefficientAnnotation() {
        return diceCoefficientAnnotation;
    }

    @JIPipeParameter("dice-coefficient-annotation")
    public void setDiceCoefficientAnnotation(OptionalAnnotationNameParameter diceCoefficientAnnotation) {
        this.diceCoefficientAnnotation = diceCoefficientAnnotation;
    }

    @JIPipeDocumentation(name = "Calculate volume similarity", description = "If enabled, the volume similarity will be annotated to the data.")
    @JIPipeParameter("volume-similarity-annotation")
    public OptionalAnnotationNameParameter getVolumeSimilarityAnnotation() {
        return volumeSimilarityAnnotation;
    }

    @JIPipeParameter("volume-similarity-annotation")
    public void setVolumeSimilarityAnnotation(OptionalAnnotationNameParameter volumeSimilarityAnnotation) {
        this.volumeSimilarityAnnotation = volumeSimilarityAnnotation;
    }

    @JIPipeDocumentation(name = "Calculate false negative error", description = "If enabled, the false negative error will be annotated to the data.")
    @JIPipeParameter("false-negative-error-annotation")
    public OptionalAnnotationNameParameter getFalseNegativeErrorAnnotation() {
        return falseNegativeErrorAnnotation;
    }

    @JIPipeParameter("false-negative-error-annotation")
    public void setFalseNegativeErrorAnnotation(OptionalAnnotationNameParameter falseNegativeErrorAnnotation) {
        this.falseNegativeErrorAnnotation = falseNegativeErrorAnnotation;
    }

    @JIPipeDocumentation(name = "Calculate false positive error", description = "If enabled, the false positive error will be annotated to the data.")
    @JIPipeParameter("false-positive-error-annotation")
    public OptionalAnnotationNameParameter getFalsePositiveErrorAnnotation() {
        return falsePositiveErrorAnnotation;
    }

    @JIPipeParameter("false-positive-error-annotation")
    public void setFalsePositiveErrorAnnotation(OptionalAnnotationNameParameter falsePositiveErrorAnnotation) {
        this.falsePositiveErrorAnnotation = falsePositiveErrorAnnotation;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus sourceImage = dataBatch.getInputData("Image 1", ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        ImagePlus targetImage = dataBatch.getInputData("Image 2", ImagePlus3DGreyscaleData.class, progressInfo).getImage();

        List<JIPipeTextAnnotation> annotations = new ArrayList<>();

        if (totalOverlapAnnotation.isEnabled()) // Overlap
        {
            totalOverlapAnnotation.addAnnotationIfEnabled(annotations, LabelImages.getTotalOverlap(sourceImage, targetImage) + "");
        }

        if (jaccardIndexAnnotation.isEnabled()) // Jaccard index
        {
            jaccardIndexAnnotation.addAnnotationIfEnabled(annotations, LabelImages.getJaccardIndex(sourceImage, targetImage) + "");
        }

        if (diceCoefficientAnnotation.isEnabled()) // Dice coefficient
        {
            diceCoefficientAnnotation.addAnnotationIfEnabled(annotations, LabelImages.getDiceCoefficient(sourceImage, targetImage) + "");
        }

        if (volumeSimilarityAnnotation.isEnabled()) // Volume similarity
        {
            volumeSimilarityAnnotation.addAnnotationIfEnabled(annotations, LabelImages.getVolumeSimilarity(sourceImage, targetImage) + "");
        }

        if (falseNegativeErrorAnnotation.isEnabled()) // False negative error
        {
            falseNegativeErrorAnnotation.addAnnotationIfEnabled(annotations, LabelImages.getFalseNegativeError(sourceImage, targetImage) + "");
        }

        if (falsePositiveErrorAnnotation.isEnabled()) // False positive error
        {
            falsePositiveErrorAnnotation.addAnnotationIfEnabled(annotations, LabelImages.getFalsePositiveError(sourceImage, targetImage) + "");
        }

        dataBatch.addOutputData("Image 1", new ImagePlus3DGreyscaleData(sourceImage), annotations, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        dataBatch.addOutputData("Image 2", new ImagePlus3DGreyscaleData(targetImage), annotations, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
    }
}
