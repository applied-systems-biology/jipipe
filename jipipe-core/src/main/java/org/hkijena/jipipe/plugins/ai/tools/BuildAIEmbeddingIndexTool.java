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
import org.hkijena.jipipe.api.DefaultJIPipeRunnable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.database.JIPipeAINodeDatabaseSearch;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabase;
import org.hkijena.jipipe.api.nodes.database.JIPipeNodeDatabaseEntry;
import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeEmbeddingDatabase;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Menu tool that allows users to manually trigger AI embedding computation
 * for all node database entries, with visible progress reporting.
 */
public class BuildAIEmbeddingIndexTool extends JIPipeDesktopMenuExtension {

    /**
     * Creates a new instance
     *
     * @param workbench workbench the extension is attached to
     */
    public BuildAIEmbeddingIndexTool(JIPipeDesktopWorkbench workbench) {
        super(workbench);
        setText("Build embedding index");
        setToolTipText("Computes AI embeddings for all node database entries that do not yet have one.");
        setIcon(JIPipe.RESOURCES.getIcon16("actions/database.png"));
        addActionListener(e -> runBuildTool());
    }

    private void runBuildTool() {
        // Check if AI is enabled
        if (!AIApplicationSettings.getInstance().isEnableAI()) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "AI functionality is disabled. Please enable it in the AI settings.",
                    getText(), JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get the AI search instance and embedding database
        JIPipeAINodeDatabaseSearch aiSearch = JIPipeNodeDatabase.getInstance().getAiSearch();
        JIPipeEmbeddingDatabase embeddingDatabase = aiSearch.getEmbeddingDatabase();

        // Resolve the model ID: try the AI service first, then fall back to the AI search's current model
        String resolvedModelId = null;
        if (JIPipe.isInstantiated()) {
            JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
            resolvedModelId = aiService.getModelId();
        }
        if (resolvedModelId == null) {
            resolvedModelId = aiSearch.getCurrentModelId();
        }
        final String modelId = resolvedModelId;

        if (modelId == null) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "No embedding model is currently loaded. Please configure and load an embedding model first.",
                    getText(), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Ensure the cache is loaded before computing
        embeddingDatabase.loadUserCache(modelId);

        // Get the entries
        List<JIPipeNodeDatabaseEntry> entries = aiSearch.getEntries();
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "The node database is empty. Nothing to embed.",
                    getText(), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Count how many entries already have embeddings
        long alreadyEmbedded = entries.stream()
                .filter(e -> embeddingDatabase.hasEmbedding(modelId, JIPipeEmbeddingDatabase.entryToId(e)))
                .count();

        if(alreadyEmbedded > 0) {
            if(JOptionPane.showConfirmDialog(getDesktopWorkbench().getWindow(), "There are already " + alreadyEmbedded + " embeddings. Clear them?", "Compute embeddings", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                embeddingDatabase.clearModel(modelId);
                alreadyEmbedded = 0;
            }
        }

        if (alreadyEmbedded == entries.size()) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "All " + entries.size() + " entries already have embeddings for model '" + modelId + "'.",
                    getText(), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Confirm with the user
        int result = JOptionPane.showConfirmDialog(getDesktopWorkbench().getWindow(),
                "Compute embeddings for " + (entries.size() - alreadyEmbedded) + " entries " +
                        "(" + alreadyEmbedded + " already cached) using model '" + modelId + "'?\n\n" +
                        "This may take a while depending on the number of entries.",
                getText(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        // Create and run the embedding build task with progress UI
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        BuildEmbeddingIndexRun run = new BuildEmbeddingIndexRun(embeddingDatabase, entries, modelId, aiService);
        JIPipeDesktopRunExecuteUI.runInDialog(getDesktopWorkbench(), getDesktopWorkbench().getWindow(), run);
    }

    @Override
    public JIPipeMenuExtensionTarget getMenuTarget() {
        return JIPipeMenuExtensionTarget.ProjectToolsMenu;
    }

    @Override
    public String getMenuPath() {
        return "AI";
    }

    /**
     * Runnable that computes embeddings for all node database entries with progress reporting.
     */
    public static class BuildEmbeddingIndexRun extends DefaultJIPipeRunnable {

        private final JIPipeEmbeddingDatabase embeddingDatabase;
        private final List<JIPipeNodeDatabaseEntry> entries;
        private final String modelId;
        private final JIPipeAIServiceComponent aiService;

        public BuildEmbeddingIndexRun(JIPipeEmbeddingDatabase embeddingDatabase,
                                      List<JIPipeNodeDatabaseEntry> entries,
                                      String modelId,
                                      JIPipeAIServiceComponent aiService) {
            this.embeddingDatabase = embeddingDatabase;
            this.entries = entries;
            this.modelId = modelId;
            this.aiService = aiService;
        }

        @Override
        public String getTaskLabel() {
            return "Build embedding index";
        }

        @Override
        public void run() {
            JIPipeProgressInfo progress = getProgressInfo();
            progress.setProgress(0, entries.size());
            progress.log("Computing embeddings for " + entries.size() + " entries (model: " + modelId + ")");

            int computed = 0;
            int skipped = 0;
            int failed = 0;

            for (int i = 0; i < entries.size(); i++) {
                if (progress.isCancelled()) {
                    progress.log("Cancelled by user after " + i + " entries.");
                    break;
                }

                JIPipeNodeDatabaseEntry entry = entries.get(i);
                String nodeId = JIPipeEmbeddingDatabase.entryToId(entry);

                if (!embeddingDatabase.hasEmbedding(modelId, nodeId)) {
                    String text = JIPipeEmbeddingDatabase.entryToText(entry);
                    try {
                        float[] embedding = aiService.tryEmbed(text).get(30, TimeUnit.SECONDS);
                        if (embedding != null) {
                            embeddingDatabase.setEmbedding(modelId, nodeId, embedding);
                            computed++;
                        } else {
                            failed++;
                            progress.log("Warning: Null embedding for '" + entry.getName() + "'");
                        }
                    } catch (Exception e) {
                        failed++;
                        progress.log("Warning: Failed to embed '" + entry.getName() + "': " + e.getMessage());
                    }
                } else {
                    skipped++;
                }

                progress.setProgress(i + 1, entries.size());
                progress.incrementProgress();
            }

            // Save the cache after computation
            if (computed > 0) {
                progress.log("Saving embedding cache to disk ...");
                embeddingDatabase.saveUserCache(modelId);
            }

            progress.log("Done. Computed: " + computed + ", Skipped (cached): " + skipped + ", Failed: " + failed);
        }
    }
}
