package org.hkijena.jipipe.extensions.clij2;

import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.documentation.HTMLDocumentationTemplate;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.utilities.IsCategorized;
import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
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
    private String menuPath = "CLIJ";
    private final HTMLText nodeDescription;

    public CLIJCommandNodeInfo(Context context, PluginInfo<CLIJMacroPlugin> pluginInfo, JIPipeProgressInfo moduleProgress) {
        this.nodeId = "clij:" + pluginInfo.getIdentifier();
        this.nodeName = createNodeName(pluginInfo);


        // Create an instance
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
        } catch (InstantiableException e) {
            throw new RuntimeException(e);
        }
    }

    private String createNodeName(PluginInfo<CLIJMacroPlugin> pluginInfo) {
        String name = pluginInfo.getName();
        if(!StringUtils.isNullOrEmpty(name)) {
            name = name.replace("_", " ");
            name = WordUtils.capitalizeFully(String.join(" ", org.apache.commons.lang.StringUtils.splitByCharacterTypeCamelCase(name)));
            while(name.contains("  ")) {
                name = name.replace( "  ", " ");
            }
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
