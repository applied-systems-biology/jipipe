package org.hkijena.jipipe.extensions.imagej2;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.module.ModuleItem;

import java.util.Map;

public class ImageJ2ModuleNode extends JIPipeIteratingAlgorithm {
    public ImageJ2ModuleNode(JIPipeNodeInfo info) {
        super(info);
        ImageJ2ModuleNodeInfo moduleNodeInfo = (ImageJ2ModuleNodeInfo) info;
        for (Map.Entry<ModuleItem<?>, ImageJ2ModuleIO> entry : moduleNodeInfo.getInputModuleIO().entrySet()) {
            entry.getValue().install(this, entry.getKey());
        }
        for (Map.Entry<ModuleItem<?>, ImageJ2ModuleIO> entry : moduleNodeInfo.getOutputModuleIO().entrySet()) {
            entry.getValue().install(this, entry.getKey());
        }
    }

    public ImageJ2ModuleNode(ImageJ2ModuleNode other) {
        super(other);
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {

    }
}
