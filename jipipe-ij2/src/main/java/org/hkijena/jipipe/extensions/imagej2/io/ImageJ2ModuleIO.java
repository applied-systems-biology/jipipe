package org.hkijena.jipipe.extensions.imagej2.io;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeDataBatch;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2OpNode;
import org.hkijena.jipipe.extensions.imagej2.ImageJ2OpNodeInfo;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
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

    boolean handlesInput();

    boolean handlesOutput();

    /**
     * Applies changes to an {@link ImageJ2OpNodeInfo} required for this IO
     * @param nodeInfo the node info
     * @param moduleItem the module
     */
    void install(ImageJ2OpNodeInfo nodeInfo, ModuleItem<?> moduleItem);

    /**
     * Applies changes to an {@link ImageJ2OpNode} required for this IO
     * @param node the node info
     * @param moduleItem the module
     */
    void install(ImageJ2OpNode node, ModuleItem<?> moduleItem);

    /**
     * Transfers data from JIPipe into the module
     * @param node the JIPipe node
     * @param dataBatch the data batch
     * @param moduleItem the module item
     * @param module the module
     * @param progressInfo the progress info
     * @return if successful
     */
    boolean transferFromJIPipe(ImageJ2OpNode node, JIPipeDataBatch dataBatch, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo);

    /**
     * Transfers data from JIPipe from the module
     * @param node the JIPipe node
     * @param dataBatch the data batch
     * @param moduleOutputParameters
     * @param moduleItem the module item
     * @param module the module
     * @param progressInfo the progress info
     * @return if successful
     */
    boolean transferToJIPipe(ImageJ2OpNode node, JIPipeDataBatch dataBatch, ParametersData moduleOutputParameters, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo);
}
