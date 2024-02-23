package org.hkijena.jipipe.extensions.imp.nodes;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageData;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

@SetJIPipeDocumentation(name = "Import IMP image", description = "Imports an image via the Image Manipulation and Processing toolkit.")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeInputSlot(value = FileData.class, slotName = "File", create = true, description = "The file to be imported")
@AddJIPipeOutputSlot(value = ImpImageData.class, slotName = "Image", create = true, description = "The image")
public class ImportImpImageAlgorithm extends JIPipeSimpleIteratingAlgorithm {
    public ImportImpImageAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportImpImageAlgorithm(ImportImpImageAlgorithm other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        Path file = iterationStep.getInputData(getFirstInputSlot(), FileData.class, progressInfo).toPath();
        try {
            BufferedImage image = ImageIO.read(file.toFile());
            iterationStep.addOutputData(getFirstOutputSlot(), new ImpImageData(image), progressInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
