package org.hkijena.jipipe.extensions.imagej2.io.data;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.DefaultDataset;
import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.IterableInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.hkijena.jipipe.JIPipe;
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
 * Handling of {@link Interval}, assumed to be {@link ImgPlus}
 * It is assumed that these refer to images (conversion to {@link org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData})
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
            nodeInfo.addInputSlotForModuleItem(moduleItem, ImageJ2DatasetData.class);
        }
        if(moduleItem.isOutput()) {
            nodeInfo.addOutputSlotForModuleItem(moduleItem, ImageJ2DatasetData.class);
        }
    }

    @Override
    public void install(ImageJ2ModuleNode node, ModuleItem<?> moduleItem) {

    }

    @Override
    public boolean transferFromJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        String slotName = node.getModuleNodeInfo().getOutputSlotModuleItems().inverse().get(moduleItem);
        Dataset dataset = node.getOutputSlot(slotName).getData(0, ImageJ2DatasetData.class, progressInfo).getDataset();
        moduleItem.setValue(module, dataset.getImgPlus());
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2ModuleNode node, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        ImgPlus imgPlus = (ImgPlus) moduleItem.getValue(module);
        String slotName = node.getModuleNodeInfo().getInputSlotModuleItems().inverse().get(moduleItem);
        JIPipeDataSlot slot = node.getInputSlot(slotName);
        slot.clearData();
        slot.addData(new ImageJ2DatasetData(new DefaultDataset(JIPipe.getInstance().getContext(), imgPlus)), progressInfo);
        return true;
    }
}
