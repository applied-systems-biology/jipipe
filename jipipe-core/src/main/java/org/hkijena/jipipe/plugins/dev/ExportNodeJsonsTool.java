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

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
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

public class ExportNodeJsonsTool extends JIPipeDesktopMenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ExportNodeJsonsTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Export all nodes as graph JSON");
        setToolTipText("Exports all available nodes as copy-able JSON into a directory.");
        setIcon(UIUtils.getIconFromResources("actions/bug.png"));
        addActionListener(e -> runExportTool());
    }

    private void runExportTool() {
        Path outputDirectory = FileChooserSettings.saveDirectory(getDesktopWorkbench().getWindow(), FileChooserSettings.LastDirectoryKey.External, "Output directory");
        if (outputDirectory != null) {
            for (Map.Entry<String, JIPipeNodeInfo> entry : JIPipe.getNodes().getRegisteredNodeInfos().entrySet()) {
                System.out.println("Export " + entry.getKey());
                String fileName = StringUtils.makeFilesystemCompatible(entry.getKey()) + ".json";
                JIPipeGraphNode node = entry.getValue().newInstance();
                JIPipeGraph graph = new JIPipeGraph();
                graph.insertNode(node);
                JsonUtils.saveToFile(graph, outputDirectory.resolve(fileName));
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
