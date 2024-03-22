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

package org.hkijena.jipipe.extensions.dev;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
import org.hkijena.jipipe.api.data.JIPipeOutputDataSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

public class ExportNodeSignatureJsonsTool extends JIPipeDesktopMenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ExportNodeSignatureJsonsTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Export all node signatures JSON");
        setToolTipText("Exports all available as JSON describing the functionality.");
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
                node.set("name", new TextNode(instance.getName()));
                node.set("type id", new TextNode(entry.getValue().getId()));
                node.set("description", new TextNode(entry.getValue().getDescription().getBody()));

                // Slot config
                ObjectNode inputSlotConfig = JsonUtils.getObjectMapper().createObjectNode();
                ObjectNode outputSlotConfig = JsonUtils.getObjectMapper().createObjectNode();
                for (JIPipeInputDataSlot inputSlot : instance.getInputSlots()) {
                    JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(inputSlot.getAcceptedDataType());
                    inputSlotConfig.set(inputSlot.getName(), new TextNode("The node has an input slot '" + inputSlot.getName() + "' that receives data of type ID '" +
                            dataInfo.getId() + "' (" + dataInfo.getName() + ")" + ". " + StringUtils.nullToEmpty(inputSlot.getInfo().getDescription())));
                }
                for (JIPipeOutputDataSlot outputSlot : instance.getOutputSlots()) {
                    JIPipeDataInfo dataInfo = JIPipeDataInfo.getInstance(outputSlot.getAcceptedDataType());
                    outputSlotConfig.set(outputSlot.getName(), new TextNode("The node has an output slot '" + outputSlot.getName() + "' that produces data of type ID '" +
                            dataInfo.getId() + "' (" + dataInfo.getName() + ")" + ". " + StringUtils.nullToEmpty(outputSlot.getInfo().getDescription())));
                }
                node.set("inputs", inputSlotConfig);
                node.set("outputs", outputSlotConfig);


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
