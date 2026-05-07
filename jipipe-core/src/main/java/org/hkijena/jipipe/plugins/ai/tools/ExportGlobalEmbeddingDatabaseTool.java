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

package org.hkijena.jipipe.plugins.ai.tools;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.nio.file.Path;
import java.util.Map;

public class ExportGlobalEmbeddingDatabaseTool extends JIPipeDesktopMenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public ExportGlobalEmbeddingDatabaseTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Export node database embeddings");
        setToolTipText("Exports the embeddings database of the nodes.");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/bug.png"));
        addActionListener(e -> runExportTool());
    }

    private void runExportTool() {
        Path outputFile = JIPipeDesktop.saveFile(getDesktopWorkbench().getWindow(), getDesktopWorkbench(), JIPipeFileChooserApplicationSettings.LastDirectoryKey.External, "Output file", HTMLText.EMPTY);
        if (outputFile != null) {
            // TODO: export embeddings
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(), "OK", getText(), JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "Development\nAI";
    }
}
