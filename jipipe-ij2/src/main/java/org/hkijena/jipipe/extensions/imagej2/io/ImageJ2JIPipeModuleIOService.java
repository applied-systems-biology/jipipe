package org.hkijena.jipipe.extensions.imagej2.io;

import net.imagej.ImageJService;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.PTService;

public interface ImageJ2JIPipeModuleIOService extends PTService<ImageJ2ModuleIO>, ImageJService {
    /**
     * Finds the best matching IO handler for the module item
     *
     * @param moduleItem the module item
     * @param ioType
     * @return the {@link ImageJ2ModuleIO} or null
     */
    ImageJ2ModuleIO findModuleIO(ModuleItem<?> moduleItem, JIPipeSlotType ioType);
}
