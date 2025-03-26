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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.convert;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejdatatypes.colorspace.ColorSpace;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Algorithm that generates {@link ResultsTableData} as histogram
 */
@SetJIPipeDocumentation(name = "Get pixels as table (multiple images)", description = "Extracts the pixel values of multiple images and puts them into a table. " +
        "The table always includes columns <code>x</code>, <code>y</code>, <code>z</code>, <code>c</code>, and <code>t</code>. For greyscale images, the value is stored into a column named according to the input. " +
        "For color images, column names depend on the color space and are named <code>SLOT_NAME.COLOR_CHANNEL</code>.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Output", create = true)
public class MultipleImagesToTableAlgorithm extends JIPipeIteratingAlgorithm {

    private boolean applyPerSlice = false;
    private String sliceAnnotation = "Image index";

    /**
     * Creates a new instance
     *
     * @param info the algorithm info
     */
    public MultipleImagesToTableAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder()
                .restrictInputTo(ImagePlusData.class)
                .addOutputSlot("Output", "", ResultsTableData.class)
                .sealOutput()
                .build());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public MultipleImagesToTableAlgorithm(MultipleImagesToTableAlgorithm other) {
        super(other);
        this.applyPerSlice = other.applyPerSlice;
        this.sliceAnnotation = other.sliceAnnotation;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Map<String, ImagePlusData> inputImages = new HashMap<>();
        for (JIPipeInputDataSlot inputSlot : getDataInputSlots()) {
            inputImages.put(inputSlot.getName(), iterationStep.getInputData(inputSlot, ImagePlusData.class, progressInfo));
        }

        if (inputImages.isEmpty()) {
            progressInfo.log("Nothing to do.");
            return;
        }

        // Ensure same size
        if (!ImageJUtils.imagesHaveSameSize(inputImages.values().stream().map(ImagePlusData::getImage).collect(Collectors.toList()))) {
            throw new IllegalArgumentException("Images do not have the same size.");
        }

        ImagePlus referenceImage = inputImages.values().iterator().next().getImage();

        if (applyPerSlice) {
            ImageJIterationUtils.forEachIndexedZCTSlice(referenceImage, (referenceImp, index) -> {
                ResultsTableData resultsTable = new ResultsTableData();
                prepareResultsTable(resultsTable);
                resultsTable.addRows(referenceImp.getWidth() * referenceImp.getHeight());

                List<ImageProcessor> processors = new ArrayList<>();
                List<String> slotNames = new ArrayList<>();
                List<ColorSpace> colorSpaces = new ArrayList<>();

                extractProcessors(index, inputImages, processors, slotNames, colorSpaces);

                writePixelsToTable(resultsTable, processors, slotNames, colorSpaces, index, 0);
                if (!StringUtils.isNullOrEmpty(sliceAnnotation)) {
                    iterationStep.addOutputData(getFirstOutputSlot(), resultsTable,
                            Collections.singletonList(new JIPipeTextAnnotation(sliceAnnotation, "slice=" + index)), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
                } else {
                    iterationStep.addOutputData(getFirstOutputSlot(), resultsTable, progressInfo);
                }
            }, progressInfo);
        } else {
            ResultsTableData resultsTable = new ResultsTableData();
            prepareResultsTable(resultsTable);
            resultsTable.addRows(referenceImage.getWidth() * referenceImage.getHeight() * referenceImage.getNFrames() * referenceImage.getNChannels() * referenceImage.getNSlices());

            int[] counter = new int[1];
            ImageJIterationUtils.forEachIndexedZCTSlice(referenceImage, (referenceImp, index) -> {

                List<ImageProcessor> processors = new ArrayList<>();
                List<String> slotNames = new ArrayList<>();
                List<ColorSpace> colorSpaces = new ArrayList<>();

                extractProcessors(index, inputImages, processors, slotNames, colorSpaces);

                writePixelsToTable(resultsTable, processors, slotNames, colorSpaces, index, counter[0]);
                counter[0] += referenceImp.getWidth() * referenceImp.getHeight();

            }, progressInfo);

            iterationStep.addOutputData(getFirstOutputSlot(), resultsTable, progressInfo);
        }
    }

    private void extractProcessors(ImageSliceIndex index, Map<String, ImagePlusData> inputImages, List<ImageProcessor> processors, List<String> slotNames, List<ColorSpace> colorSpaces) {
        for (JIPipeInputDataSlot inputSlot : getInputSlots()) {
            ImagePlusData img = inputImages.get(inputSlot.getName());
            ImageProcessor imp = ImageJUtils.getSliceZero(img.getImage(), index);
            processors.add(imp);
            slotNames.add(inputSlot.getName());
            colorSpaces.add(img.getColorSpace());
        }
    }

    private void writePixelsToTable(ResultsTableData target, List<ImageProcessor> processors, List<String> slotNames, List<ColorSpace> colorSpaces, ImageSliceIndex sliceIndex, int startIndex) {

        // Write standard items
        ImageProcessor referenceIp = processors.get(0);
        final int nPixels = referenceIp.getWidth() * referenceIp.getHeight();
        for (int i = 0; i < nPixels; i++) {
            final int row = i + startIndex;
            final int x = i % referenceIp.getWidth();
            final int y = i / referenceIp.getWidth();
            final int z = sliceIndex.getZ();
            final int c = sliceIndex.getC();
            final int t = sliceIndex.getT();

            // For performance reasons, use predefined order
            target.setValueAt(x, row, 0);
            target.setValueAt(y, row, 1);
            target.setValueAt(z, row, 2);
            target.setValueAt(c, row, 3);
            target.setValueAt(t, row, 4);
        }

        // Write image items
        for (int ipIndex = 0; ipIndex < processors.size(); ipIndex++) {
            ImageProcessor imp = processors.get(ipIndex);
            final boolean isGreyscale = imp.isGrayscale();
            ColorSpace colorSpace = colorSpaces.get(ipIndex);
            final int[] channelBuffer = imp.isGrayscale() ? new int[0] : new int[colorSpace.getNChannels()];
            final String[] channelNames = imp.isGrayscale() ? new String[0] : new String[colorSpace.getNChannels()];
            if (!isGreyscale) {
                for (int i = 0; i < colorSpace.getNChannels(); i++) {
                    channelNames[i] = colorSpace.getChannelShortName(i);
                }
            }
            final String ipName = slotNames.get(ipIndex);

            for (int i = 0; i < nPixels; i++) {
                final int row = i + startIndex;

                if (isGreyscale) {
                    target.setValueAt(imp.getf(i), row, ipName);
                } else {
                    colorSpace.decomposePixel(imp.get(i), channelBuffer);
                    for (int channel = 0; channel < channelBuffer.length; channel++) {
                        target.setValueAt(channelBuffer[channel], row, ipName + "." + channelNames[channel]);
                    }
                }
            }
        }

    }

    private void prepareResultsTable(ResultsTableData resultsTable) {
        resultsTable.addNumericColumn("x");
        resultsTable.addNumericColumn("y");
        resultsTable.addNumericColumn("z");
        resultsTable.addNumericColumn("c");
        resultsTable.addNumericColumn("t");
    }

    @SetJIPipeDocumentation(name = "Apply per slice", description = "If higher dimensional data is provided, generate a table for each slice. If disabled, " +
            "a table is generated for the whole image.")
    @JIPipeParameter("apply-per-slice")
    public boolean isApplyPerSlice() {
        return applyPerSlice;
    }

    @JIPipeParameter("apply-per-slice")
    public void setApplyPerSlice(boolean applyPerSlice) {
        this.applyPerSlice = applyPerSlice;
    }

    @SetJIPipeDocumentation(name = "Apply per slice annotation", description = "Optional annotation type that generated for each slice output. " +
            "It contains the string 'slice=[Number]'.")
    @JIPipeParameter("slice-annotation")
    public String getSliceAnnotation() {
        return sliceAnnotation;
    }

    @JIPipeParameter("slice-annotation")
    public void setSliceAnnotation(String sliceAnnotation) {
        this.sliceAnnotation = sliceAnnotation;
    }
}
