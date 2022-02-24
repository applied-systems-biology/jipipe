package org.hkijena.jipipe.extensions.imagej2;

import com.google.common.collect.Sets;
import net.imagej.ops.OpInfo;
import net.imagej.ops.Ops;
import net.imagej.ops.imagemoments.moments.DefaultMoment11;
import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2JIPipeModuleIOService;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;
import org.jhotdraw.app.SDIApplication;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.MenuEntry;
import org.scijava.MenuPath;
import org.scijava.module.ModuleInfo;
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
    private final List<JIPipeInputSlot> inputSlots = new ArrayList<>();
    private final List<JIPipeOutputSlot> outputSlots = new ArrayList<>();
    private DefaultJIPipeOutputSlot parameterOutputSlot;
    private final Map<ModuleItem<?>, ImageJ2ModuleIO> inputModuleIO = new IdentityHashMap<>();
    private final Map<ModuleItem<?>, ImageJ2ModuleIO> outputModuleIO = new IdentityHashMap<>();
    private final Map<String, ModuleItem<?>> inputSlotToModuleItem = new HashMap<>();
    private final Map<String, ModuleItem<?>> outputSlotToModuleItem = new HashMap<>();
    private final Map<ModuleItem<?>, String> moduleItemToInputSlot = new IdentityHashMap<>();
    private final Map<ModuleItem<?>, String> moduleItemToOutputSlot = new IdentityHashMap<>();
    private HTMLText description;

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

    public ImageJ2OpNodeInfo(Context context, OpInfo opInfo, JIPipeProgressInfo progressInfo) throws InstantiableException {
        this.context = context;
        this.id = "ij2:op:" + opInfo.getName();
        this.opInfo = opInfo;
        this.nodeTypeCategory = new ImagesNodeTypeCategory();
        String title = opInfo.cInfo().getTitle();
        MenuPath menuPath = opInfo.cInfo().getMenuPath();
        if(menuPath.isEmpty() || title.contains(":")) {
            List<String> strings = Arrays.stream(title.split("[:.$]")).map(String::trim).map(WordUtils::capitalize).collect(Collectors.toList());
            if(strings.size() > 1) {
                this.name = "IJ2: " + String.join(" ", org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase(strings.get(strings.size() - 1)));
                strings.remove(strings.size() - 1);
                this.menuPath = "IJ2\n" + String.join("\n", strings);
            }
            else {
                this.name = "IJ2: " + title;
                this.menuPath = "IJ2\n" +  menuPath.stream().map(MenuEntry::getName).collect(Collectors.joining("\n"));
            }
        }
        else {
            this.name = "IJ2: " + title;
            this.menuPath = "IJ2\n" +  menuPath.stream().map(MenuEntry::getName).collect(Collectors.joining("\n"));
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
     * The slot has a data type {@link org.hkijena.jipipe.extensions.multiparameters.datatypes.ParametersData}
     * @return the slot definition
     */
    public DefaultJIPipeOutputSlot getOrCreateParameterDataOutputSlot() {
        if(parameterOutputSlot == null) {
            String slotName = StringUtils.makeUniqueString("Output parameters", " ", this::hasOutputSlot);
            parameterOutputSlot = new DefaultJIPipeOutputSlot(ParametersData.class, slotName, "Output parameters generated by the IJ2 module", null, true);
            outputSlots.add(parameterOutputSlot);
        }
        return parameterOutputSlot;
    }

    public void addInputSlotForModuleItem(ModuleItem<?> item, Class<? extends JIPipeData> dataClass) {
        String name = StringUtils.makeUniqueString(WordUtils.capitalize(StringUtils.makeFilesystemCompatible(item.getName())), " ", inputSlotToModuleItem.keySet());
        inputSlots.add(new DefaultJIPipeInputSlot(dataClass, name, item.getDescription(), true, true));
        inputSlotToModuleItem.put(name, item);
        moduleItemToInputSlot.put(item, name);
    }

    public void addOutputSlotForModuleItem(ModuleItem<?> item, Class<? extends JIPipeData> dataClass) {
        String name = StringUtils.makeUniqueString(WordUtils.capitalize(StringUtils.makeFilesystemCompatible(item.getName())), " ", outputSlotToModuleItem.keySet());
        outputSlots.add(new DefaultJIPipeOutputSlot(dataClass, name, item.getDescription(), null, true));
       outputSlotToModuleItem.put(name, item);
       moduleItemToOutputSlot.put(item, name);
    }

    private void initializeParameters(Context context, JIPipeProgressInfo progressInfo) {
        ImageJ2JIPipeModuleIOService service = context.getService(ImageJ2JIPipeModuleIOService.class);
        if(opInfo.getType() == Ops.Threshold.Otsu.class) {
            System.out.println();
        }
        for (ModuleItem<?> item : opInfo.outputs()) {
            ImageJ2ModuleIO moduleIO = service.findModuleIO(item, JIPipeSlotType.Output);
            if(moduleIO == null) {
                throw new RuntimeException("Unable to resolve output of type " + item.getType());
            }
            moduleIO.install(this, item);
            outputModuleIO.put(item, moduleIO);
        }
        for (ModuleItem<?> item : opInfo.inputs()) {
            if(item.isOutput() && !item.isRequired()) {
                progressInfo.log("Skipping input " + item.getName() + ", as it seems to be an optional input, but is at the same time an output.");
                continue;
            }
            ImageJ2ModuleIO moduleIO = service.findModuleIO(item, JIPipeSlotType.Input);
            if(moduleIO == null) {
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
    public List<JIPipeInputSlot> getInputSlots() {
        return inputSlots;
    }

    @Override
    public List<JIPipeOutputSlot> getOutputSlots() {
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
