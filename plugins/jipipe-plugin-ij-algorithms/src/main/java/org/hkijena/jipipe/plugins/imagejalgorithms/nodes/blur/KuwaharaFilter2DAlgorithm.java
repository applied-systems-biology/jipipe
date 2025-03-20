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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.blur;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeDataAnnotationMergeMode;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.Kuwahara_LinearStructure_Filter_v3;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale32FData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalDataAnnotationNameParameter;
import trainableSegmentation.filters.Kuwahara;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Wrapper around {@link RankFilters}
 */
@SetJIPipeDocumentation(name = "Kuwahara filter 2D", description = "Applies a Kuwahara filter, a noise-reduction filter that preserves edges. " +
        "Also supports the linear Kuwahara algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@ConfigureJIPipeNode(menuPath = "Filter", nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeInputSlot(value = ImagePlusGreyscale32FData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscale32FData.class, name = "Output", create = true)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nFilters", aliasName = "Kuwahara Filter")
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Process\nFilters", aliasName = "Linear Kuwahara")
public class KuwaharaFilter2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private int numberOfAngles = 30;
    private int lineLength = 11;
    private Criterion criterion = Criterion.Variance;
    private OptionalDataAnnotationNameParameter kernelDataAnnotation = new OptionalDataAnnotationNameParameter("Kernels", false);
    private boolean linearKuwahara = false;

    public KuwaharaFilter2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public KuwaharaFilter2DAlgorithm(KuwaharaFilter2DAlgorithm other) {
        super(other);
        this.numberOfAngles = other.numberOfAngles;
        this.lineLength = other.lineLength;
        this.criterion = other.criterion;
        this.kernelDataAnnotation = new OptionalDataAnnotationNameParameter(other.kernelDataAnnotation);
        this.linearKuwahara = other.linearKuwahara;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImg = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscale32FData.class, progressInfo).getImage();
        ImagePlus outputImg;
        ImageStack kernel;

        if(linearKuwahara) {
            progressInfo.log("Using linear Kuwahara implementation");

            Kuwahara_LinearStructure_Filter_v3 kuwahara = new Kuwahara_LinearStructure_Filter_v3();
            kuwahara.setSize(lineLength);
            kuwahara.setnAngles(numberOfAngles);
            kuwahara.setCriterionMethod(criterion.nativeValue);

            kernel = kuwahara.createKernel(lineLength, numberOfAngles);

            outputImg = ImageJUtils.generateForEachIndexedZCTSlice(inputImg, (ip, index) -> {
                ImageProcessor copy = ip.duplicate();
                kuwahara.filter(copy, kernel);
                return copy;
            }, progressInfo);
        }
        else {
            progressInfo.log("Using regular Kuwahara implementation");
            Kuwahara kuwahara = new Kuwahara();
            kuwahara.setSize(lineLength);
            kuwahara.setNumberOfAngles(numberOfAngles);
            kuwahara.setCriterionMethod(criterion.nativeValue);

            kernel = kuwahara.createKernel(lineLength, numberOfAngles);

            outputImg = ImageJUtils.generateForEachIndexedZCTSlice(inputImg, (ip, index) -> {
                ImageProcessor copy = ip.duplicate();
                kuwahara.filter(copy, kernel);
                return copy;
            }, progressInfo);
        }



        List<JIPipeDataAnnotation> dataAnnotations = new ArrayList<>();
        if(kernelDataAnnotation.isEnabled()) {
            dataAnnotations.add(new JIPipeDataAnnotation(kernelDataAnnotation.getContent(), new ImagePlusData(new ImagePlus("Kernels", kernel))));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(outputImg), Collections.emptyList(), JIPipeTextAnnotationMergeMode.OverwriteExisting,
                dataAnnotations, JIPipeDataAnnotationMergeMode.Merge, progressInfo);
    }

    @SetJIPipeDocumentation(name = "Number of angles", description = "The number of angles. " +
            "The higher the number, the better the results, but with the cost of additional performance penalty.")
    @JIPipeParameter("number-of-angles")
    public int getNumberOfAngles() {
        return numberOfAngles;
    }

    @JIPipeParameter("number-of-angles")
    public void setNumberOfAngles(int numberOfAngles) {
        this.numberOfAngles = numberOfAngles;
    }

    @SetJIPipeDocumentation(name = "Line length", description = "The length of the line along which the averaging takes place. " +
            "Must be odd.")
    @JIPipeParameter("line-length")
    public int getLineLength() {
        return lineLength;
    }

    @JIPipeParameter("line-length")
    public void setLineLength(int lineLength) {
        this.lineLength = lineLength;
    }

    @SetJIPipeDocumentation(name = "Criterion", description = "The criterion for the filter")
    @JIPipeParameter("criterion")
    public Criterion getCriterion() {
        return criterion;
    }

    @JIPipeParameter("criterion")
    public void setCriterion(Criterion criterion) {
        this.criterion = criterion;
    }

    @SetJIPipeDocumentation(name = "Annotate with kernels", description = "If enabled, annotate with an image that contains the kernels")
    @JIPipeParameter("kernel-data-annotation")
    public OptionalDataAnnotationNameParameter getKernelDataAnnotation() {
        return kernelDataAnnotation;
    }

    @JIPipeParameter("kernel-data-annotation")
    public void setKernelDataAnnotation(OptionalDataAnnotationNameParameter kernelDataAnnotation) {
        this.kernelDataAnnotation = kernelDataAnnotation;
    }

    public enum Criterion {
        Variance("Variance", 0),
        VarianceOverMean("Variance / Mean", 1),
        VarianceOverMeanSq("Variance / (Mean * Mean)", 2);

        private final String label;
        private final int nativeValue;

        Criterion(String label, int nativeValue) {
            this.label = label;
            this.nativeValue = nativeValue;
        }

        public String getLabel() {
            return label;
        }

        public int getNativeValue() {
            return nativeValue;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
