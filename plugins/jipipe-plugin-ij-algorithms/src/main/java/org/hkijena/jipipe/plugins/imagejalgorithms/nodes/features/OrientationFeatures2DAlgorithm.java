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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.features;

import ij.ImagePlus;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import orientation.*;
import orientation.imageware.Builder;
import orientation.imageware.ImageWare;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Orientation features 2D (OrientationJ)", description = "Extracts various orientation-related features from the input image. " +
        "These are the same features as produced by the OrientationJ Analysis.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Features")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Energy")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Orientation")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Coherency")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Gradient-X")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Gradient-Y")
public class OrientationFeatures2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public static final JIPipeDataSlotInfo OUTPUT_GRADIENT_X = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Gradient-X", "");
    public static final JIPipeDataSlotInfo OUTPUT_GRADIENT_Y = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Gradient-Y", "");
    public static final JIPipeDataSlotInfo OUTPUT_ENERGY = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Energy", "");
    public static final JIPipeDataSlotInfo OUTPUT_ORIENTATION = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Orientation", "");
    public static final JIPipeDataSlotInfo OUTPUT_COHERENCE = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Coherency", "");

    private final OrientationJStructureTensorParameters structureTensorParameters;
    private final OutputParameters outputParameters;
    private boolean radians = true;

    public OrientationFeatures2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.structureTensorParameters = new OrientationJStructureTensorParameters();
        this.outputParameters = new OutputParameters();
        registerSubParameters(structureTensorParameters, outputParameters);
        updateOutputSlots();
    }

    public OrientationFeatures2DAlgorithm(OrientationFeatures2DAlgorithm other) {
        super(other);
        this.structureTensorParameters = new OrientationJStructureTensorParameters(other.structureTensorParameters);
        this.outputParameters = new OutputParameters(other.outputParameters);
        registerSubParameters(structureTensorParameters, outputParameters);
        updateOutputSlots();
        this.radians = other.radians;
    }

    private void updateOutputSlots() {
        toggleSlot(OUTPUT_GRADIENT_X, outputParameters.outputGradientX);
        toggleSlot(OUTPUT_GRADIENT_Y, outputParameters.outputGradientY);
        toggleSlot(OUTPUT_ORIENTATION, outputParameters.outputOrientation);
        toggleSlot(OUTPUT_ENERGY, outputParameters.outputEnergy);
        toggleSlot(OUTPUT_COHERENCE, outputParameters.outputCoherency);
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

        Map<ImageSliceIndex, ImageProcessor> orientationSlices = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> energySlices = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> coherencyIndexSlices = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> gradientXSlices = new HashMap<>();
        Map<ImageSliceIndex, ImageProcessor> gradientYSlices = new HashMap<>();

        ImageJUtils.forEachIndexedZCTSliceWithProgress(inputImage, (ip, index, sliceProgress) -> {
            OrientationJLogWrapper log = new OrientationJLogWrapper(sliceProgress);
            OrientationParameters params = new OrientationParameters(OrientationService.ANALYSIS);

            // Pass structure tensor settings
            params.sigmaST = structureTensorParameters.getLocalWindowSigma();
            params.gradient = structureTensorParameters.getGradient().getNativeValue();

            // Enable all outputs
            params.view[OrientationParameters.TENSOR_ENERGY] = outputParameters.outputEnergy;
            params.view[OrientationParameters.TENSOR_ORIENTATION] = outputParameters.outputOrientation;
            params.view[OrientationParameters.TENSOR_COHERENCY] = outputParameters.outputCoherency;
            params.view[OrientationParameters.GRADIENT_HORIZONTAL] = outputParameters.outputGradientX;
            params.view[OrientationParameters.GRADIENT_VERTICAL] = outputParameters.outputGradientY;

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

            if (outputParameters.outputEnergy) {
                energySlices.put(index, groupImage.energy.buildImageStack().getProcessor(1));
            }
            if (outputParameters.outputCoherency) {
                coherencyIndexSlices.put(index, groupImage.coherency.buildImageStack().getProcessor(1));
            }
            if (outputParameters.outputOrientation) {
                orientationSlices.put(index, groupImage.orientation.buildImageStack().getProcessor(1));
            }
            if (outputParameters.outputGradientX) {
                gradientXSlices.put(index, groupImage.gx.buildImageStack().getProcessor(1));
            }
            if (outputParameters.outputGradientY) {
                gradientYSlices.put(index, groupImage.gy.buildImageStack().getProcessor(1));
            }
        }, progressInfo);

        // Collect all outputs

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
        if (!gradientXSlices.isEmpty()) {
            ImagePlus img = ImageJUtils.mergeMappedSlices(gradientXSlices);
            img.copyScale(inputImage);
            iterationStep.addOutputData("Gradient-X", new ImagePlusGreyscaleData(img), progressInfo);
        }
        if (!gradientYSlices.isEmpty()) {
            ImagePlus img = ImageJUtils.mergeMappedSlices(gradientYSlices);
            img.copyScale(inputImage);
            iterationStep.addOutputData("Gradient-Y", new ImagePlusGreyscaleData(img), progressInfo);
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
        private boolean outputGradientX = true;
        private boolean outputGradientY = true;
        private boolean outputCoherency = true;
        private boolean outputEnergy = true;
        private boolean outputOrientation = true;

        public OutputParameters() {
        }

        public OutputParameters(OutputParameters other) {
            this.outputGradientX = other.outputGradientX;
            this.outputGradientY = other.outputGradientY;
            this.outputCoherency = other.outputCoherency;
            this.outputEnergy = other.outputEnergy;
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

        @SetJIPipeDocumentation(name = "Output Gradient X")
        @JIPipeParameter("output-gradient-x")
        public boolean isOutputGradientX() {
            return outputGradientX;
        }

        @JIPipeParameter("output-gradient-x")
        public void setOutputGradientX(boolean outputGradientX) {
            this.outputGradientX = outputGradientX;
        }

        @SetJIPipeDocumentation(name = "Output Gradient Y")
        @JIPipeParameter("output-gradient-y")
        public boolean isOutputGradientY() {
            return outputGradientY;
        }

        @JIPipeParameter("output-gradient-y")
        public void setOutputGradientY(boolean outputGradientY) {
            this.outputGradientY = outputGradientY;
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
