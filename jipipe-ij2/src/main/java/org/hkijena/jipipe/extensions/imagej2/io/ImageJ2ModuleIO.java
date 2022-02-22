package org.hkijena.jipipe.extensions.imagej2.io;

import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2ModuleNodeInfo;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
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
     * Applies changes to an {@link ImageJ2ModuleNodeInfo} required for this IO
     * @param nodeInfo the node info
     * @param moduleItem the module
     */
    void install(ImageJ2ModuleNodeInfo nodeInfo, ModuleItem<?> moduleItem);

    /**
     * Applies changes to an {@link ImageJ2ModuleNode} required for this IO
     * @param node the node info
     * @param moduleItem the module
     */
    void install(ImageJ2ModuleNode node, ModuleItem<?> moduleItem);

    /**
     * Transfers data from JIPipe into the module
     * @param node the JIPipe node
     * @param module the module
     * @return if successful
     */
    boolean transferFromJIPipe(ImageJ2ModuleNode node, ModuleItem<?> moduleItem, Module module);

    /**
     * Transfers data from JIPipe from the module
     * @param node the JIPipe node
     * @param module the module
     * @return if successful
     */
    boolean transferToJIPipe(ImageJ2ModuleNode node, ModuleItem<?> moduleItem, Module module);
}
