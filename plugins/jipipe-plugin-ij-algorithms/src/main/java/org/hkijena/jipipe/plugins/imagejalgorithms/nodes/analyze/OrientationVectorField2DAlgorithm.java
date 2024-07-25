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

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.analyze;

import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.ResultsTable;
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
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.plugins.tables.datatypes.ResultsTableData;
import orientation.*;
import orientation.imageware.Builder;
import orientation.imageware.ImageWare;

import java.util.HashMap;
import java.util.Map;

@SetJIPipeDocumentation(name = "Orientation vector field 2D (OrientationJ)", description = "Applies the OrientationJ vector field calculation algorithm")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Analyze")
@AddJIPipeInputSlot(value = ImagePlusGreyscaleData.class, name = "Input", create = true)
@AddJIPipeOutputSlot(value = ROI2DListData.class, name = "Overlay", create = true)
@AddJIPipeOutputSlot(value = ResultsTableData.class, name = "Results", create = true)
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Energy")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Orientation")
@AddJIPipeOutputSlot(value = ImagePlusGreyscaleData.class, name = "Coherency")
public class OrientationVectorField2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    public static final JIPipeDataSlotInfo OUTPUT_ENERGY = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Energy", "");
    public static final JIPipeDataSlotInfo OUTPUT_ORIENTATION = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Orientation", "");
    public static final JIPipeDataSlotInfo OUTPUT_COHERENCE = new JIPipeDataSlotInfo(ImagePlusGreyscaleData.class, JIPipeSlotType.Output, "Coherency", "");

    private final OrientationJStructureTensorParameters structureTensorParameters;
    private final OutputParameters outputParameters;
    private boolean radians = true;

    public OrientationVectorField2DAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.structureTensorParameters = new OrientationJStructureTensorParameters();
        this.outputParameters = new OutputParameters();
        registerSubParameters(structureTensorParameters, outputParameters);
        updateOutputSlots();
    }

    public OrientationVectorField2DAlgorithm(OrientationVectorField2DAlgorithm other) {
        super(other);
        this.structureTensorParameters = new OrientationJStructureTensorParameters(other.structureTensorParameters);
        this.outputParameters = new OutputParameters(other.outputParameters);
        registerSubParameters(structureTensorParameters, outputParameters);
        updateOutputSlots();
        this.radians = other.radians;
    }

    private void updateOutputSlots() {
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
        ROI2DListData overlay = new ROI2DListData();
        ResultsTableData results = new ResultsTableData();

        ImageJUtils.forEachIndexedZCTSliceWithProgress(inputImage, (ip, index, sliceProgress) -> {
            OrientationJLogWrapper log = new OrientationJLogWrapper(sliceProgress);
            OrientationParameters params = new OrientationParameters(OrientationService.VECTORFIELD);

            // Pass structure tensor settings
            params.sigmaST = structureTensorParameters.getLocalWindowSigma();
            params.gradient = structureTensorParameters.getGradient().getNativeValue();

            // Enable all outputs
            params.showVectorOverlay = true;
            params.showVectorTable = true;
            params.view[OrientationParameters.TENSOR_ENERGY] = outputParameters.outputEnergy;
            params.view[OrientationParameters.TENSOR_ORIENTATION] = outputParameters.outputOrientation;
            params.view[OrientationParameters.TENSOR_COHERENCY] = outputParameters.outputCoherency;

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

            extractVectorField(groupImage, params, overlay, results, index);
            if (outputParameters.outputEnergy) {
                energySlices.put(index, groupImage.energy.buildImageStack().getProcessor(1));
            }
            if (outputParameters.outputCoherency) {
                coherencyIndexSlices.put(index, groupImage.coherency.buildImageStack().getProcessor(1));
            }
            if (outputParameters.outputOrientation) {
                orientationSlices.put(index, groupImage.orientation.buildImageStack().getProcessor(1));
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
        iterationStep.addOutputData("Results", results, progressInfo);
        iterationStep.addOutputData("Overlay", overlay, progressInfo);
    }

    public static void extractVectorField(GroupImage gim, OrientationParameters params, ROI2DListData overlay, ResultsTableData table, ImageSliceIndex sliceIndex) {
        if (gim == null) {
            return;
        }

        int size = params.vectorGrid;
        int type = params.vectorType;
        double scale = params.vectorScale;

        int nt = gim.energy.getSizeZ();
        Clusters[] clusters = new Clusters[nt];
        int xstart = (gim.nx - (gim.nx / size) * size) / 2;
        int ystart = (gim.ny - (gim.ny / size) * size) / 2;
        double max = gim.energy.getMaximum();
        if (max <= 0)
            return;

        int size2 = size * size;
        for (int t = 0; t < nt; t++) {
            clusters[t] = new Clusters();
            for (int y = ystart; y < gim.ny; y += size)
                for (int x = xstart; x < gim.nx; x += size) {
                    double dx = 0.0;
                    double dy = 0.0;
                    double coherencies = 0.0;
                    double energies = 0.0;
                    for (int k = 0; k < size; k++)
                        for (int l = 0; l < size; l++) {
                            double angle = gim.orientation.getPixel(x + k, y + l, t);
                            double coh = gim.coherency.getPixel(x + k, y + l, t);
                            dx += Math.cos(angle);
                            dy += Math.sin(angle);
                            coherencies += coh;
                            energies += gim.energy.getPixel(x + k, y + l, t);
                        }
                    dx /= size2;
                    dy /= size2;
                    coherencies /= size2;
                    energies /= size2;
                    if (energies > 0)
                        if (coherencies > 0)
                            clusters[t].add(new Cluster(x, y, size, size, dx, dy, coherencies, (energies / max)));
                }
        }

        if (params.showVectorTable) {
            for (int t = 0; t < nt; t++)
                for (Cluster c : clusters[t]) {
                    double a = Math.toDegrees(Math.atan2(c.dy, c.dx));
                    if (a < -90)
                        a += 180;
                    if (a > 90)
                        a -= 180;
                    table.addAndModifyRow()
                            .set("X", c.x + size / 2)
                            .set("Y", c.y + size / 2)
                            .set("C", sliceIndex.getC())
                            .set("Z", sliceIndex.getZ())
                            .set("T", sliceIndex.getT())
                            .set("DX", -c.dx)
                            .set("DY", c.dy)
                            .set("Orientation", a)
                            .set("Coherency", c.coherency)
                            .set("Energy", c.energy)
                            .build();
                }
        }

        if (params.showVectorOverlay) {
            double r = scale / 100.0 * size * 0.5;
            for (int t = 0; t < nt; t++)
                for (Cluster c : clusters[t]) {
                    double a = r;
                    if (type == 1)
                        a = r * c.energy;
                    else if (type == 2)
                        a = r * c.coherency;
                    else if (type == 3)
                        a = r * c.energy * c.coherency;

                    int x1 = (int) Math.round(c.x + size / 2 + a * c.dx);
                    int y1 = (int) Math.round(c.y + size / 2 - a * c.dy);
                    int x2 = (int) Math.round(c.x + size / 2 - a * c.dx);
                    int y2 = (int) Math.round(c.y + size / 2 + a * c.dy);
                    Roi roi = new Line(x1, y1, x2, y2);
                    roi.setPosition(t + 1);
                    //Roi.setColor(new Color(200, 0, (int)(200*Math.random()), 100));
                    overlay.add(roi);
                }
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
        private boolean outputCoherency = true;
        private boolean outputEnergy = true;
        private boolean outputOrientation = true;

        public OutputParameters() {
        }

        public OutputParameters(OutputParameters other) {
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
