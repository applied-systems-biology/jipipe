package org.hkijena.jipipe.extensions.imagej2.io.data;

import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.NumericType;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

/**
 * Handling of {@link net.imagej.ImgPlus}
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class ImgPlusImageJ2ModuleIO extends AbstractService implements ImageJ2ModuleIO {
    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return ImgPlus.class;
    }

    @Override
    public void install(ImageJ2ModuleNodeInfo nodeInfo, ModuleItem<?> moduleItem) {

    }

    @Override
    public void install(ImageJ2ModuleNode node, ModuleItem<?> moduleItem) {

    }

    @Override
    public boolean transferFromJIPipe(ImageJ2ModuleNode node, ModuleItem<?> moduleItem, Module module) {
        return false;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2ModuleNode node, ModuleItem<?> moduleItem, Module module) {
        ImgPlus<NumericType> img = (ImgPlus<NumericType>) moduleItem.getValue(module);
        ImagePlus image = ImageJFunctions.wrap(img, moduleItem.getName());
        // TODO: ImageJ2ModuleNodeInfo must contain assignment
        return true;
    }
}
