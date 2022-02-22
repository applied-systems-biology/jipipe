package org.hkijena.jipipe.extensions.imagej2;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2JIPipeModuleIOService;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.MenuEntry;
import org.scijava.module.ModuleInfo;
import org.scijava.module.ModuleItem;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A {@link JIPipeNodeInfo} implementation that reads its information from an ImageJ2 {@link org.scijava.command.Command}
 */
public class ImageJ2ModuleNodeInfo implements JIPipeNodeInfo {

    private final Context context;
    private final String id;
    private final String menuPath;
    private final String name;
    private final JIPipeNodeTypeCategory nodeTypeCategory;
    private final List<JIPipeInputSlot> inputSlots = new ArrayList<>();
    private final List<JIPipeOutputSlot> outputSlots = new ArrayList<>();
    private final Map<ModuleItem<?>, ImageJ2ModuleIO> inputModuleIO = new HashMap<>();
    private final Map<ModuleItem<?>, ImageJ2ModuleIO> outputModuleIO = new HashMap<>();
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

    public ImageJ2ModuleNodeInfo(Context context, ModuleInfo moduleInfo, JIPipeProgressInfo progressInfo) throws InstantiableException {
        this.context = context;
        this.id = "ij2:module:" + moduleInfo.getDelegateClassName();
        this.nodeTypeCategory = new ImagesNodeTypeCategory();
        this.menuPath = "IJ2\n" +  moduleInfo.getMenuPath().stream().map(MenuEntry::getName).collect(Collectors.joining("\n"));
        this.name = "IJ2: " + moduleInfo.getTitle();
        this.description = new HTMLText("An ImageJ2 function<br/>" + moduleInfo.getName() + "<br/>" + moduleInfo.toString());

        initializeParameters(moduleInfo, context, progressInfo);
    }

    private void initializeParameters(ModuleInfo moduleInfo, Context context, JIPipeProgressInfo progressInfo) {
        ImageJ2JIPipeModuleIOService service = context.getService(ImageJ2JIPipeModuleIOService.class);
        for (ModuleItem<?> item : moduleInfo.inputs()) {
            ImageJ2ModuleIO moduleIO = service.findModuleIO(item);
            if(moduleIO == null) {
                throw new RuntimeException("Unable to resolve input of type " + item.getType());
            }
            moduleIO.install(this, item);
            inputModuleIO.put(item, moduleIO);
        }
        for (ModuleItem<?> item : moduleInfo.outputs()) {
            ImageJ2ModuleIO moduleIO = service.findModuleIO(item);
            if(moduleIO == null) {
                throw new RuntimeException("Unable to resolve output of type " + item.getType());
            }
            moduleIO.install(this, item);
            outputModuleIO.put(item, moduleIO);
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
        return ImageJ2ModuleNode.class;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new ImageJ2ModuleNode(this);
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new ImageJ2ModuleNode((ImageJ2ModuleNode) algorithm);
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
}
