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

package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.dimensions;

import com.google.common.collect.Iterables;
import ij.ImagePlus;
import ij.ImageStack;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeMergingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeMultiIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.extensions.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;

import java.util.*;
import java.util.stream.Collectors;

@SetJIPipeDocumentation(name = "Merge 2D slices into hyperstack", description = "Merges all incoming 2D slices into a hyperstack. The slice positions within the hyperstack can be controlled via expressions. " +
        "The node can handle missing and negative slice positions due to a remapping procedure. Please ensure that there are no duplicate locations.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Dimensions")
@AddJIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Slices", description = "The 2D slices. Each one should be annotated by the Z, C, and T position", create = true)
@AddJIPipeOutputSlot(value = ImagePlusData.class, slotName = "Hyperstack", description = "The generated hyperstack", create = true)
public class Merge2DToHyperstackAlgorithm extends JIPipeMergingAlgorithm {
    private JIPipeExpressionParameter sliceZLocation = new JIPipeExpressionParameter("Z");
    private JIPipeExpressionParameter sliceCLocation = new JIPipeExpressionParameter("C");
    private JIPipeExpressionParameter sliceTLocation = new JIPipeExpressionParameter("T");

    public Merge2DToHyperstackAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public Merge2DToHyperstackAlgorithm(Merge2DToHyperstackAlgorithm other) {
        super(other);
        this.sliceZLocation = new JIPipeExpressionParameter(other.sliceZLocation);
        this.sliceCLocation = new JIPipeExpressionParameter(other.sliceCLocation);
        this.sliceTLocation = new JIPipeExpressionParameter(other.sliceTLocation);
    }

    @Override
    protected void runIteration(JIPipeMultiIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {

        if (iterationStep.getInputRows(getFirstInputSlot()).isEmpty()) {
            progressInfo.log("No inputs. Skipping.");
            return;
        }

        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        variables.putAnnotations(iterationStep.getMergedTextAnnotations());
        Map<ImagePlusData, ImageSliceIndex> sliceMappings = new IdentityHashMap<>();

        progressInfo.log("Collecting target slice locations ...");
        int maxZ = Integer.MIN_VALUE;
        int maxC = Integer.MIN_VALUE;
        int maxT = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int minC = Integer.MAX_VALUE;
        int minT = Integer.MAX_VALUE;
        for (int inputRow : iterationStep.getInputRows(getFirstInputSlot())) {

            // use the original annotations
            for (JIPipeTextAnnotation textAnnotation : getFirstInputSlot().getTextAnnotations(inputRow)) {
                variables.set(textAnnotation.getName(), textAnnotation.getValue());
            }

            int z = sliceZLocation.evaluateToInteger(variables);
            int c = sliceCLocation.evaluateToInteger(variables);
            int t = sliceTLocation.evaluateToInteger(variables);

            maxZ = Math.max(maxZ, z);
            maxC = Math.max(maxC, c);
            maxT = Math.max(maxT, t);
            minZ = Math.min(minZ, z);
            minC = Math.min(minC, c);
            minT = Math.min(minT, t);

            sliceMappings.put(getFirstInputSlot().getData(inputRow, ImagePlus2DData.class, progressInfo), new ImageSliceIndex(c, z, t));
        }

        List<ImagePlus> inputImagesList = sliceMappings.keySet().stream().map(ImagePlusData::getImage).collect(Collectors.toList());

        if (!ImageJUtils.imagesHaveSameSize(inputImagesList)) {
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "Input images do not have the same size!",
                    "All input images in the same batch should have the same width, height, number of slices, number of frames, and number of channels."));
        }

        int consensusBitDepth = ImageJUtils.getConsensusBitDepth(inputImagesList);

        progressInfo.log("Remapping ...");
        List<Integer> zRemapping = new ArrayList<>(Arrays.asList(new Integer[maxZ - minZ + 1]));
        List<Integer> cRemapping = new ArrayList<>(Arrays.asList(new Integer[maxC - minC + 1]));
        List<Integer> tRemapping = new ArrayList<>(Arrays.asList(new Integer[maxT - minT + 1]));

        for (Map.Entry<ImagePlusData, ImageSliceIndex> entry : sliceMappings.entrySet()) {
            int z = entry.getValue().getZ() - minZ;
            int c = entry.getValue().getC() - minC;
            int t = entry.getValue().getT() - minT;
            zRemapping.set(z, entry.getValue().getZ());
            cRemapping.set(c, entry.getValue().getC());
            tRemapping.set(t, entry.getValue().getT());
        }

        // Cleanup the empty locations within the map
        Iterables.removeIf(zRemapping, Objects::isNull);
        Iterables.removeIf(cRemapping, Objects::isNull);
        Iterables.removeIf(tRemapping, Objects::isNull);

        ImagePlusData referenceImage = sliceMappings.keySet().iterator().next();
        ImageStack outputImageStack = new ImageStack(referenceImage.getWidth(), referenceImage.getHeight(), zRemapping.size() * cRemapping.size() * tRemapping.size());
        for (Map.Entry<ImagePlusData, ImageSliceIndex> entry : sliceMappings.entrySet()) {
            int z = zRemapping.indexOf(entry.getValue().getZ());
            int c = cRemapping.indexOf(entry.getValue().getC());
            int t = tRemapping.indexOf(entry.getValue().getT());
            ImageSliceIndex targetIndex = new ImageSliceIndex(c, z, t);
            ImagePlus converted = ImageJUtils.convertToBitDepthIfNeeded(entry.getKey().getImage(), consensusBitDepth);
            outputImageStack.setProcessor(converted.getProcessor(), targetIndex.zeroSliceIndexToOneStackIndex(cRemapping.size(), zRemapping.size(), tRemapping.size()));
        }

        ImagePlus outputImage = new ImagePlus("Merged", outputImageStack);
        outputImage.setDimensions(cRemapping.size(), zRemapping.size(), tRemapping.size());
        outputImage.copyScale(referenceImage.getImage());

        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(outputImage), progressInfo);
    }

    @SetJIPipeDocumentation(name = "Assign 2D slice to Z location", description = "Expression that returns the expected Z location of the slice")
    @JIPipeParameter(value = "slice-z-location", important = true)
    @JIPipeExpressionParameterSettings(hint = "per 2D slice")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getSliceZLocation() {
        return sliceZLocation;
    }

    @JIPipeParameter("slice-z-location")
    public void setSliceZLocation(JIPipeExpressionParameter sliceZLocation) {
        this.sliceZLocation = sliceZLocation;
    }

    @SetJIPipeDocumentation(name = "Assign 2D slice C location", description = "Expression that returns the expected C (channel) location of the slice")
    @JIPipeParameter(value = "slice-c-location", important = true)
    @JIPipeExpressionParameterSettings(hint = "per 2D slice")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getSliceCLocation() {
        return sliceCLocation;
    }

    @JIPipeParameter("slice-c-location")
    public void setSliceCLocation(JIPipeExpressionParameter sliceCLocation) {
        this.sliceCLocation = sliceCLocation;
    }

    @SetJIPipeDocumentation(name = "Assign 2D slice to T location", description = "Expression that returns the expected T (frame) location of the slice")
    @JIPipeParameter(value = "slice-t-location", important = true)
    @JIPipeExpressionParameterSettings(hint = "per 2D slice")
    @JIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
    public JIPipeExpressionParameter getSliceTLocation() {
        return sliceTLocation;
    }

    @JIPipeParameter("slice-t-location")
    public void setSliceTLocation(JIPipeExpressionParameter sliceTLocation) {
        this.sliceTLocation = sliceTLocation;
    }
}
