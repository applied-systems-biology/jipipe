package org.hkijena.jipipe.extensions.imagej2.io;

import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.service.Service;

/**
 * An object that is responsible for transferring inputs and outputs between JIPipe slots/parameters and ImageJ2 parameters
 */
public interface ImageJ2ModuleIO extends Service {
    /**
     * The class used to store data inside a module
     * @return the class
     */
    Class<?> getAcceptedModuleFieldClass();

    /**
     * Transfers data from JIPipe into the module
     * @param algorithm the JIPipe node
     * @param module the module
     * @return if successful
     */
    boolean transferFromJIPipe(JIPipeAlgorithm algorithm, ModuleItem<?> moduleItem, Module module);

    /**
     * Transfers data from JIPipe from the module
     * @param algorithm the JIPipe node
     * @param module the module
     * @return if successful
     */
    boolean transferToJIPipe(JIPipeAlgorithm algorithm, ModuleItem<?> moduleItem, Module module);
}
