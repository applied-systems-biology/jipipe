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

package org.hkijena.jipipe.plugins.imagej2.io.parameters.output;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagej2.ImageJ2OpNode;
import org.hkijena.jipipe.plugins.imagej2.ImageJ2OpNodeInfo;
import org.hkijena.jipipe.plugins.imagej2.io.parameters.ParameterModuleIO;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.service.AbstractService;

/**
 * Handles input parameters (passed from/to JIPipe parameters)
 *
 * @param <ModuleType> the type used in the module
 * @param <JIPipeType> the type used in JIPipe
 */
public abstract class ParameterImageJ2ModuleOutput<ModuleType, JIPipeType> extends AbstractService implements ParameterModuleIO {
    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return getModuleClass();
    }

    @Override
    public void install(ImageJ2OpNodeInfo nodeInfo, ModuleItem<?> moduleItem) {
        nodeInfo.getOrCreateParameterDataOutputSlot();
    }

    @Override
    public void install(ImageJ2OpNode node, ModuleItem<?> moduleItem) {
    }

    @Override
    public boolean transferFromJIPipe(ImageJ2OpNode node, JIPipeSingleIterationStep iterationStep, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2OpNode node, JIPipeSingleIterationStep iterationStep, ParametersData moduleOutputParameters, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        moduleOutputParameters.getParameterData().put(moduleItem.getName(), convertFromModuleToJIPipe((ModuleType) moduleItem.getValue(module)));
        return true;
    }

    @Override
    public boolean handlesInput() {
        return false;
    }

    @Override
    public boolean handlesOutput() {
        return true;
    }

    public abstract JIPipeType convertFromModuleToJIPipe(ModuleType obj);

    public abstract ModuleType convertFromJIPipeToModule(JIPipeType obj);

    public abstract Class<JIPipeType> getJIPipeParameterClass();

    public abstract Class<ModuleType> getModuleClass();
}
