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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.labels;

import ij.ImagePlus;
import inra.ijpb.label.LabelImages;
import org.hkijena.jipipe.api.AddJIPipeCitation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.AnnotationsNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscaleData;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Annotate with label overlap", description = "Compares two label or binary images and calculates " +
        "measurements of their overlap error or agreement. Measurements include: " +
        "<ul>" +
        "<li>Overlap</li>" +
        "<li>Jaccard index</li>" +
        "<li>Dice coefficient</li>" +
        "<li>Volume Similarity</li>" +
        "<li>False Negative Error</li>" +
        "<li>False Positive Error</li>" +
        "</ul>")
@AddJIPipeCitation("See https://imagej.net/plugins/morpholibj#label-overlap-measures for the formulas")
@ConfigureJIPipeNode(menuPath = "For images", nodeTypeCategory = AnnotationsNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, name = "Image 1", create = true)
@AddJIPipeInputSlot(value = ImagePlus3DGreyscaleData.class, name = "Image 2", create = true)
@AddJIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, name = "Image 1", create = true)
@AddJIPipeOutputSlot(value = ImagePlus3DGreyscaleData.class, name = "Image 2", create = true)
@AddJIPipeCitation("Legland, D.; Arganda-Carreras, I. & Andrey, P. (2016), \"MorphoLibJ: integrated library and plugins for mathematical morphology with ImageJ\", " +
        "Bioinformatics (Oxford Univ Press) 32(22): 3532-3534, PMID 27412086, doi:10.1093/bioinformatics/btw413")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Plugins\nMorphoLibJ\nAnalyze", aliasName = "Label Overlap Measures (to annotations)")
public class AnnotateWithOverlapMeasureLabelsAlgorithm extends JIPipeIteratingAlgorithm {

    private OptionalTextAnnotationNameParameter totalOverlapAnnotation = new OptionalTextAnnotationNameParameter("Total overlap", false);
    private OptionalTextAnnotationNameParameter jaccardIndexAnnotation = new OptionalTextAnnotationNameParameter("Jaccard index", false);
    private OptionalTextAnnotationNameParameter diceCoefficientAnnotation = new OptionalTextAnnotationNameParameter("Dice coefficient", true);
    private OptionalTextAnnotationNameParameter volumeSimilarityAnnotation = new OptionalTextAnnotationNameParameter("Volume similarity", false);
    private OptionalTextAnnotationNameParameter falseNegativeErrorAnnotation = new OptionalTextAnnotationNameParameter("False negative error", false);
    private OptionalTextAnnotationNameParameter falsePositiveErrorAnnotation = new OptionalTextAnnotationNameParameter("False positive error", false);

    public AnnotateWithOverlapMeasureLabelsAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public AnnotateWithOverlapMeasureLabelsAlgorithm(AnnotateWithOverlapMeasureLabelsAlgorithm other) {
        super(other);
        this.totalOverlapAnnotation = new OptionalTextAnnotationNameParameter(other.totalOverlapAnnotation);
        this.jaccardIndexAnnotation = new OptionalTextAnnotationNameParameter(other.jaccardIndexAnnotation);
        this.diceCoefficientAnnotation = new OptionalTextAnnotationNameParameter(other.diceCoefficientAnnotation);
        this.volumeSimilarityAnnotation = new OptionalTextAnnotationNameParameter(other.volumeSimilarityAnnotation);
        this.falseNegativeErrorAnnotation = new OptionalTextAnnotationNameParameter(other.falseNegativeErrorAnnotation);
        this.falsePositiveErrorAnnotation = new OptionalTextAnnotationNameParameter(other.falsePositiveErrorAnnotation);
    }

    @SetJIPipeDocumentation(name = "Calculate total overlap", description = "If enabled, the total overlap will be annotated to the data.")
    @JIPipeParameter("total-overlap-annotation")
    public OptionalTextAnnotationNameParameter getTotalOverlapAnnotation() {
        return totalOverlapAnnotation;
    }

