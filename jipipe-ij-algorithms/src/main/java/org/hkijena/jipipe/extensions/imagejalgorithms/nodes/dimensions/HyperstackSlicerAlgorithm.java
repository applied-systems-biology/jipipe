package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.dimensions;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
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
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndices;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.ranges.IntegerRange;

import java.util.ArrayList;
import java.util.List;

@JIPipeDocumentation(name = "Reduce & split hyperstack", description = "Slices a hyperstack via a range of indices.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks")
public class HyperstackSlicerAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private IntegerRange indicesZ = new IntegerRange("0");
    private IntegerRange indicesC = new IntegerRange("0");
    private IntegerRange indicesT = new IntegerRange("0");
    private OptionalAnnotationNameParameter annotateZ = new OptionalAnnotationNameParameter("Z", true);
    private OptionalAnnotationNameParameter annotateC = new OptionalAnnotationNameParameter("C", true);
    private OptionalAnnotationNameParameter annotateT = new OptionalAnnotationNameParameter("T", true);

    public HyperstackSlicerAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public HyperstackSlicerAlgorithm(HyperstackSlicerAlgorithm other) {
        super(other);
        this.indicesC = new IntegerRange(other.indicesC);
        this.indicesZ = new IntegerRange(other.indicesZ);
        this.indicesT = new IntegerRange(other.indicesT);
        this.annotateZ = new OptionalAnnotationNameParameter(other.annotateZ);
        this.annotateC = new OptionalAnnotationNameParameter(other.annotateC);
        this.annotateT = new OptionalAnnotationNameParameter(other.annotateT);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeProgressInfo progressInfo) {
        ImagePlus img = iterationStep.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();

        // Collect indices
        ImageSliceIndices indices = new ImageSliceIndices();
        extractZ(indices, img.getNSlices());
        extractC(indices, img.getNChannels());
        extractT(indices, img.getNFrames());

        int numZ = indices.getZ().size();
        int numC = indices.getC().size();
        int numT = indices.getT().size();

        if (numZ * numC * numT == 0) {
            throw new RuntimeException("Resulting image is empty!");
        }

        ImageStack stack = new ImageStack(img.getWidth(), img.getHeight(),
                numZ * numC * numT);
        List<JIPipeTextAnnotation> annotations = new ArrayList<>();
        for (int z = 0; z < numZ; z++) {
            for (int c = 0; c < numC; c++) {
                for (int t = 0; t < numT; t++) {
                    int zSource = indices.getZ().get(z);
                    int cSource = indices.getC().get(c);
                    int tSource = indices.getT().get(t);
                    int sz = wrapNumber(zSource, img.getNSlices());
                    int sc = wrapNumber(cSource, img.getNChannels());
                    int st = wrapNumber(tSource, img.getNFrames());
                    int stackIndexResult = ImageJUtils.zeroSliceIndexToOneStackIndex(c, z, t, numC, numZ, numT);
                    ImageProcessor processor = ImageJUtils.getSliceZero(img, sc, sz, st).duplicate();
                    stack.setProcessor(processor, stackIndexResult);

                    annotateZ.addAnnotationIfEnabled(annotations, sz + "");
                    annotateC.addAnnotationIfEnabled(annotations, sc + "");
                    annotateT.addAnnotationIfEnabled(annotations, st + "");
                }
            }
        }

        ImagePlus resultImage = new ImagePlus("Slice", stack);
        resultImage.setDimensions(numC, numZ, numT);
        resultImage.copyScale(img);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
    }

    private int wrapNumber(int x, int w) {
        while (x < 0) {
            x += w;
        }
        return x % w;
    }

    private void extractZ(ImageSliceIndices indices, int maxZ) {
        indices.getZ().addAll(indicesZ.getIntegers(0, maxZ, new JIPipeExpressionVariablesMap()));
    }

    private void extractC(ImageSliceIndices indices, int maxC) {
        indices.getC().addAll(indicesC.getIntegers(0, maxC, new JIPipeExpressionVariablesMap()));
    }

    private void extractT(ImageSliceIndices indices, int maxT) {
        indices.getT().addAll(indicesT.getIntegers(0, maxT, new JIPipeExpressionVariablesMap()));
    }

    @JIPipeDocumentation(name = "Indices (Z)", description = "Array of Z indices to be included in the final image. All indices begin with zero. Indices outside the available range are automatically wrapped. Return an empty array to skip a slice.")
    @JIPipeParameter("indices-z")
    public IntegerRange getIndicesZ() {
        return indicesZ;
    }

    @JIPipeParameter("indices-z")
    public void setIndicesZ(IntegerRange indicesZ) {
        this.indicesZ = indicesZ;
    }

    @JIPipeDocumentation(name = "Indices (Channel)", description = "Array of channel/C indices to be included in the final image.. All indices begin with zero. Indices outside the available range are automatically wrapped. Return an empty array to skip a slice.")
    @JIPipeParameter("indices-c")
    public IntegerRange getIndicesC() {
        return indicesC;
    }

    @JIPipeParameter("indices-c")
    public void setIndicesC(IntegerRange indicesC) {
        this.indicesC = indicesC;
    }

    @JIPipeDocumentation(name = "Indices (Frames)", description = "Array of frame/T indices to be included in the final image.. All indices begin with zero. Indices outside the available range are automatically wrapped. Return an empty array to skip a slice.")
    @JIPipeParameter("indices-t")
    public IntegerRange getIndicesT() {
        return indicesT;
    }

    @JIPipeParameter("indices-t")
    public void setIndicesT(IntegerRange indicesT) {
        this.indicesT = indicesT;
    }

    @JIPipeDocumentation(name = "Annotate with Z indices", description = "If enabled, the output is annotated with the source Z slices (zero-based)")
    @JIPipeParameter("annotate-z")
    public OptionalAnnotationNameParameter getAnnotateZ() {
        return annotateZ;
    }

    @JIPipeParameter("annotate-z")
    public void setAnnotateZ(OptionalAnnotationNameParameter annotateZ) {
        this.annotateZ = annotateZ;
    }

    @JIPipeDocumentation(name = "Annotate with C indices", description = "If enabled, the output is annotated with the source channel slices (zero-based)")
    @JIPipeParameter("annotate-c")
    public OptionalAnnotationNameParameter getAnnotateC() {
        return annotateC;
    }

    @JIPipeParameter("annotate-c")
    public void setAnnotateC(OptionalAnnotationNameParameter annotateC) {
        this.annotateC = annotateC;
    }

    @JIPipeDocumentation(name = "Annotate with T indices", description = "If enabled, the output is annotated with the source frame slices (zero-based)")
    @JIPipeParameter("annotate-t")
    public OptionalAnnotationNameParameter getAnnotateT() {
        return annotateT;
    }

    @JIPipeParameter("annotate-t")
    public void setAnnotateT(OptionalAnnotationNameParameter annotateT) {
        this.annotateT = annotateT;
    }
}
