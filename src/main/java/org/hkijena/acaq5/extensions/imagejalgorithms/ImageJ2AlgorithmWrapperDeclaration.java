package org.hkijena.acaq5.extensions.imagejalgorithms;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import ij.ImagePlus;
import net.imagej.Dataset;
import org.apache.commons.lang.WordUtils;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.data.traits.ACAQDataSlotTraitConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.acaq5.utils.StringUtils;
import org.scijava.Context;
import org.scijava.MenuEntry;
import org.scijava.command.CommandInfo;
import org.scijava.module.Module;
import org.scijava.module.ModuleException;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Algorithm type that wraps around an ImageJ2 algorithm
 */
public class ImageJ2AlgorithmWrapperDeclaration implements ACAQAlgorithmDeclaration {
    private String menuPath;
    private CommandInfo commandInfo;
    private ModuleInfo moduleInfo;
    private String id;
    private String name;
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private List<AlgorithmOutputSlot> outputSlots = new ArrayList<>();
    private ACAQDataSlotTraitConfiguration slotTraitConfiguration;
    private ACAQMutableSlotConfiguration slotConfiguration;
    private BiMap<String, String> inputMap = HashBiMap.create();
    private BiMap<String, String> outputMap = HashBiMap.create();
    private Context context;

    /**
     * Creates a new ImageJ wrapper algorithm type from an ImageJ command
     *
     * @param commandInfo the command
     * @param context     the SciJava context
     * @throws ModuleException triggered when the command cannot be converted into a module
     */
    public ImageJ2AlgorithmWrapperDeclaration(CommandInfo commandInfo, Context context) throws ModuleException {
        this.context = context;
        this.commandInfo = commandInfo;
        Module module = commandInfo.createModule();
        context.inject(module);
        this.moduleInfo = module.getInfo();
        this.name = commandInfo.getMenuPath().getLeaf().getName();
        this.id = "external-imagej2-" + StringUtils.jsonify(commandInfo.getMenuPath().stream().map(MenuEntry::getName).collect(Collectors.joining("-")));
        this.menuPath = commandInfo.getMenuPath().stream().map(MenuEntry::getName).collect(Collectors.joining("\n"));
        this.slotTraitConfiguration = new ACAQDataSlotTraitConfiguration();
    }

    /**
     * Must be run before registering the declaration.
     * Requires that all dependencies are available.
     */
    public void initialize() {
        this.slotConfiguration = new ACAQMutableSlotConfiguration();
        for (ModuleItem<?> moduleItem : ImmutableList.copyOf(moduleInfo.inputs())) {
            if (Dataset.class.isAssignableFrom(moduleItem.getType()) || ImagePlus.class.isAssignableFrom(moduleItem.getType())) {
                String name = findInputSlotName(moduleItem);
                inputMap.put(name, moduleItem.getName());
                slotConfiguration.addSlot(name, new ACAQSlotDefinition(ImagePlusData.class, ACAQDataSlot.SlotType.Input, null), false);
                inputSlots.add(new DefaultAlgorithmInputSlot(ImagePlusData.class, name, false));
            }
        }
        for (ModuleItem<?> moduleItem : ImmutableList.copyOf(moduleInfo.outputs())) {
            if (Dataset.class.isAssignableFrom(moduleItem.getType()) || ImagePlus.class.isAssignableFrom(moduleItem.getType())) {
                String name = findOutputSlotName(moduleItem);
                outputMap.put(name, moduleItem.getName());
                slotConfiguration.addSlot(name, new ACAQSlotDefinition(ImagePlusData.class, ACAQDataSlot.SlotType.Output, null), false);
                outputSlots.add(new DefaultAlgorithmOutputSlot(ImagePlusData.class, name, null, false));
            }
        }
        slotConfiguration.setInputSealed(true);
        slotConfiguration.setOutputSealed(true);
    }

    private String findInputSlotName(ModuleItem<?> moduleItem) {
        return WordUtils.capitalize(moduleItem.getName());
    }

    private String findOutputSlotName(ModuleItem<?> moduleItem) {
        String name = WordUtils.capitalize(moduleItem.getName());
        name = name.replace("input", "output")
                .replace("Input", "Output");
        if (slotConfiguration.hasSlot(name)) {
            name = "Output " + name;
        }
        return StringUtils.makeUniqueString(name, " ", s -> slotConfiguration.hasSlot(s));
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<? extends ACAQAlgorithm> getAlgorithmClass() {
        return ImageJ2AlgorithmWrapper.class;
    }

    @Override
    public ACAQAlgorithm newInstance() {
        return new ImageJ2AlgorithmWrapper(this);
    }

    @Override
    public ACAQAlgorithm clone(ACAQAlgorithm algorithm) {
        return new ImageJ2AlgorithmWrapper((ImageJ2AlgorithmWrapper) algorithm);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getMenuPath() {
        return menuPath;
    }

    @Override
    public ACAQAlgorithmCategory getCategory() {
        return ACAQAlgorithmCategory.Miscellaneous;
    }

    @Override
    public Set<ACAQTraitDeclaration> getPreferredTraits() {
        return Collections.emptySet();
    }

    @Override
    public Set<ACAQTraitDeclaration> getUnwantedTraits() {
        return Collections.emptySet();
    }

    @Override
    public ACAQDataSlotTraitConfiguration getSlotTraitConfiguration() {
        return slotTraitConfiguration;
    }

    @Override
    public List<AlgorithmInputSlot> getInputSlots() {
        return inputSlots;
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    @Override
    public Set<ACAQDependency> getDependencies() {
        return null;
    }

    public CommandInfo getCommandInfo() {
        return commandInfo;
    }

    public ModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public ACAQSlotConfiguration getSlotConfiguration() {
        return slotConfiguration;
    }

    /**
     * @return map from ACAQ slot name to ImageJ2 module item name
     */
    public BiMap<String, String> getInputMap() {
        return inputMap;
    }

    /**
     * @return map from ACAQ slot name to ImageJ2 module item name
     */
    public BiMap<String, String> getOutputMap() {
        return outputMap;
    }

    public Context getContext() {
        return context;
    }
}
