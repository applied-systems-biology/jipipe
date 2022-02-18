package org.hkijena.jipipe.extensions.imagej2;

import net.imagej.ops.OpInfo;
import net.imagej.ops.OpUtils;
import net.imglib2.Interval;
import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeManualParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.ItemIO;
import org.scijava.convert.ConvertService;
import org.scijava.module.ModuleItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * A {@link JIPipeNodeInfo} implementation that reads its information from an ImageJ2 {@link org.scijava.command.Command}
 */
public class ImageJ2OpNodeInfo implements JIPipeNodeInfo {

    private final Context context;
    private final ConvertService convertService;
    private final String id;
    private final OpInfo opInfo;
    private final String menuPath;
    private final String name;
    private final JIPipeNodeTypeCategory nodeTypeCategory;
    private final List<JIPipeInputSlot> inputSlots = new ArrayList<>();
    private final List<JIPipeOutputSlot> outputSlots = new ArrayList<>();
    private HTMLText description;
    private boolean conversionSuccessful;

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

    public ImageJ2OpNodeInfo(JIPipe jiPipe, Context context, OpInfo opInfo, boolean single, JIPipeProgressInfo progressInfo) throws InstantiableException {
        this.context = context;
        this.convertService = context.getService(ConvertService.class);
        this.id = "ij2:op:" + opInfo.getName() + "::" + opInfo.cInfo().getClassName();
        this.opInfo = opInfo;
        this.nodeTypeCategory = new ImagesNodeTypeCategory();

        // Generate menu path
        List<String> menuPathItems = new ArrayList<>();
        menuPathItems.add("ImageJ2 Ops");
        for (String s : opInfo.getName().split("\\.")) {
            menuPathItems.add(WordUtils.capitalize(s));
        }
        if(single) {
            menuPathItems.remove(menuPathItems.size() - 1);
        }
        this.menuPath = String.join("\n", menuPathItems);

        this.name = "Op: " + WordUtils.capitalize(opInfo.getSimpleName());
        this.description = new HTMLText("An ImageJ2 Op<br/>" + opInfo.getName() + "<br/>" + OpUtils.opString(opInfo.cInfo()));
        initializeParameters(jiPipe, progressInfo);
    }

    private void initializeParameters(JIPipe jiPipe, JIPipeProgressInfo progressInfo) {
        Set<String> existingInputs = new HashSet<>();
        Set<String> existingOutputs = new HashSet<>();
        for (ModuleItem<?> item : opInfo.inputs()) {
            // Detect Interval type as images
            if(Interval.class.isAssignableFrom(item.getType())) {
               if(item.getIOType() == ItemIO.INPUT || item.getIOType() == ItemIO.BOTH) {
                   String slotName = StringUtils.makeUniqueString(StringUtils.makeFilesystemCompatible(WordUtils.capitalize(item.getName())),
                           " ",
                           existingInputs);
                   existingInputs.add(slotName);
                   inputSlots.add(new DefaultJIPipeInputSlot(ImagePlusData.class, slotName, true, false));
               }
                if(item.getIOType() == ItemIO.OUTPUT || item.getIOType() == ItemIO.BOTH) {
                    String slotName = StringUtils.makeUniqueString(StringUtils.makeFilesystemCompatible(WordUtils.capitalize(item.getName())),
                            " ",
                            existingOutputs);
                    existingOutputs.add(slotName);
                    outputSlots.add(new DefaultJIPipeOutputSlot(ImagePlusData.class, slotName, null, false));
                }
            }
            else if(item.getIOType() == ItemIO.INPUT) {
                // Is this a parameter?
                JIPipeParameterTypeInfo parameterTypeInfo = jiPipe.getParameterTypeRegistry().getInfoByFieldClass(item.getType());
                if(parameterTypeInfo != null) {
                    nodeParameters.addParameter(StringUtils.orElse(item.getPersistKey(), item.getName()), parameterTypeInfo.getFieldClass(), item.getName(), item.getDescription());
//                    JIPipeManualParameterAccess.builder().setFieldClass(parameterTypeInfo.getFieldClass())
//                            .setKey(StringUtils.orElse(item.getPersistKey(), item.getName()))
//                            .setGetter(item.)
                }
                else {
                    progressInfo.log("Unable to convert parameter " + item + " of type " + item.getType() + ". Is input of unknown data type and not supported by parameters.");
                    conversionSuccessful = false;
                }
            }
            else {
                progressInfo.log("Unable to convert parameter " + item + " of type " + item.getType());
                conversionSuccessful = false;
            }
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
        return ImageJ2OpAlgorithm.class;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new ImageJ2OpAlgorithm(this);
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new ImageJ2OpAlgorithm((ImageJ2OpAlgorithm) algorithm);
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

    public OpInfo getOpInfo() {
        return opInfo;
    }

    public boolean isConversionSuccessful() {
        return conversionSuccessful;
    }
}
