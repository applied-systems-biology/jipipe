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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.dimensions;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.ImagePlus2DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.ImagePlus3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.HyperstackDimension;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.hkijena.jipipe.api.nodes.JIPipeMergingAlgorithm.MERGING_ALGORITHM_DESCRIPTION;
import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.TO_3D_CONVERSION;

/**
 * Wrapper around {@link ImageProcessor}
 */
@JIPipeDocumentation(name = "Create 3D stack", description = "Merges 2D image planes into a 3D stack. Data annotations are used to put " +
        "images into groups. All images in a group are then merged into a 3D stack. The order of the stack slices is determined by the 'Slice index annotation' " +
        "that is ignored while defining the groups." + "\n\n" + MERGING_ALGORITHM_DESCRIPTION)
@JIPipeNode(menuPath = "Dimensions", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlus2DData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlus3DData.class, slotName = "Output")
public class CreateStackAlgorithm extends JIPipeMergingAlgorithm {

    private String counterAnnotation = "Slice";
    private HyperstackDimension outputDimension = HyperstackDimension.Depth;

    /**
     * Instantiates a new node type.
     *
     * @param info the info
     */
    public CreateStackAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlus2DData.class)
                .addOutputSlot("Output", ImagePlus3DData.class, "Input", TO_3D_CONVERSION)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
    }

    /**
     * Instantiates a new node type.
     *
     * @param other the other
     */
    public CreateStackAlgorithm(CreateStackAlgorithm other) {
        super(other);
        this.counterAnnotation = other.counterAnnotation;
        this.outputDimension = other.outputDimension;
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    public Set<String> getIgnoredAnnotationColumns() {
        if (StringUtils.isNullOrEmpty(counterAnnotation))
            return super.getIgnoredAnnotationColumns();
        else
            return Collections.singleton(counterAnnotation);
    }

    @Override
    protected void runIteration(JIPipeMergingDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Set<Integer> inputRows = dataBatch.getInputRows(getFirstInputSlot());
        List<Integer> sortedInputRows;
        if (!StringUtils.isNullOrEmpty(counterAnnotation)) {
            JIPipeTextAnnotation defaultCounter = new JIPipeTextAnnotation(counterAnnotation, "");
            sortedInputRows = inputRows.stream().sorted(Comparator.comparing(row ->
                    getFirstInputSlot().getAnnotationOr(row, counterAnnotation, defaultCounter))).collect(Collectors.toList());
            dataBatch.removeMergedAnnotation(counterAnnotation);
        } else {
            sortedInputRows = new ArrayList<>(inputRows);
        }

        if (sortedInputRows.isEmpty())
            return;

        ImagePlus firstSlice = getFirstInputSlot().getData(sortedInputRows.get(0), ImagePlus2DData.class, progressInfo).getImage();
        ImageStack stack = new ImageStack(firstSlice.getWidth(), firstSlice.getHeight(), sortedInputRows.size());
        for (int i = 0; i < sortedInputRows.size(); ++i) {
            ImagePlus copy = getFirstInputSlot().getData(sortedInputRows.get(i), ImagePlus2DData.class, progressInfo).getImage();
            stack.setProcessor(copy.getProcessor(), i + 1);
            stack.setSliceLabel("slice=" + i, i + 1);
        }
        ImagePlus resultImage = new ImagePlus("Stack", stack);
        if (outputDimension == HyperstackDimension.Channel) {
            resultImage.setDimensions(resultImage.getNSlices(), 1, 1);
        } else if (outputDimension == HyperstackDimension.Frame) {
            resultImage.setDimensions(1, 1, resultImage.getNSlices());
        }
        resultImage.copyScale(firstSlice);
        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlusData(resultImage), progressInfo);
    }

    @JIPipeDocumentation(name = "Slice index annotation",
            description = "Data annotation that is used as reference for ordering the slices. Annotation values are lexicographically sorted.")
    @JIPipeParameter("counter-annotation-type")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/data-types/annotation.png")
    public String getCounterAnnotation() {
        return counterAnnotation;
    }

    @JIPipeParameter("counter-annotation-type")
    public void setCounterAnnotation(String counterAnnotation) {
        this.counterAnnotation = counterAnnotation;
    }

    @JIPipeDocumentation(name = "Output dimension", description = "Determines in which dimension the stack grows. You can choose between Z (default), channel, and time")
    @JIPipeParameter("output-dimension")
    public HyperstackDimension getOutputDimension() {
        return outputDimension;
    }

    @JIPipeParameter("output-dimension")
    public void setOutputDimension(HyperstackDimension outputDimension) {
        this.outputDimension = outputDimension;
    }
}
