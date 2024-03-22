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

package org.hkijena.jipipe.extensions.imagej2.io;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
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
     *
     * @return the class
     */
    Class<?> getAcceptedModuleFieldClass();

    boolean handlesInput();

    boolean handlesOutput();

    /**
     * Applies changes to an {@link ImageJ2OpNodeInfo} required for this IO
     *
     * @param nodeInfo   the node info
     * @param moduleItem the module
     */
    void install(ImageJ2OpNodeInfo nodeInfo, ModuleItem<?> moduleItem);

    /**
     * Applies changes to an {@link ImageJ2OpNode} required for this IO
     *
     * @param node       the node info
     * @param moduleItem the module
     */
    void install(ImageJ2OpNode node, ModuleItem<?> moduleItem);

    /**
     * Transfers data from JIPipe into the module
     *
     * @param node          the JIPipe node
     * @param iterationStep the data batch
     * @param moduleItem    the module item
     * @param module        the module
     * @param progressInfo  the progress info
     * @return if successful
     */
    boolean transferFromJIPipe(ImageJ2OpNode node, JIPipeSingleIterationStep iterationStep, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo);

    /**
     * Transfers data from JIPipe from the module
     *
     * @param node                   the JIPipe node
     * @param iterationStep          the data batch
     * @param moduleOutputParameters the module output parameters
     * @param moduleItem             the module item
     * @param module                 the module
     * @param progressInfo           the progress info
     * @return if successful
     */
    boolean transferToJIPipe(ImageJ2OpNode node, JIPipeSingleIterationStep iterationStep, ParametersData moduleOutputParameters, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo);
}