    @JIPipeParameter("total-overlap-annotation")
    public void setTotalOverlapAnnotation(OptionalTextAnnotationNameParameter totalOverlapAnnotation) {
        this.totalOverlapAnnotation = totalOverlapAnnotation;
    }

    @SetJIPipeDocumentation(name = "Calculate Jaccard Index", description = "If enabled, the Jaccard Index will be annotated to the data.")
    @JIPipeParameter("jaccard-index-annotation")
    public OptionalTextAnnotationNameParameter getJaccardIndexAnnotation() {
        return jaccardIndexAnnotation;
    }

    @JIPipeParameter("jaccard-index-annotation")
    public void setJaccardIndexAnnotation(OptionalTextAnnotationNameParameter jaccardIndexAnnotation) {
        this.jaccardIndexAnnotation = jaccardIndexAnnotation;
    }

    @SetJIPipeDocumentation(name = "Calculate Dice Coefficient", description = "If enabled, the Dice Coefficient will be annotated to the data.")
    @JIPipeParameter("dice-coefficient-annotation")
    public OptionalTextAnnotationNameParameter getDiceCoefficientAnnotation() {
        return diceCoefficientAnnotation;
    }

    @JIPipeParameter("dice-coefficient-annotation")
    public void setDiceCoefficientAnnotation(OptionalTextAnnotationNameParameter diceCoefficientAnnotation) {
        this.diceCoefficientAnnotation = diceCoefficientAnnotation;
    }

    @SetJIPipeDocumentation(name = "Calculate volume similarity", description = "If enabled, the volume similarity will be annotated to the data.")
    @JIPipeParameter("volume-similarity-annotation")
    public OptionalTextAnnotationNameParameter getVolumeSimilarityAnnotation() {
        return volumeSimilarityAnnotation;
    }

    @JIPipeParameter("volume-similarity-annotation")
    public void setVolumeSimilarityAnnotation(OptionalTextAnnotationNameParameter volumeSimilarityAnnotation) {
        this.volumeSimilarityAnnotation = volumeSimilarityAnnotation;
    }

    @SetJIPipeDocumentation(name = "Calculate false negative error", description = "If enabled, the false negative error will be annotated to the data.")
    @JIPipeParameter("false-negative-error-annotation")
    public OptionalTextAnnotationNameParameter getFalseNegativeErrorAnnotation() {
        return falseNegativeErrorAnnotation;
    }

    @JIPipeParameter("false-negative-error-annotation")
    public void setFalseNegativeErrorAnnotation(OptionalTextAnnotationNameParameter falseNegativeErrorAnnotation) {
        this.falseNegativeErrorAnnotation = falseNegativeErrorAnnotation;
    }

    @SetJIPipeDocumentation(name = "Calculate false positive error", description = "If enabled, the false positive error will be annotated to the data.")
    @JIPipeParameter("false-positive-error-annotation")
    public OptionalTextAnnotationNameParameter getFalsePositiveErrorAnnotation() {
        return falsePositiveErrorAnnotation;
    }

    @JIPipeParameter("false-positive-error-annotation")
    public void setFalsePositiveErrorAnnotation(OptionalTextAnnotationNameParameter falsePositiveErrorAnnotation) {
        this.falsePositiveErrorAnnotation = falsePositiveErrorAnnotation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus sourceImage = iterationStep.getInputData("Image 1", ImagePlus3DGreyscaleData.class, progressInfo).getImage();
        ImagePlus targetImage = iterationStep.getInputData("Image 2", ImagePlus3DGreyscaleData.class, progressInfo).getImage();

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

        iterationStep.addOutputData("Image 1", new ImagePlus3DGreyscaleData(sourceImage), annotations, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
        iterationStep.addOutputData("Image 2", new ImagePlus3DGreyscaleData(targetImage), annotations, JIPipeTextAnnotationMergeMode.OverwriteExisting, progressInfo);
    }
}
