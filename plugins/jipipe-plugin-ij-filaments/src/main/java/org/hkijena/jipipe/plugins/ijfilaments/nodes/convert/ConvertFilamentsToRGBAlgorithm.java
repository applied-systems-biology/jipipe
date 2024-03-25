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

package org.hkijena.jipipe.plugins.ijfilaments.nodes.convert;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.plugins.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.plugins.ijfilaments.util.FilamentsDrawer;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.BitDepth;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJUtils;

@SetJIPipeDocumentation(name = "Convert filaments to RGB", description = "Visualizes filaments by rendering them onto an RGB image")
@ConfigureJIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Convert")
@AddJIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", create = true)
@AddJIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", create = true, optional = true)
@AddJIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", create = true)
public class ConvertFilamentsToRGBAlgorithm extends JIPipeIteratingAlgorithm {

    private final FilamentsDrawer filamentsDrawer;
    private boolean drawOverReference = true;

    public ConvertFilamentsToRGBAlgorithm(JIPipeNodeInfo info) {
        super(info);
        this.filamentsDrawer = new FilamentsDrawer();
        registerSubParameter(filamentsDrawer);
    }

    public ConvertFilamentsToRGBAlgorithm(ConvertFilamentsToRGBAlgorithm other) {
        super(other);
        this.filamentsDrawer = new FilamentsDrawer(other.filamentsDrawer);
        this.drawOverReference = other.drawOverReference;
        registerSubParameter(filamentsDrawer);
    }

    @SetJIPipeDocumentation(name = "Filament drawing settings", description = "The following settings control how filaments are visualized")
    @JIPipeParameter("filaments-drawer")
    public FilamentsDrawer getFilamentsDrawer() {
        return filamentsDrawer;
    }

    @SetJIPipeDocumentation(name = "Draw over reference", description = "If enabled, draw over the reference image")
    @JIPipeParameter("draw-over-reference")
    public boolean isDrawOverReference() {
        return drawOverReference;
    }

    @JIPipeParameter("draw-over-reference")
    public void setDrawOverReference(boolean drawOverReference) {
        this.drawOverReference = drawOverReference;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Filaments3DData filaments3DData = iterationStep.getInputData("Input", Filaments3DData.class, progressInfo);
        ImagePlus reference = ImageJUtils.unwrap(iterationStep.getInputData("Reference", ImagePlusData.class, progressInfo));
        if (reference == null) {
            reference = filaments3DData.createBlankCanvas("Image", BitDepth.ColorRGB);
        } else if (!drawOverReference) {
            ImagePlus blank = IJ.createHyperStack("Image", reference.getWidth(), reference.getHeight(), reference.getNChannels(), reference.getNSlices(), reference.getNFrames(), 24);
            blank.copyScale(reference);
            reference = blank;
        } else if (reference.getType() == ImagePlus.COLOR_RGB) {
            reference = ImageJUtils.duplicate(reference);
        } else {
            reference = ImageJUtils.convertToColorRGBIfNeeded(reference);
        }
        ImageJUtils.forEachIndexedZCTSlice(reference, (ip, index) -> {
            filamentsDrawer.drawFilamentsOnProcessor(filaments3DData, (ColorProcessor) ip, index.getZ(), index.getC(), index.getT());
        }, progressInfo);
        iterationStep.addOutputData("Output", new ImagePlusColorRGBData(reference), progressInfo);
    }
}
