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

package org.hkijena.jipipe.plugins.imagej2.io.data.output;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.plugins.imagej2.ImageJ2OpNode;
import org.hkijena.jipipe.plugins.imagej2.ImageJ2OpNodeInfo;
import org.hkijena.jipipe.plugins.imagej2.io.data.DataSlotModuleIO;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.scijava.module.Module;
import org.scijava.module.ModuleItem;
import org.scijava.service.AbstractService;

public abstract class DataSlotModuleOutput<ModuleDataType, JIPipeDataType extends JIPipeData> extends AbstractService implements DataSlotModuleIO {

    @Override
    public Class<?> getAcceptedModuleFieldClass() {
        return getModuleDataType();
    }

    @Override
    public boolean handlesInput() {
        return false;
    }

    @Override
    public boolean handlesOutput() {
        return true;
    }

    @Override
    public void install(ImageJ2OpNode node, ModuleItem<?> moduleItem) {

    }

    @Override
    public void install(ImageJ2OpNodeInfo nodeInfo, ModuleItem<?> moduleItem) {
        nodeInfo.addOutputSlotForModuleItem(moduleItem, getJIPipeDataType());
    }

    @Override
    public boolean transferFromJIPipe(ImageJ2OpNode node, JIPipeSingleIterationStep iterationStep, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        return true;
    }

    @Override
    public boolean transferToJIPipe(ImageJ2OpNode node, JIPipeSingleIterationStep iterationStep, ParametersData moduleOutputParameters, ModuleItem moduleItem, Module module, JIPipeProgressInfo progressInfo) {
        ModuleDataType obj = (ModuleDataType) moduleItem.getValue(module);
        if (obj != null) {
            JIPipeDataType converted = convertModuleToJIPipeData(obj);
            String slotName = node.getModuleNodeInfo().getOutputSlotName(moduleItem);
            iterationStep.addOutputData(slotName, converted, progressInfo);
        } else {
            progressInfo.log("Module output " + moduleItem.getName() + " is null. Skipping.");
        }
        return true;
    }

    public abstract ModuleDataType convertJIPipeToModuleData(JIPipeDataType obj);

    public abstract JIPipeDataType convertModuleToJIPipeData(ModuleDataType obj);

    public abstract Class<ModuleDataType> getModuleDataType();

    public abstract Class<JIPipeDataType> getJIPipeDataType();
}
