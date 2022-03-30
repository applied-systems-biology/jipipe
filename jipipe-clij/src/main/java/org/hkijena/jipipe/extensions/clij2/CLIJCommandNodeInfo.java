package org.hkijena.jipipe.extensions.clij2;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import ij.IJ;
import net.haesleinhuepf.clij.macro.AbstractCLIJPlugin;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.documentation.HTMLDocumentationTemplate;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.utilities.IsCategorized;
import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.utils.StringUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class CLIJCommandNodeInfo implements JIPipeNodeInfo {

    private final JIPipeNodeTypeCategory nodeTypeCategory = new ImagesNodeTypeCategory();
    private final String nodeId;
    private final String nodeName;
    private final List<JIPipeInputSlot> inputSlots = new ArrayList<>();
    private final List<JIPipeOutputSlot> outputSlots = new ArrayList<>();
    private final BiMap<String, Integer> inputSlotToArgIndexMap = HashBiMap.create();
    private final BiMap<String, Integer> outputSlotToArgIndexMap = HashBiMap.create();
    private final BiMap<String, Integer> parameterIdToArgIndexMap = HashBiMap.create();
    private final JIPipeDynamicParameterCollection nodeParameters = new JIPipeDynamicParameterCollection(false);
    private String menuPath = "CLIJ";
    private final HTMLText nodeDescription;

    public CLIJCommandNodeInfo(Context context, PluginInfo<CLIJMacroPlugin> pluginInfo, JIPipeProgressInfo moduleProgress) {
        this.nodeId = "clij:" + pluginInfo.getIdentifier();
        this.nodeName = createNodeName(pluginInfo);

        // Information only available to an instance
        try {
            CLIJMacroPlugin instance = pluginInfo.createInstance();
            if(instance instanceof IsCategorized) {
                menuPath = "CLIJ\n" + ((IsCategorized) instance).getCategories().replace(',', '\n');
            }
            String description = "";
            String availableForDimensions = "";
            if(instance instanceof OffersDocumentation) {
                description = ((OffersDocumentation) instance).getDescription();
                availableForDimensions = ((OffersDocumentation) instance).getAvailableForDimensions();
            }
            nodeDescription = new HTMLText(new HTMLDocumentationTemplate(description, availableForDimensions, instance, true).toString(false));

            importParameters(instance, moduleProgress);
        } catch (InstantiableException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Based on the run method of {@link net.haesleinhuepf.clij2.AbstractCLIJ2Plugin}
     * @param instance the instance
     * @param moduleProgress the progress info
     */
    private void importParameters(CLIJMacroPlugin instance, JIPipeProgressInfo moduleProgress) {
        String[] parameters = instance.getParameterHelpText().split(",");
        Object[] default_values = null;
        if(instance instanceof AbstractCLIJPlugin) {
            default_values = ((AbstractCLIJPlugin) instance).getDefaultValues();
        }
        if (parameters.length > 0 && parameters[0].length() > 0) {
            for (int i = 0; i < parameters.length; i++) {
                String[] parameterParts = parameters[i].trim().split(" ");
                String parameterType = parameterParts[0];
                String parameterName = parameterParts[1];
                boolean byRef = false;
                if (parameterType.equals("ByRef")) {
                    parameterType = parameterParts[1];
                    parameterName = parameterParts[2];
                    byRef = true;
                }

                if (parameterType.equals("Image")) {
                    if (!(parameterName.contains("destination") || byRef)) {
                        // Image input
                        String slotName = createSlotName(parameterName);
                        inputSlots.add(new DefaultJIPipeInputSlot(CLIJImageData.class, slotName, "", true, false));
                        inputSlotToArgIndexMap.put(slotName, i);
                    }
                    else {
                        // Image output
                        String slotName = createSlotName(parameterName);
                        outputSlots.add(new DefaultJIPipeOutputSlot(CLIJImageData.class, slotName, "", null, true));
                        outputSlotToArgIndexMap.put(slotName, i);
                    }
                } else if (parameterType.equals("String")) {
                    String defaultValue = "";
                    if (default_values != null) {
                       defaultValue = (String)default_values[i];
                    }
                     // String parameter
                    JIPipeMutableParameterAccess parameterAccess = nodeParameters.addParameter(parameterName, String.class);
                    parameterAccess.setName(createParameterName(parameterName));
                    parameterAccess.set(defaultValue);
                    parameterIdToArgIndexMap.put(parameterName, i);
                } else if (parameterType.equals("Boolean")) {
                    boolean defaultValue = true;
                    if (default_values != null) {
                        defaultValue = Boolean.parseBoolean("" + default_values[i]);
                    }
                    // Boolean parameter
                    JIPipeMutableParameterAccess parameterAccess = nodeParameters.addParameter(parameterName, Boolean.class);
                    parameterAccess.setName(createParameterName(parameterName));
                    parameterAccess.set(defaultValue);
                    parameterIdToArgIndexMap.put(parameterName, i);
                } else { // Number
                    double defaultValue = 2.0;
                    if (default_values != null) {
                       defaultValue = Double.parseDouble("" + default_values[i]);
                    }
                    // Double parameter
                    JIPipeMutableParameterAccess parameterAccess = nodeParameters.addParameter(parameterName, Double.class);
                    parameterAccess.setName(createParameterName(parameterName));
                    parameterAccess.set(defaultValue);
                    parameterIdToArgIndexMap.put(parameterName, i);
                }
            }
        }
    }

    public BiMap<String, Integer> getInputSlotToArgIndexMap() {
        return inputSlotToArgIndexMap;
    }

    public BiMap<String, Integer> getOutputSlotToArgIndexMap() {
        return outputSlotToArgIndexMap;
    }

    public BiMap<String, Integer> getParameterIdToArgIndexMap() {
        return parameterIdToArgIndexMap;
    }

    private String createSlotName(String parameterName) {
        return StringUtils.removeDuplicateDelimiters(WordUtils.capitalizeFully(parameterName.replace('_', ' ')), " ");
    }

    private String createParameterName(String parameterName) {
        parameterName = parameterName.replace('_', ' ');
        return StringUtils.removeDuplicateDelimiters(WordUtils.capitalizeFully(String.join(" ", org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase(parameterName))), " ");
    }

    public JIPipeDynamicParameterCollection getNodeParameters() {
        return nodeParameters;
    }

    private String createNodeName(PluginInfo<CLIJMacroPlugin> pluginInfo) {
        String name = pluginInfo.getName();
        if(!StringUtils.isNullOrEmpty(name)) {
            name = name.replace("_", " ");
            name = WordUtils.capitalizeFully(String.join(" ", org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase(name)));
            name = StringUtils.removeDuplicateDelimiters(name, " ");
            name = name.replace("Clij", "CLIJ");
            name = name.replace("CLIJ 2", "CLIJ2");
            for (int i = 0; i < 5; i++) {
                name = name.replace(i + " D", i + "D");
            }
        }
        else {
            name = pluginInfo.getIdentifier();
        }
        return name;
    }

    @Override
    public String getId() {
        return nodeId;
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return CLIJCommandNode.class;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new CLIJCommandNode(this);
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new CLIJCommandNode((CLIJCommandNode) algorithm);
    }

    @Override
    public String getName() {
        return nodeName;
    }

    @Override
    public HTMLText getDescription() {
        return nodeDescription;
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
}
