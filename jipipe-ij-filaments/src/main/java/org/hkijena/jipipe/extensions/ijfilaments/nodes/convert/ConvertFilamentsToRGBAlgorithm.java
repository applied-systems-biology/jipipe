package org.hkijena.jipipe.extensions.ijfilaments.nodes.convert;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.FilamentsNodeTypeCategory;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.ijfilaments.util.FilamentsDrawer;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.color.ImagePlusColorRGBData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.BitDepth;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;

@JIPipeDocumentation(name = "Convert filaments to RGB", description = "Visualizes filaments by rendering them onto an RGB image")
@JIPipeNode(nodeTypeCategory = FilamentsNodeTypeCategory.class, menuPath = "Convert")
@JIPipeInputSlot(value = Filaments3DData.class, slotName = "Input", autoCreate = true)
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Reference", autoCreate = true, optional = true)
@JIPipeOutputSlot(value = ImagePlusColorRGBData.class, slotName = "Output", autoCreate = true)
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

    @JIPipeDocumentation(name = "Filament drawing settings", description = "The following settings control how filaments are visualized")
    @JIPipeParameter("filaments-drawer")
    public FilamentsDrawer getFilamentsDrawer() {
        return filamentsDrawer;
    }

    @JIPipeDocumentation(name = "Draw over reference", description = "If enabled, draw over the reference image")
    @JIPipeParameter("draw-over-reference")
    public boolean isDrawOverReference() {
        return drawOverReference;
    }

    @JIPipeParameter("draw-over-reference")
    public void setDrawOverReference(boolean drawOverReference) {
        this.drawOverReference = drawOverReference;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        Filaments3DData filaments3DData = dataBatch.getInputData("Input", Filaments3DData.class, progressInfo);
        ImagePlus reference = ImageJUtils.unwrap(dataBatch.getInputData("Reference", ImagePlusData.class, progressInfo));
        if(reference == null) {
            reference = filaments3DData.createBlankCanvas("Image", BitDepth.ColorRGB);
        }
        else if(!drawOverReference) {
            ImagePlus blank = IJ.createHyperStack("Image", reference.getWidth(), reference.getHeight(), reference.getNChannels(), reference.getNSlices(), reference.getNFrames(), 24);
            blank.copyScale(reference);
            reference = blank;
        }
        else if(reference.getType() == ImagePlus.COLOR_RGB) {
            reference = ImageJUtils.duplicate(reference);
        }
        else {
            reference = ImageJUtils.convertToColorRGBIfNeeded(reference);
        }
        ImageJUtils.forEachIndexedZCTSlice(reference, (ip, index) -> {
            filamentsDrawer.drawFilamentsOnProcessor(filaments3DData, (ColorProcessor) ip, index.getZ(), index.getC(), index.getT());
        }, progressInfo);
        dataBatch.addOutputData("Output", new ImagePlusColorRGBData(reference), progressInfo);
    }
}