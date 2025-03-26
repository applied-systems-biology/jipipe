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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.detect;

import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.OrientationJLogWrapper;
import org.hkijena.jipipe.plugins.imagejalgorithms.utils.OrientationJStructureTensorParameters;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ROI2DListData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJIterationUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.dimensions.ImageSliceIndex;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import orientation.*;
import orientation.imageware.Builder;
import orientation.imageware.ImageWare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Detect corners 2D (Harris)", description = "OrientationJ implementation of the the Harris corner detection.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Detect")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Overlay", create = true, description = "Detected corners as ROI")
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Results", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Energy")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Orientation")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Coherency")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Harris-index")
public class CornerHarris2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public static final JIPipeDataSlotInfo OUTPUT_ENERGY = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Energy", "");
    public static final JIPipeDataSlotInfo OUTPUT_ORIENTATION = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Orientation", "");
    public static final JIPipeDataSlotInfo OUTPUT_COHERENCY = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Coherency", "");
    public static final JIPipeDataSlotInfo OUTPUT_HARRIS_INDEX = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Harris-index", "");

    private final OrientationJStructureTensorParameters structureTensorParameters;
    private final OutputParameters outputParameters;
    private double harrisK = 0.05;
    private int harrisL = 2;
    private double minLevel = 10;
    private boolean radians = true;

    public CornerHarris2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.structureTensorParameters = new OrientationJStructureTensorParameters();
        this.outputParameters = new OutputParameters();
        registerSubParameters(structureTensorParameters, outputParameters);
        updateOutputSlots();
    }

    public CornerHarris2DAlgorithm(CornerHarris2DAlgorithm other) {
        super(other);
        this.structureTensorParameters = new OrientationJStructureTensorParameters(other.structureTensorParameters);
        this.outputParameters = new OutputParameters(other.outputParameters);
        registerSubParameters(structureTensorParameters, outputParameters);
        updateOutputSlots();

        this.harrisK = other.harrisK;
        this.harrisL = other.harrisL;
        this.minLevel = other.minLevel;
        this.radians = other.radians;
    }

    public static void extractHarris(GroupImage gim, OrientationParameters params, ImageSliceIndex sliceIndex, ResultsTableData table, ROI2DListData overlay) {
        if (gim == null) {
            return;
        }
        int L = params.harrisL;
        double min = Math.min(1, Math.max(0, params.harrisMin * 0.01));
        if (L <= 0) {
            L = 0;
        }

        ArrayList<Corner> corners = new ArrayList<Corner>();
        double v;
        for (int t = 0; t < gim.nt; t++) {
            for (int y = 1; y < gim.ny - 1; y++)
                for (int x = 1; x < gim.nx - 1; x++) {
                    v = gim.harris.getPixel(x, y, t);
                    //if ( (v - min) * r > min) {
                    if (gim.harris.getPixel(x - 1, y, t) < v) {
                        if (gim.harris.getPixel(x + 1, y, t) < v) {
                            if (gim.harris.getPixel(x, y - 1, t) < v) {
                                if (gim.harris.getPixel(x, y + 1, t) < v) {
                                    corners.add(new Corner(x, y, t, v));
                                }
                            }
                        }
                    }

                    //}
                }
        }
        Collections.sort(corners);

        if (params.showHarrisTable) {
            for (int i = 0; i < corners.size() * min; i++) {
                Corner pt = corners.get(i);
                table.addAndModifyRow()
                        .set("X", pt.x)
                        .set("Y", pt.y)
                        .set("C", sliceIndex.getC())
                        .set("Z", sliceIndex.getZ())
                        .set("T", sliceIndex.getT())
                        .set("Harris index", pt.getHarrisIndex())
                        .build();
            }
        }

        if (params.showHarrisOverlay) {
            for (int i = 0; i < corners.size() * min; i++) {
                Corner pt = corners.get(i);
                Roi roi = new OvalRoi(pt.x - L / 2, pt.y - L / 2, L, L);
                roi.setPosition(sliceIndex.getC() + 1, sliceIndex.getZ() + 1, sliceIndex.getT() + 1);
                overlay.add(roi);
            }
        }
    }

    private void updateOutputSlots() {
        toggleSlot(OUTPUT_ORIENTATION, outputParameters.outputOrientation);
        toggleSlot(OUTPUT_ENERGY, outputParameters.outputEnergy);
        toggleSlot(OUTPUT_COHERENCY, outputParameters.outputCoherency);
        toggleSlot(OUTPUT_HARRIS_INDEX, outputParameters.outputHarrisIndex);
    }

    @Override
    public void onParameterChanged(ParameterChangedEvent event) {
        super.onParameterChanged(event);

        if (event.getSource() == outputParameters) {
            // update slots based on parameters
            updateOutputSlots();
        }
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus inputImage = iterationStep.getInputData(getFirstInputSlot(), ImagePlusGreyscaleData.class, progressInfo).getImage();

        Map<ImageSliceIndex, ImageProcessor> harrisIndexSlices = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> orientationSlices = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> energySlices = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> coherencyIndexSlices = new HashMap<>();
        ROI2DListData overlay = new ROI2DListData();
        ResultsTableData results = new ResultsTableData();

        ImageJIterationUtils.forEachIndexedZCTSliceWithProgress(inputImage, (ip, index, sliceProgress) -> {
            OrientationJLogWrapper log = new OrientationJLogWrapper(sliceProgress);
            OrientationParameters params = new OrientationParameters(OrientationService.HARRIS);

            // Pass structure tensor settings
            params.sigmaST = structureTensorParameters.getLocalWindowSigma();
            params.gradient = structureTensorParameters.getGradient().getNativeValue();

            // Pass other parameters
            params.harrisK = harrisK;
            params.harrisL = harrisL;
            params.harrisMin = minLevel;

            // Enable all outputs
            params.showHarrisTable = true;
            params.showHarrisOverlay = true;
            params.view[OrientationParameters.TENSOR_ENERGY] = outputParameters.outputEnergy;
            params.view[OrientationParameters.TENSOR_ORIENTATION] = outputParameters.outputOrientation;
            params.view[OrientationParameters.TENSOR_COHERENCY] = outputParameters.outputCoherency;
            params.view[OrientationParameters.HARRIS] = outputParameters.outputHarrisIndex;

            // Apply algorithm
            ImageWare source = Builder.create(new ImagePlus("slice", ip));
            GroupImage groupImage = new GroupImage(log, source, params);

            if (params.gradient == OrientationParameters.HESSIAN) {
                new Hessian(log, groupImage, params).run();
            } else {
                new Gradient(log, groupImage, params).run();
            }

            StructureTensor st = new StructureTensor(log, groupImage, params);
            st.run();

            // Extract results
            extractHarris(groupImage, params, index, results, overlay);

            if (outputParameters.outputEnergy) {
                energySlices.put(index, groupImage.energy.buildImageStack().getProcessor(1));
            }
            if (outputParameters.outputCoherency) {
                coherencyIndexSlices.put(index, groupImage.coherency.buildImageStack().getProcessor(1));
            }
            if (outputParameters.outputOrientation) {
                orientationSlices.put(index, groupImage.orientation.buildImageStack().getProcessor(1));
            }
            if (outputParameters.outputHarrisIndex) {
                harrisIndexSlices.put(index, groupImage.harris.buildImageStack().getProcessor(1));
            }
        }, progressInfo);

        // Collect all outputs
        iterationStep.addOutputData("Results", results, progressInfo);
        iterationStep.addOutputData("Overlay", overlay, progressInfo);
        if (!energySlices.isEmpty()) {
            ImagePlus img = ImageJUtils.mergeMappedSlices(energySlices);
            img.copyScale(inputImage);
            iterationStep.addOutputData("Energy", new ImagePlusGreyscaleData(img), progressInfo);
        }
        if (!coherencyIndexSlices.isEmpty()) {
            ImagePlus img = ImageJUtils.mergeMappedSlices(coherencyIndexSlices);
            img.copyScale(inputImage);
            iterationStep.addOutputData("Coherency", new ImagePlusGreyscaleData(img), progressInfo);
        }
        if (!orientationSlices.isEmpty()) {
            ImagePlus img = ImageJUtils.mergeMappedSlices(orientationSlices);
            img.copyScale(inputImage);
            iterationStep.addOutputData("Orientation", new ImagePlusGreyscaleData(img), progressInfo);
        }
        if (!harrisIndexSlices.isEmpty()) {
            ImagePlus img = ImageJUtils.mergeMappedSlices(harrisIndexSlices);
            img.copyScale(inputImage);
            iterationStep.addOutputData("Harris-index", new ImagePlusGreyscaleData(img), progressInfo);
        }

    }

    @SetJIPipeDocumentation(name = "Structure tensor")
    @JIPipeParameter("structure-tensor")
    public OrientationJStructureTensorParameters getStructureTensorParameters() {
        return structureTensorParameters;
    }

    @SetJIPipeDocumentation(name = "Outputs")
    @JIPipeParameter("outputs")
    public OutputParameters getOutputParameters() {
        return outputParameters;
    }

    @SetJIPipeDocumentation(name = "Harris K", description = "Harris detector free parameter in the equation")
    @JIPipeParameter("harris-k")
    public double getHarrisK() {
        return harrisK;
    }

    @JIPipeParameter("harris-k")
    public void setHarrisK(double harrisK) {
        this.harrisK = harrisK;
    }

    @SetJIPipeDocumentation(name = "Window size", description = "The size of neighbourhood considered for corner detection")
    @JIPipeParameter("harris-l")
    public int getHarrisL() {
        return harrisL;
    }

    @JIPipeParameter("harris-l")
    public void setHarrisL(int harrisL) {
        this.harrisL = harrisL;
    }

    @SetJIPipeDocumentation(name = "Minimum level")
    @JIPipeParameter("min-level")
    public double getMinLevel() {
        return minLevel;
    }

    @JIPipeParameter("min-level")
    public void setMinLevel(double minLevel) {
        this.minLevel = minLevel;
    }

    @SetJIPipeDocumentation(name = "Output angles in radians", description = "If enabled, angles are output in radians")
    @JIPipeParameter("radians")
    public boolean isRadians() {
        return radians;
    }

    @JIPipeParameter("radians")
    public void setRadians(boolean radians) {
        this.radians = radians;
    }

    public static class OutputParameters extends AbstractJIPipeParameterCollection {
        private boolean outputCoherency = false;
        private boolean outputEnergy = false;
        private boolean outputHarrisIndex = true;
        private boolean outputOrientation = false;

        public OutputParameters() {
        }

        public OutputParameters(OutputParameters other) {
            this.outputCoherency = other.outputCoherency;
            this.outputEnergy = other.outputEnergy;
            this.outputHarrisIndex = other.outputHarrisIndex;
            this.outputOrientation = other.outputOrientation;
        }

        @SetJIPipeDocumentation(name = "Output coherency")
        @JIPipeParameter("output-coherency")
        public boolean isOutputCoherency() {
            return outputCoherency;
        }

        @JIPipeParameter("output-coherency")
        public void setOutputCoherency(boolean outputCoherency) {
            this.outputCoherency = outputCoherency;
        }

        @SetJIPipeDocumentation(name = "Output energy")
        @JIPipeParameter("output-energy")
        public boolean isOutputEnergy() {
            return outputEnergy;
        }

        @JIPipeParameter("output-energy")
        public void setOutputEnergy(boolean outputEnergy) {
            this.outputEnergy = outputEnergy;
        }

        @SetJIPipeDocumentation(name = "Output Harris-index")
        @JIPipeParameter("output-harris-index")
        public boolean isOutputHarrisIndex() {
            return outputHarrisIndex;
        }

        @JIPipeParameter("output-harris-index")
        public void setOutputHarrisIndex(boolean outputHarrisIndex) {
            this.outputHarrisIndex = outputHarrisIndex;
        }

        @SetJIPipeDocumentation(name = "Output orientation")
        @JIPipeParameter("output-orientation")
        public boolean isOutputOrientation() {
            return outputOrientation;
        }

        @JIPipeParameter("output-orientation")
        public void setOutputOrientation(boolean outputOrientation) {
            this.outputOrientation = outputOrientation;
        }
    }
}
