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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.datasources;

import ij.ImagePlus;
import ij.plugin.LutLoader;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.LUT;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.ImageJNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.filesystem.dataypes.FileData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.d2.color.ImagePlus2DColorRGBData;

@SetJIPipeDocumentation(name = "Import LUT as RGB image", description = "Imports an ImageJ *.lut file")
@AddJIPipeInputSlot(value = FileData.class, name = "Input", description = "A *.lut file", create = true)
@AddJIPipeOutputSlot(value = ImagePlus2DColorRGBData.class, name = "Output", description = "Description of the LUT as RGB image", create = true)
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
