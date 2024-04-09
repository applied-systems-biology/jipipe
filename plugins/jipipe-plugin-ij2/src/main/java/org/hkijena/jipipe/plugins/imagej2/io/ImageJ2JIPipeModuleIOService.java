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

package org.hkijena.jipipe.plugins.imagej2.io;

import net.imagej.ImageJService;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.PTService;

/**
 * Service to communicate between ImageJ and JIPipe
 */
public interface ImageJ2JIPipeModuleIOService extends PTService<ImageJ2ModuleIO>, ImageJService {
    /**
     * Finds the best matching IO handler for the module item
     *
     * @param moduleItem the module item
     * @param ioType     the IO type
     * @return the {@link ImageJ2ModuleIO} or null
     */
    ImageJ2ModuleIO findModuleIO(ModuleItem<?> moduleItem, JIPipeSlotType ioType);
}
