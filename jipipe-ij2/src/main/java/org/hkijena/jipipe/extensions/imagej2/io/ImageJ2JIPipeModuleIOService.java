package org.hkijena.jipipe.extensions.imagej2.io;

import net.imagej.ImageJService;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.PTService;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

public interface ImageJ2JIPipeModuleIOService extends PTService<ImageJ2ModuleIO>, ImageJService {
    /**
     * Finds the best matching IO handler for the module item
     * @param moduleItem the module item
     * @return the {@link ImageJ2ModuleIO} or null
     */
    ImageJ2ModuleIO findModuleIO(ModuleItem<?> moduleItem);
}
