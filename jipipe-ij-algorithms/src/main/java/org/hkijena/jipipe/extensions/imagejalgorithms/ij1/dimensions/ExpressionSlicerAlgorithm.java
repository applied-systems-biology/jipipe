package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

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
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.extensions.expressions.*;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndices;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalAnnotationNameParameter;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ParameterUtils;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

@JIPipeDocumentation(name = "Reduce & split hyperstack (Expression)", description = "Uses expressions to create custom stacks. There are three expressions, one for each Z, channel and frame plane that " +
        "return numeric indices of slices that should be generated. You can optionally enable to iterate over all incoming slices of the image to generate multiple stacks. If no iteration is enabled (default)," +
        " each expression is only executed for z=0,c=0,t=0.")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlusData.class, slotName = "Output", autoCreate = true)
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@JIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "Image\nStacks")
public class ExpressionSlicerAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private DefaultExpressionParameter expressionZ = new DefaultExpressionParameter("z");
    private DefaultExpressionParameter expressionC = new DefaultExpressionParameter("c");
    private DefaultExpressionParameter expressionT = new DefaultExpressionParameter("t");
    private boolean iteratePerZ = false;
    private boolean iteratePerC = false;
    private boolean iteratePerT = false;
    private boolean removeDuplicates = true;
    private OptionalAnnotationNameParameter annotateZ = new OptionalAnnotationNameParameter("Z", true);
    private OptionalAnnotationNameParameter annotateC = new OptionalAnnotationNameParameter("C", true);
    private OptionalAnnotationNameParameter annotateT = new OptionalAnnotationNameParameter("T", true);

    public ExpressionSlicerAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ExpressionSlicerAlgorithm(ExpressionSlicerAlgorithm other) {
        super(other);
        this.expressionC = new DefaultExpressionParameter(other.expressionC);
        this.expressionZ = new DefaultExpressionParameter(other.expressionZ);
        this.expressionT = new DefaultExpressionParameter(other.expressionT);
        this.iteratePerC = other.iteratePerC;
        this.iteratePerT = other.iteratePerT;
        this.iteratePerZ = other.iteratePerZ;
        this.removeDuplicates = other.removeDuplicates;
        this.annotateZ = new OptionalAnnotationNameParameter(other.annotateZ);
        this.annotateC = new OptionalAnnotationNameParameter(other.annotateC);
        this.annotateT = new OptionalAnnotationNameParameter(other.annotateT);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus img = dataBatch.getInputData(getFirstInputSlot(), ImagePlusData.class, progressInfo).getImage();

        // Collect indices
        List<ImageSliceIndices> imageSliceIndicesList = new ArrayList<>();
        ExpressionVariables parameters = new ExpressionVariables();
        parameters.set("width", img.getWidth());
        parameters.set("height", img.getHeight());
        parameters.set("size_z", img.getNSlices());
        parameters.set("size_c", img.getNChannels());
        parameters.set("size_t", img.getNFrames());
        for (int z = 0; z < img.getNSlices(); z++) {
            if (!iteratePerZ && z != 0)
                continue;
            for (int c = 0; c < img.getNChannels(); c++) {
                if (!iteratePerC && c != 0)
                    continue;
                for (int t = 0; t < img.getNFrames(); t++) {
                    if (!iteratePerT && t != 0)
                        continue;
                    parameters.set("z", z);
                    parameters.set("c", c);
                    parameters.set("t", t);

                    ImageSliceIndices indices = new ImageSliceIndices();
                    extractZ(parameters, indices);
                    extractC(parameters, indices);
                    extractT(parameters, indices);
                    imageSliceIndicesList.add(indices);
                }
            }
        }

        // Remove duplicates
        if (removeDuplicates) {
            imageSliceIndicesList = imageSliceIndicesList.stream().distinct().collect(Collectors.toList());
        }

        // Generate outputs
        for (int i = 0; i < imageSliceIndicesList.size(); i++) {
            JIPipeProgressInfo sliceInfo = progressInfo.resolveAndLog("Generating slice", i, imageSliceIndicesList.size());
            ImageSliceIndices indices = imageSliceIndicesList.get(i);

            int numZ = indices.getZ().size();
            int numC = indices.getC().size();
            int numT = indices.getT().size();

            if (numZ * numC * numT == 0) {
                sliceInfo.log("Skipping (|Z|*|C|*|T| == 0)");
                continue;
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
            dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), annotations, JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        }
    }

    private int wrapNumber(int x, int w) {
        while (x < 0) {
            x += w;
        }
        return x % w;
    }

    private void extractZ(ExpressionVariables parameters, ImageSliceIndices indices) {
        Object result = expressionZ.evaluate(parameters);
        if (result instanceof Number)
            indices.getZ().add(((Number) result).intValue());
        else {
            for (Object item : (Collection<?>) result) {
                indices.getZ().add(((Number) item).intValue());
            }
        }
    }

    private void extractC(ExpressionVariables parameters, ImageSliceIndices indices) {
        Object result = expressionC.evaluate(parameters);
        if (result instanceof Number)
            indices.getC().add(((Number) result).intValue());
        else {
            for (Object item : (Collection<?>) result) {
                indices.getC().add(((Number) item).intValue());
            }
        }
    }

    private void extractT(ExpressionVariables parameters, ImageSliceIndices indices) {
        Object result = expressionT.evaluate(parameters);
        if (result instanceof Number)
            indices.getT().add(((Number) result).intValue());
        else {
            for (Object item : (Collection<?>) result) {
                indices.getT().add(((Number) item).intValue());
            }
        }
    }

    @JIPipeDocumentation(name = "Generate Z slices", description = "Expression that is executed for each Z/C/T slice and generates an array of slice indices (or a single slice index) that " +
            "determine which Z indices are exported. All indices begin with zero. Indices outside the available range are automatically wrapped. Return an empty array to skip a slice.")
    @JIPipeParameter("expression-z")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public DefaultExpressionParameter getExpressionZ() {
        return expressionZ;
    }

    @JIPipeParameter("expression-z")
    public void setExpressionZ(DefaultExpressionParameter expressionZ) {
        this.expressionZ = expressionZ;
    }

    @JIPipeDocumentation(name = "Generate channel slices", description = "Expression that is executed for each Z/C/T slice and generates an array of slice indices (or a single slice index) that " +
            "determine which C indices are exported. All indices begin with zero. Indices outside the available range are automatically wrapped. Return an empty array to skip a slice.")
    @JIPipeParameter("expression-c")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public DefaultExpressionParameter getExpressionC() {
        return expressionC;
    }

    @JIPipeParameter("expression-c")
    public void setExpressionC(DefaultExpressionParameter expressionC) {
        this.expressionC = expressionC;
    }

    @JIPipeDocumentation(name = "Generate frame slices", description = "Expression that is executed for each Z/C/T slice and generates an array of slice indices (or a single slice index) that " +
            "determine which T indices are exported. All indices begin with zero. Indices outside the available range are automatically wrapped. Return an empty array to skip a slice.")
    @JIPipeParameter("expression-t")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    public DefaultExpressionParameter getExpressionT() {
        return expressionT;
    }

    @JIPipeParameter("expression-t")
    public void setExpressionT(DefaultExpressionParameter expressionT) {
        this.expressionT = expressionT;
    }

    @JIPipeDocumentation(name = "Iterate incoming Z slices", description = "If enabled, all expressions are executed per Z slice.")
    @JIPipeParameter("iterate-per-z")
    public boolean isIteratePerZ() {
        return iteratePerZ;
    }

    @JIPipeParameter("iterate-per-z")
    public void setIteratePerZ(boolean iteratePerZ) {
        this.iteratePerZ = iteratePerZ;
    }

    @JIPipeDocumentation(name = "Iterate incoming channel slices", description = "If enabled, all expressions are executed per C slice.")
    @JIPipeParameter("iterate-per-c")
    public boolean isIteratePerC() {
        return iteratePerC;
    }

    @JIPipeParameter("iterate-per-c")
    public void setIteratePerC(boolean iteratePerC) {
        this.iteratePerC = iteratePerC;
    }

    @JIPipeDocumentation(name = "Iterate incoming frame slices", description = "If enabled, all expressions are executed per T slice.")
    @JIPipeParameter("iterate-per-t")
    public boolean isIteratePerT() {
        return iteratePerT;
    }

    @JIPipeParameter("iterate-per-t")
    public void setIteratePerT(boolean iteratePerT) {
        this.iteratePerT = iteratePerT;
    }

    @JIPipeDocumentation(name = "Remove duplicates", description = "If enabled, duplicate stack index definitions are removed.")
    @JIPipeParameter("remove-duplicates")
    public boolean isRemoveDuplicates() {
        return removeDuplicates;
    }

    @JIPipeParameter("remove-duplicates")
    public void setRemoveDuplicates(boolean removeDuplicates) {
        this.removeDuplicates = removeDuplicates;
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

    public static class VariableSource implements ExpressionParameterVariableSource {
        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> result = new HashSet<>();
            result.add(new ExpressionParameterVariable("Image width", "The width of the image", "width"));
            result.add(new ExpressionParameterVariable("Image height", "The height of the image", "height"));
            result.add(new ExpressionParameterVariable("Image Z slices", "The number of Z slices in the image", "size_z"));
            result.add(new ExpressionParameterVariable("Image channels", "The number of channel (C) slices in the image", "size_c"));
            result.add(new ExpressionParameterVariable("Image frames", "The number of frames (T) in the image", "size_t"));
            result.add(new ExpressionParameterVariable("Z coordinate", "The Z coordinate within the image (first index is zero)", "z"));
            result.add(new ExpressionParameterVariable("Channel coordinate", "The channel (C) coordinate within the image (first index is zero)", "c"));
            result.add(new ExpressionParameterVariable("Frame coordinate", "The frame (T) coordinate within the image (first index is zero)", "t"));
            return result;
        }
    }
}
