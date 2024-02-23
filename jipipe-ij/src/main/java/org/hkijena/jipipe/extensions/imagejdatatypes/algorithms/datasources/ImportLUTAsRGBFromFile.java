package org.hkijena.jipipe.extensions.imagejdatatypes.algorithms.datasources;

import ij.ImagePlus;
import ij.plugin.LutLoader;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.LUT;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;

@SetJIPipeDocumentation(name = "Import LUT as RGB image", description = "Imports an ImageJ *.lut file")
@AddJIPipeInputSlot(value = FileData.class, slotName = "Input", description = "A *.lut file", create = true)
@AddJIPipeOutputSlot(value = ImagePlus2DColorRGBData.class, slotName = "Output", description = "Description of the LUT as RGB image", create = true)
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class)
@AddJIPipeNodeAlias(nodeTypeCategory = ImageJNodeTypeCategory.class, menuPath = "File", aliasName = "Open (LUT)")
public class ImportLUTAsRGBFromFile extends JIPipeSimpleIteratingAlgorithm {
    public ImportLUTAsRGBFromFile(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportLUTAsRGBFromFile(ImportLUTAsRGBFromFile other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        String lutFile = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo).getPath();
        LUT lut = LutLoader.openLut(lutFile);
        ByteProcessor processor = new ByteProcessor(256, 1);
        byte[] pixels = (byte[]) processor.getPixels();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = (byte) i;
        }
        processor.setLut(lut);
        ImagePlus img = new ImagePlus("LUT Render", new ColorProcessor(processor.getBufferedImage()));
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlus2DColorRGBData(img), progressInfo);
    }
}
