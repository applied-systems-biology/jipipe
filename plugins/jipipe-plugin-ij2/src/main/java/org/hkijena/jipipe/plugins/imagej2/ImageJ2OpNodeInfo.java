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

package org.hkijena.jipipe.plugins.imagej2;

import net.imagej.ops.OpInfo;
import org.apache.commons.text.WordUtils;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSlotRole;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.plugins.imagej2.io.ImageJ2JIPipeModuleIOService;
import org.hkijena.jipipe.plugins.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.MenuEntry;
import org.scijava.MenuPath;
import org.scijava.module.ModuleItem;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A {@link JIPipeNodeInfo} implementation that reads its information from an ImageJ2 {@link org.scijava.command.Command}
 */
public class ImageJ2OpNodeInfo implements JIPipeNodeInfo {

    private final Context context;
    private final String id;
    private final OpInfo opInfo;
    private final String menuPath;
    private final String name;
    private final JIPipeNodeTypeCategory nodeTypeCategory;
    private final List<AddJIPipeInputSlot> inputSlots = new ArrayList<>();
    private final List<AddJIPipeOutputSlot> outputSlots = new ArrayList<>();
    private final Map<ModuleItem<?>, ImageJ2ModuleIO> inputModuleIO = new IdentityHashMap<>();
    private final Map<ModuleItem<?>, ImageJ2ModuleIO> outputModuleIO = new IdentityHashMap<>();
    private final Map<String, ModuleItem<?>> inputSlotToModuleItem = new HashMap<>();
    private final Map<String, ModuleItem<?>> outputSlotToModuleItem = new HashMap<>();
    private final Map<ModuleItem<?>, String> moduleItemToInputSlot = new IdentityHashMap<>();
    private final Map<ModuleItem<?>, String> moduleItemToOutputSlot = new IdentityHashMap<>();
    /**
     * Contains functions that map from an input slot name to a function to extract the result as {@link JIPipeData}
     */
    private final Map<String, Function<Object, JIPipeData>> inputSlotToJIPipeConverters = new HashMap<>();
    /**
     * Contains functions that map from an output slot name to a function to extract the result as {@link JIPipeData}
     */
    private final Map<String, Function<Object, JIPipeData>> outputSlotToJIPipeConverters = new HashMap<>();
    /**
     * Parameter collection that will be assigned to the node
     */
    private final JIPipeDynamicParameterCollection nodeParameters = new JIPipeDynamicParameterCollection();
    private DefaultAddJIPipeOutputSlot parameterOutputSlot;
    private HTMLText description;

    public ImageJ2OpNodeInfo(Context context, OpInfo opInfo, JIPipeProgressInfo progressInfo) throws InstantiableException {
        this.context = context;
        this.id = "ij2:op:" + opInfo.getName();
        this.opInfo = opInfo;
        this.nodeTypeCategory = new ImagesNodeTypeCategory();
        String title = opInfo.cInfo().getTitle();
        MenuPath menuPath = opInfo.cInfo().getMenuPath();
        if (menuPath.isEmpty() || title.contains(":")) {
            List<String> strings = Arrays.stream(title.split("[:.$]")).map(String::trim).map(WordUtils::capitalize).collect(Collectors.toList());
            if (strings.size() > 1) {
                this.name = "IJ2: " + String.join(" ", org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase(strings.get(strings.size() - 1)));
                strings.remove(strings.size() - 1);
                this.menuPath = "IJ2\n" + String.join("\n", strings);
            } else {
                this.name = "IJ2: " + title;
                this.menuPath = "IJ2\n" + menuPath.stream().map(MenuEntry::getName).collect(Collectors.joining("\n"));
            }
        } else {
            this.name = "IJ2: " + title;
            this.menuPath = "IJ2\n" + menuPath.stream().map(MenuEntry::getName).collect(Collectors.joining("\n"));
        }
        this.description = new HTMLText(StringUtils.orElse(opInfo.cInfo().getDescription(), "The developer provided no description") + "<br/><br/>This node was automatically imported from ImageJ2. " +
                "Please be aware that JIPipe cannot guarantee that there are no issues.");

        initializeParameters(context, progressInfo);
    }

    public boolean hasParameterDataOutputSlot() {
        return parameterOutputSlot != null;
    }

