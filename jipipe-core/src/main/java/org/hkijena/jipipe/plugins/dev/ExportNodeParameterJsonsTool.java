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

package org.hkijena.jipipe.plugins.dev;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.settings.FileChooserSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class ExportNodeParameterJsonsTool extends JIPipeDesktopMenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ExportNodeParameterJsonsTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Export all node properties JSON");
        setToolTipText("Exports all available as JSON describing the properties.");
        setIcon(UIUtils.getIconFromResources("actions/bug.png"));
        addActionListener(e -> runExportTool());
    }

    private void runExportTool() {
        Path outputDirectory = FileChooserSettings.saveDirectory(getDesktopWorkbench().getWindow(), FileChooserSettings.LastDirectoryKey.External, "Output directory");
        if (outputDirectory != null) {
            for (Map.Entry<String, JIPipeNodeInfo> entry : JIPipe.getNodes().getRegisteredNodeInfos().entrySet()) {
                JIPipeGraphNode instance = entry.getValue().newInstance();
                System.out.println("Export " + entry.getKey());
                String fileName = StringUtils.makeFilesystemCompatible(entry.getKey()) + ".json";
                ObjectNode output = JsonUtils.getObjectMapper().createObjectNode();
                ObjectNode nodeMap = JsonUtils.getObjectMapper().createObjectNode();
                ObjectNode node = JsonUtils.getObjectMapper().createObjectNode();

                // Known global settings
                node.set("jipipe:graph-compartment", new TextNode("UUID of the graph compartment. Ignored."));
                node.set("jipipe:alias-id", new TextNode("Human-readable unique name of the node within the graph. Ignored."));
                node.set("jipipe:ui-grid-location", new TextNode("Contains the location of the node in the UI. Can be empty. Ignored."));
                node.set("jipipe:node-info-id", new TextNode("Type ID of the node. Must be exact. Important."));

                // Slot config
                ObjectNode slotConfig = JsonUtils.getObjectMapper().createObjectNode();
                ObjectNode inputSlotConfig = JsonUtils.getObjectMapper().createObjectNode();
                ObjectNode outputSlotConfig = JsonUtils.getObjectMapper().createObjectNode();
                for (JIPipeInputDataSlot inputSlot : instance.getInputSlots()) {
                    JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(inputSlot.getAcceptedDataType());
                    inputSlotConfig.set(inputSlot.getName(), new TextNode("Defines an input slot '" + inputSlot.getName() + "' that receives data of type ID '" +
                            dataInfo.getId() + "' (" + dataInfo.getName() + ")"));
                }
                for (JIPipeOutputDataSlot outputSlot : instance.getOutputSlots()) {
                    JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(outputSlot.getAcceptedDataType());
                    outputSlotConfig.set(outputSlot.getName(), new TextNode("Defines an output slot '" + outputSlot.getName() + "' that produces data of type ID '" +
                            dataInfo.getId() + "' (" + dataInfo.getName() + ")"));
                }
                slotConfig.set("input", inputSlotConfig);
                slotConfig.set("output", outputSlotConfig);
                node.set("jipipe:slot-configuration", slotConfig);

                // Parameters
                JIPipeParameterTree tree = new JIPipeParameterTree(instance);
                for (Map.Entry<String, JIPipeParameterAccess> accessEntry : tree.getParameters().entrySet()) {
                    node.set(accessEntry.getKey(), new TextNode("Parameter '" + accessEntry.getValue().getName() + "' of type with ID '" +
                            JIPipe.getParameterTypes().getInfoByFieldClass(accessEntry.getValue().getFieldClass()).getId() + "'. " + accessEntry.getValue().getDescription()));
                }

                nodeMap.set(UUID.randomUUID().toString(), node);
                output.set("nodes", nodeMap);
                output.set("edges", JsonUtils.getObjectMapper().createArrayNode());
                JsonUtils.saveToFile(output, outputDirectory.resolve(fileName));
            }
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(), "OK", getText(), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Development";
    }
}
