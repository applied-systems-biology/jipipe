package org.hkijena.jipipe.extensions.imagej2.io.data;

import ij.ImagePlus;
import net.imagej.Dataset;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

/**
 * Handling of {@link ij.ImagePlus}
 * It is assumed that these refer to images (conversion to {@link ImageJ2DatasetData})
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class ImagePlusImageJ2ModuleIO extends AbstractService implements ImageJ2ModuleIO {
    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return ImagePlus.class;
    }

    @Override
    public boolean handlesInput() {
        return true;
    }

    @Override
    public boolean handlesOutput() {
        return true;
    }

    @Override
    public void install(ImageJ2ModuleNodeInfo nodeInfo, ModuleItem<?> moduleItem) {
        if(moduleItem.isInput()) {
            nodeInfo.addInputSlotForModuleItem(moduleItem, ImagePlusData.class);
        }
        if(moduleItem.isOutput()) {
            nodeInfo.addOutputSlotForModuleItem(moduleItem, ImagePlusData.class);
        }
    }

    @Override
    public void install(ImageJ2ModuleNode node, ModuleItem<?> moduleItem) {

    }

    @Override
    public boolean transferFromJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        String slotName = node.getModuleNodeInfo().getOutputSlotModuleItems().inverse().get(moduleItem);
        ImagePlus imagePlus = node.getOutputSlot(slotName).getData(0, ImagePlusData.class, progressInfo).getImage();
        moduleItem.setValue(module, imagePlus);
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        ImagePlus imagePlus = (ImagePlus) moduleItem.getValue(module);
        String slotName = node.getModuleNodeInfo().getInputSlotModuleItems().inverse().get(moduleItem);
        JIPipeDataSlot slot = node.getInputSlot(slotName);
        slot.clearData();
        slot.addData(new ImagePlusData(imagePlus), progressInfo);
        return true;
    }
}