    /**
     * Gets or creates the output slot for exported module parameters
     * The slot has a data type {@link org.hkijena.jipipe.plugins.multiparameters.datatypes.ParametersData}
     *
     * @return the slot definition
     */
    public DefaultAddJIPipeOutputSlot getOrCreateParameterDataOutputSlot() {
        if (parameterOutputSlot == null) {
            String slotName = StringUtils.makeUniqueString("Output parameters", " ", this::hasOutputSlot);
            parameterOutputSlot = new DefaultAddJIPipeOutputSlot(ParametersData.class, slotName, "Output parameters generated by the IJ2 module", null, true, JIPipeDataSlotRole.Data);
            outputSlots.add(parameterOutputSlot);
        }
        return parameterOutputSlot;
    }

    public void addInputSlotForModuleItem(ModuleItem<?> item, Class<? extends JIPipeData> dataClass) {
        String name = StringUtils.makeUniqueString(WordUtils.capitalize(StringUtils.makeFilesystemCompatible(item.getName())), " ", inputSlotToModuleItem.keySet());
        inputSlots.add(new DefaultAddJIPipeInputSlot(dataClass, name, item.getDescription(), true, true, JIPipeDataSlotRole.Data));
        inputSlotToModuleItem.put(name, item);
        moduleItemToInputSlot.put(item, name);
    }

    public void addOutputSlotForModuleItem(ModuleItem<?> item, Class<? extends JIPipeData> dataClass) {
        String name = StringUtils.makeUniqueString(WordUtils.capitalize(StringUtils.makeFilesystemCompatible(item.getName())), " ", outputSlotToModuleItem.keySet());
        outputSlots.add(new DefaultAddJIPipeOutputSlot(dataClass, name, item.getDescription(), null, true, JIPipeDataSlotRole.Data));
        outputSlotToModuleItem.put(name, item);
        moduleItemToOutputSlot.put(item, name);
    }

    private void initializeParameters(Context context, JIPipeProgressInfo progressInfo) {
        ImageJ2JIPipeModuleIOService service = context.getService(ImageJ2JIPipeModuleIOService.class);
        for (ModuleItem<?> item : opInfo.outputs()) {
            ImageJ2ModuleIO moduleIO = service.findModuleIO(item, JIPipeSlotType.Output);
            if (moduleIO == null) {
                throw new RuntimeException("Unable to resolve output of type " + item.getType());
            }
            moduleIO.install(this, item);
            outputModuleIO.put(item, moduleIO);
        }
        for (ModuleItem<?> item : opInfo.inputs()) {
            if (item.isOutput() && !item.isRequired()) {
                progressInfo.log("Skipping input " + item.getName() + ", as it seems to be an optional input, but is at the same time an output.");
                continue;
            }
            ImageJ2ModuleIO moduleIO = service.findModuleIO(item, JIPipeSlotType.Input);
            if (moduleIO == null) {
                throw new RuntimeException("Unable to resolve input of type " + item.getType());
            }
            moduleIO.install(this, item);
            inputModuleIO.put(item, moduleIO);
        }

    }

    public JIPipeDynamicParameterCollection getNodeParameters() {
        return nodeParameters;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return ImageJ2OpNode.class;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new ImageJ2OpNode(this);
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new ImageJ2OpNode((ImageJ2OpNode) algorithm);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HTMLText getDescription() {
        return description;
    }

    @Override
    public String getMenuPath() {
        return menuPath;
    }

    @Override
    public JIPipeNodeTypeCategory getCategory() {
        return nodeTypeCategory;
    }

    @Override
    public List<AddJIPipeInputSlot> getInputSlots() {
        return inputSlots;
    }

    @Override
    public List<AddJIPipeOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    @Override
    public List<String> getAdditionalCitations() {
        return Collections.singletonList("Rueden, C., Dietz, C., Horn, M., Schindelin, J., Northan, B., Berthold, M. & Eliceiri, K. (2021). ImageJ Ops [Software]. https://imagej.net/Ops.");
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    public Map<ModuleItem<?>, ImageJ2ModuleIO> getInputModuleIO() {
        return inputModuleIO;
    }

    public Map<ModuleItem<?>, ImageJ2ModuleIO> getOutputModuleIO() {
        return outputModuleIO;
    }

    public String getOutputSlotName(ModuleItem<?> moduleItem) {
        return moduleItemToOutputSlot.get(moduleItem);
    }

    public String getInputSlotName(ModuleItem<?> moduleItem) {
        return moduleItemToInputSlot.get(moduleItem);
    }

    public OpInfo getOpInfo() {
        return opInfo;
    }

    public Context getContext() {
        return context;
    }
}
