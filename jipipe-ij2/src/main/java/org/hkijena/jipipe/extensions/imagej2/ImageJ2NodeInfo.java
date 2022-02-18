package org.hkijena.jipipe.extensions.imagej2;

import ij.ImagePlus;
import net.imagej.Dataset;
import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.ItemIO;
import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.SciJavaPlugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
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
public class ImageJ2NodeInfo implements JIPipeNodeInfo {

    private final Context context;
    private final ConvertService convertService;
    private final PluginInfo<?> pluginInfo;
    private final String id;
    private final String menuPath;
    private final String name;
    private final JIPipeNodeTypeCategory nodeTypeCategory;
    private final List<JIPipeInputSlot> inputSlots = new ArrayList<>();
    private final List<JIPipeOutputSlot> outputSlots = new ArrayList<>();
    private HTMLText description = new HTMLText();

    /**
     * Contains functions that map from an input slot name to a function to extract the result as {@link JIPipeData}
     */
    private final Map<String, Function<Object, JIPipeData>> inputSlotToJIPipeConverters = new HashMap<>();
    /**
     * Contains functions that map from an output slot name to a function to extract the result as {@link JIPipeData}
     */
    private final Map<String, Function<Object, JIPipeData>> outputSlotToJIPipeConverters = new HashMap<>();

    public ImageJ2NodeInfo(Context context, PluginInfo<?> pluginInfo) throws InstantiableException {
        this.context = context;
        this.convertService = context.getService(ConvertService.class);
        this.pluginInfo = pluginInfo;
        this.id = "ij2:" + pluginInfo.getIdentifier();
        this.nodeTypeCategory = new ImagesNodeTypeCategory();

        // Generate menu path and name
        Plugin pluginAnnotation = pluginInfo.getAnnotation();
        String[] pluginMenuPathItems = pluginAnnotation.menuPath().split(">");
        List<String> menuPathItems = new ArrayList<>();
        menuPathItems.add("ImageJ2");
        menuPathItems.addAll(Arrays.asList(pluginMenuPathItems).subList(0, pluginMenuPathItems.length - 1));
        this.menuPath = String.join("\n", menuPathItems);
        this.name = "ImageJ2 " + pluginMenuPathItems[pluginMenuPathItems.length - 1];

        // Look for parameters
        initializeParameters();
    }

    /**
     * Detects {@link org.scijava.plugin.Parameter} annotations that map to
     */
    private void initializeParameters() throws InstantiableException {
        Class<? extends SciJavaPlugin> pluginClass = pluginInfo.loadClass();
        Set<String> existingInputSlots = new HashSet<>();
        Set<String> existingOutputSlots = new HashSet<>();
        for (Field declaredField : pluginClass.getDeclaredFields()) {
            Parameter parameterAnnotation = declaredField.getAnnotation(Parameter.class);
            if(parameterAnnotation == null)
                continue;

            // Parameters can be both input and output in SciJava
            ItemIO itemIO = parameterAnnotation.type();

            // Image data types
            if(declaredField.getType() == ImagePlus.class) {
                if(itemIO == ItemIO.INPUT || itemIO == ItemIO.BOTH) {
                    String slotName = parameterFieldToSlotName(declaredField, existingInputSlots);
                    inputSlotToJIPipeConverters.put(slotName, (instance) -> {
                         ImagePlus imagePlus = (ImagePlus) ReflectionUtils.getFieldValue(declaredField, instance);
                         return new ImagePlusData(imagePlus);
                    });
                }
                if(itemIO == ItemIO.OUTPUT || itemIO == ItemIO.BOTH) {
                    String slotName = parameterFieldToSlotName(declaredField, existingOutputSlots);
                    outputSlotToJIPipeConverters.put(slotName, (instance) -> {
                        ImagePlus imagePlus = (ImagePlus) ReflectionUtils.getFieldValue(declaredField, instance);
                        return new ImagePlusData(imagePlus);
                    });
                }
            }
            else if(declaredField.getType() == Dataset.class) {
                if(itemIO == ItemIO.INPUT || itemIO == ItemIO.BOTH) {
                    String slotName = parameterFieldToSlotName(declaredField, existingInputSlots);
                    inputSlotToJIPipeConverters.put(slotName, (instance) -> {
                        Dataset dataset = (Dataset) ReflectionUtils.getFieldValue(declaredField, instance);
                        ImagePlus imagePlus = convertService.convert(dataset, ImagePlus.class);
                        return new ImagePlusData(imagePlus);
                    });
                }
                if(itemIO == ItemIO.OUTPUT || itemIO == ItemIO.BOTH) {
                    String slotName = parameterFieldToSlotName(declaredField, existingOutputSlots);
                    outputSlotToJIPipeConverters.put(slotName, (instance) -> {
                        ImagePlus imagePlus = (ImagePlus) ReflectionUtils.getFieldValue(declaredField, instance);
                        return new ImagePlusData(imagePlus);
                    });
                }
            }
        }
    }

    private String parameterFieldToSlotName(Field field, Set<String> existing) {
        String slotName = StringUtils.makeUniqueString(StringUtils.makeFilesystemCompatible(WordUtils.capitalize(field.getName())), " ", existing);
        existing.add(slotName);
        return slotName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return ImageJ2Algorithm.class;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new ImageJ2Algorithm(this);
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new ImageJ2Algorithm((ImageJ2Algorithm) algorithm);
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
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    public PluginInfo<?> getPluginInfo() {
        return pluginInfo;
    }
}
