package org.hkijena.jipipe.extensions.imagej2.io.data;

import ij.ImagePlus;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

/**
 * Handling of {@link Interval}
 * It is assumed that these refer to images (conversion to {@link ImagePlus} by assuming they are {@link Img})
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class IntervalImageJ2ModuleIO extends AbstractService implements ImageJ2ModuleIO {
    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return Interval.class;
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
        Interval imgPlus = ImageJFunctions.wrap(imagePlus);
        moduleItem.setValue(module, imgPlus);
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        Img img = (Img) moduleItem.getValue(module);
        ImagePlus image = ImageJFunctions.wrap(img, moduleItem.getName());
        String slotName = node.getModuleNodeInfo().getInputSlotModuleItems().inverse().get(moduleItem);
        JIPipeDataSlot slot = node.getInputSlot(slotName);
        slot.clearData();
        slot.addData(new ImagePlusData(image), progressInfo);
        return true;
    }
}
