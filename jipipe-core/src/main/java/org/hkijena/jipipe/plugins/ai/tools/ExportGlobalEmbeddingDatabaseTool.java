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
import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeEmbeddingDatabase;
import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeGlobalEmbeddingSearch;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.JIPipeDesktop;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

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
        // Check if AI is enabled
        if (!AIApplicationSettings.getInstance().isEnableAI()) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "AI functionality is disabled. Please enable it in the AI settings.",
                    getText(), JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get the global embedding database
        JIPipeEmbeddingDatabase embeddingDatabase = JIPipeGlobalEmbeddingSearch.getInstance().getEmbeddingDatabase();

        // Resolve the model ID: try the AI service first, then fall back to the AI search's current model
        String modelId = null;
        if (JIPipe.isInstantiated()) {
            JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
            modelId = aiService.getModelId();
        }
        if (modelId == null) {
            modelId = JIPipeGlobalEmbeddingSearch.getInstance().getCurrentModelId();
        }

        if (modelId == null) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "No embedding model is currently loaded. Please configure and load an embedding model first.",
                    getText(), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if there are any embeddings to export
        Set<String> nodeIds = embeddingDatabase.getNodeIds(modelId);
        if (nodeIds.isEmpty()) {
            int result = JOptionPane.showConfirmDialog(getDesktopWorkbench().getWindow(),
                    "The embedding database is empty for model '" + modelId + "'.\n" +
                            "You may need to compute embeddings first by using the AI search.\n\n" +
                            "Do you want to continue anyway?",
                    getText(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Show file chooser
        Path outputFile = JIPipeDesktop.saveFile(getDesktopWorkbench().getWindow(), getDesktopWorkbench(),
                JIPipeFileChooserApplicationSettings.LastDirectoryKey.External, "Output file", HTMLText.EMPTY);
        if (outputFile == null) {
            return;
        }

        // Perform the export
        try {
            embeddingDatabase.saveToDisk(outputFile, modelId);

            // Verify the export by re-checking the count
            int count = nodeIds.size();
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "Successfully exported " + count + " embedding(s) for model '" + modelId + "'\nto " + outputFile,
                    getText(), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "Failed to export embedding database: " + e.getMessage(),
                    getText(), JOptionPane.ERROR_MESSAGE);
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
