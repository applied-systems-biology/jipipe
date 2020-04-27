package org.hkijena.acaq5.extensions.imagejalgorithms.ij2;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import ij.ImagePlus;
import net.imagej.Dataset;
import org.apache.commons.lang.WordUtils;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQRunnerSubStatus;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQDataInterface;
import org.hkijena.acaq5.api.algorithm.ACAQIteratingAlgorithm;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.parameters.ACAQDynamicParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQSubParameters;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.command.Command;
import org.scijava.command.CommandInfo;
import org.scijava.convert.ConvertService;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;
import org.scijava.service.SciJavaService;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An algorithm that wraps around an ImageJ2 {@link org.scijava.command.Command}
 */
public class ImageJ2AlgorithmWrapper extends ACAQIteratingAlgorithm {

    private ACAQDynamicParameterCollection moduleParameters = new ACAQDynamicParameterCollection();
    private BiMap<String, String> parameterMap = HashBiMap.create();

    /**
     * Creates a new instance
     *
     * @param declaration algorithm declaration
     */
    public ImageJ2AlgorithmWrapper(ImageJ2AlgorithmWrapperDeclaration declaration) {
        super(declaration, declaration.getSlotConfiguration());
        initializeParameters(declaration);
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ImageJ2AlgorithmWrapper(ImageJ2AlgorithmWrapper other) {
        super(other);
        this.moduleParameters = new ACAQDynamicParameterCollection(other.moduleParameters);
    }

    private void initializeParameters(ImageJ2AlgorithmWrapperDeclaration declaration) {
        for (ModuleItem<?> moduleItem : declaration.getModuleInfo().inputs()) {
            if (isCompatibleParameter(moduleItem)) {
                String parameterName = WordUtils.capitalize(moduleItem.getName());
                parameterName = StringUtils.makeUniqueString(parameterName, " ", s -> parameterMap.containsValue(s));
                moduleParameters.addParameter(parameterName, moduleItem.getType());
                parameterMap.put(moduleItem.getName(), parameterName);
            }
        }
        moduleParameters.setAllowUserModification(false);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface, ACAQRunnerSubStatus subProgress, Consumer<ACAQRunnerSubStatus> algorithmProgress, Supplier<Boolean> isCancelled) {
        ImageJ2AlgorithmWrapperDeclaration declaration = (ImageJ2AlgorithmWrapperDeclaration) getDeclaration();
        Module module;
        try {
            Command command = declaration.getCommandInfo().createInstance();
            module = declaration.getCommandInfo().createModule(command);
            declaration.getContext().inject(module);

        } catch (InstantiableException e) {
            throw new RuntimeException(e);
        }

        // Pass parameters
        for (Map.Entry<String, String> entry : parameterMap.entrySet()) {
            Object value = moduleParameters.getValue(entry.getValue());
            module.setInput(entry.getKey(), value);
        }

        // Pass input data
        for (ACAQDataSlot inputSlot : getInputSlots()) {
            ModuleItem<?> moduleItem = module.getInfo().getInput(declaration.getInputMap().get(inputSlot.getName()));
            if (ImagePlus.class.isAssignableFrom(moduleItem.getType())) {
                ImagePlusData data = dataInterface.getInputData(inputSlot, ImagePlusData.class);
                ImagePlus img = data.getImage().duplicate();
                module.setInput(declaration.getInputMap().get(inputSlot.getName()), img);
            } else if (Dataset.class.isAssignableFrom(moduleItem.getType())) {
                ImagePlusData data = dataInterface.getInputData(inputSlot, ImagePlusData.class);
                ImagePlus img = data.getImage();
                ConvertService convertService = ((ImageJ2AlgorithmWrapperDeclaration) getDeclaration()).getContext().getService(ConvertService.class);
                Dataset dataset = convertService.convert(img, Dataset.class);
                module.setInput(declaration.getInputMap().get(inputSlot.getName()), dataset.duplicate());
            } else {
                throw new UnsupportedOperationException("Unsupported input: " + moduleItem);
            }

        }

        // Run the algorithm
        module.run();

        // Extract output data
        for (ACAQDataSlot outputSlot : getOutputSlots()) {
            Object output = module.getOutput(declaration.getOutputMap().get(outputSlot.getName()));
            if (output instanceof ImagePlus) {
                outputSlot.addData(new ImagePlusData((ImagePlus) output));
            } else if (output instanceof Dataset) {
                Dataset dataset = (Dataset) output;
                ConvertService convertService = ((ImageJ2AlgorithmWrapperDeclaration) getDeclaration()).getContext().getService(ConvertService.class);
                ImagePlus imagePlus = convertService.convert(dataset.duplicate(), ImagePlus.class);
                outputSlot.addData(new ImagePlusData(imagePlus));
            } else {
                throw new UnsupportedOperationException("Unsupported data: " + output);
            }
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    @ACAQSubParameters("module-parameters")
    @ACAQDocumentation(name = "ImageJ parameters", description = "Parameters associated to the internal ImageJ algorithm")
    public ACAQDynamicParameterCollection getModuleParameters() {
        return moduleParameters;
    }

    /**
     * Returns true if the command can be wrapped into an ACAQ5 algorithm
     *
     * @param commandInfo the command
     * @param context     SciJava context
     * @return if the command is compatible
     */
    public static boolean isCompatible(CommandInfo commandInfo, Context context) {
        if (commandInfo.toString().toLowerCase().contains("gauss"))
            System.out.println();
        try {
            if (commandInfo.getMenuPath() == null || commandInfo.getMenuPath().isEmpty())
                return false;
            if (commandInfo.isInteractive())
                return false;
            Module module = commandInfo.createModule();
            context.inject(module);
            ModuleInfo moduleInfo = module.getInfo();
            if (moduleInfo == null)
                return false;
            boolean hasSlots = false;
            for (ModuleItem<?> moduleItem : ImmutableList.copyOf(moduleInfo.inputs())) {
                if (!isCompatibleInputSlot(moduleItem) && !isCompatibleParameter(moduleItem) && !isInternalParameter(moduleItem)) {
                    return false;
                }
                if (isCompatibleInputSlot(moduleItem)) {
                    hasSlots = true;
                }
            }
            for (ModuleItem<?> moduleItem : ImmutableList.copyOf(moduleInfo.outputs())) {
                if (!isCompatibleOutputSlot(moduleItem) && !isCompatibleParameter(moduleItem) && !isInternalParameter(moduleItem)) {
                    return false;
                }
                if (isCompatibleOutputSlot(moduleItem)) {
                    hasSlots = true;
                }
            }
            return hasSlots;
        } catch (ModuleException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns true if the parameter is only handled internally by this wrapper
     *
     * @param moduleItem the module item
     * @return if the parameter is only handled internally by this wrapper
     */
    public static boolean isInternalParameter(ModuleItem<?> moduleItem) {
        return Context.class.isAssignableFrom(moduleItem.getType()) || SciJavaService.class.isAssignableFrom(moduleItem.getType());
    }

    /**
     * Returns true if the module item can be adapted into an input slot
     *
     * @param moduleItem the module item
     * @return if the module item can be adapted into an input slot
     */
    public static boolean isCompatibleInputSlot(ModuleItem<?> moduleItem) {
        return Dataset.class.isAssignableFrom(moduleItem.getType()) || ImagePlus.class.isAssignableFrom(moduleItem.getType());
    }

    /**
     * Returns true if the module item can be adapted into an output slot
     *
     * @param moduleItem the module item
     * @return if the module item can be adapted into an output slot
     */
    public static boolean isCompatibleOutputSlot(ModuleItem<?> moduleItem) {
        return Dataset.class.isAssignableFrom(moduleItem.getType()) || ImagePlus.class.isAssignableFrom(moduleItem.getType());
    }

    /**
     * Returns true if the module item is of a parameter type supported by ACAQ5 parameter editors
     *
     * @param moduleItem the module item
     * @return if the module item is of a parameter type supported by ACAQ5 parameter editors
     */
    public static boolean isCompatibleParameter(ModuleItem<?> moduleItem) {
        return ACAQUIParametertypeRegistry.getInstance().hasEditorFor(moduleItem.getType());
    }

}
