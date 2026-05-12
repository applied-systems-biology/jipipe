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
import org.hkijena.jipipe.api.nodes.database.embeddings.JIPipeGlobalEmbeddingSearch;
import org.hkijena.jipipe.api.service.components.JIPipeAIServiceComponent;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeMenuExtensionTarget;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.running.JIPipeDesktopRunExecuteUI;
import org.hkijena.jipipe.plugins.ai.AIApplicationSettings;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Menu tool that allows users to manually trigger AI embedding computation
 * for all node database entries, with visible progress reporting.
 * <p>
 * Uses the tiered architecture: global entries are stored in the
 * {@link JIPipeGlobalEmbeddingSearch} singleton, local entries in the
 * per-project transient database.
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

        // Get the AI search instance
        JIPipeAINodeDatabaseSearch aiSearch = JIPipeNodeDatabase.getInstance().getAiSearch();

        // Get the global and local embedding databases
        JIPipeEmbeddingDatabase globalDb = JIPipeGlobalEmbeddingSearch.getInstance().getEmbeddingDatabase();
        JIPipeEmbeddingDatabase localDb = aiSearch.getLocalEmbeddingDatabase();

        // Resolve the model ID: try the AI service first, then fall back to the global search's current model
        String resolvedModelId = null;
        if (JIPipe.isInstantiated()) {
            JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
            resolvedModelId = aiService.getModelId();
        }
        if (resolvedModelId == null) {
            resolvedModelId = JIPipeGlobalEmbeddingSearch.getInstance().getCurrentModelId();
        }
        final String modelId = resolvedModelId;

        if (modelId == null) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "No embedding model is currently loaded. Please configure and load an embedding model first.",
                    getText(), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Ensure the global cache is loaded before computing
        JIPipeGlobalEmbeddingSearch.getInstance().initialize(modelId, JIPipe.getInstance().getAiService().getEmbeddingProgressInfo());

        // Get the entries and split into global/local
        List<JIPipeNodeDatabaseEntry> entries = aiSearch.getEntries();
        if (entries.isEmpty()) {
            JOptionPane.showMessageDialog(getDesktopWorkbench().getWindow(),
                    "The node database is empty. Nothing to embed.",
                    getText(), JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<JIPipeNodeDatabaseEntry> globalEntries = new ArrayList<>();
        List<JIPipeNodeDatabaseEntry> localEntries = new ArrayList<>();
        for (JIPipeNodeDatabaseEntry entry : entries) {
            if (JIPipeAINodeDatabaseSearch.isGlobalEntry(entry)) {
                globalEntries.add(entry);
            } else {
                localEntries.add(entry);
            }
        }

        // Count how many entries already have embeddings
        long alreadyEmbeddedGlobal = globalEntries.stream()
                .filter(e -> globalDb.hasEmbedding(modelId, JIPipeEmbeddingDatabase.entryToId(e)))
                .count();
        long alreadyEmbeddedLocal = localEntries.stream()
                .filter(e -> localDb.hasEmbedding(modelId, JIPipeEmbeddingDatabase.entryToId(e)))
                .count();
        long alreadyEmbedded = alreadyEmbeddedGlobal + alreadyEmbeddedLocal;

        if (alreadyEmbedded > 0) {
            if (JOptionPane.showConfirmDialog(getDesktopWorkbench().getWindow(), "There are already " + alreadyEmbedded + " embeddings. Clear them?", "Compute embeddings", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                globalDb.clearModel(modelId);
                localDb.clearModel(modelId);
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
                        "Global entries: " + globalEntries.size() + ", Local entries: " + localEntries.size() + "\n\n" +
                        "This may take a while depending on the number of entries.",
                getText(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        // Create and run the embedding build task with progress UI
        JIPipeAIServiceComponent aiService = JIPipe.getInstance().getAiService();
        BuildEmbeddingIndexRun run = new BuildEmbeddingIndexRun(globalDb, localDb, globalEntries, localEntries, modelId, aiService);
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
     * Handles both global and local entries with their respective databases.
     */
    public static class BuildEmbeddingIndexRun extends DefaultJIPipeRunnable {

        private final JIPipeEmbeddingDatabase globalDb;
        private final JIPipeEmbeddingDatabase localDb;
        private final List<JIPipeNodeDatabaseEntry> globalEntries;
        private final List<JIPipeNodeDatabaseEntry> localEntries;
        private final String modelId;
        private final JIPipeAIServiceComponent aiService;

        public BuildEmbeddingIndexRun(JIPipeEmbeddingDatabase globalDb,
                                      JIPipeEmbeddingDatabase localDb,
                                      List<JIPipeNodeDatabaseEntry> globalEntries,
                                      List<JIPipeNodeDatabaseEntry> localEntries,
                                      String modelId,
                                      JIPipeAIServiceComponent aiService) {
            this.globalDb = globalDb;
            this.localDb = localDb;
            this.globalEntries = globalEntries;
            this.localEntries = localEntries;
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
            int totalEntries = globalEntries.size() + localEntries.size();
            progress.setProgress(0, totalEntries);
            progress.log("Computing embeddings for " + totalEntries + " entries (model: " + modelId
                    + ", global: " + globalEntries.size() + ", local: " + localEntries.size() + ")");

            int computed = 0;
            int skipped = 0;
            int failed = 0;
            int index = 0;

            // Process global entries
            for (JIPipeNodeDatabaseEntry entry : globalEntries) {
                if (progress.isCancelled()) {
                    progress.log("Cancelled by user after " + index + " entries.");
                    break;
                }

                String nodeId = JIPipeEmbeddingDatabase.entryToId(entry);

                if (!globalDb.hasEmbedding(modelId, nodeId)) {
                    String text = JIPipeEmbeddingDatabase.entryToText(entry);
                    try {
                        float[] embedding = aiService.tryEmbed(text).get(30, TimeUnit.SECONDS);
                        if (embedding != null) {
                            globalDb.setEmbeddingWithHash(modelId, nodeId, embedding, text);
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

                index++;
                progress.setProgress(index, totalEntries);
                progress.incrementProgress();
            }

            // Process local entries
            for (JIPipeNodeDatabaseEntry entry : localEntries) {
                if (progress.isCancelled()) {
                    progress.log("Cancelled by user after " + index + " entries.");
                    break;
                }

                String nodeId = JIPipeEmbeddingDatabase.entryToId(entry);

                if (!localDb.hasEmbedding(modelId, nodeId)) {
                    String text = JIPipeEmbeddingDatabase.entryToText(entry);
                    try {
                        float[] embedding = aiService.tryEmbed(text).get(30, TimeUnit.SECONDS);
                        if (embedding != null) {
                            localDb.setEmbeddingWithHash(modelId, nodeId, embedding, text);
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

                index++;
                progress.setProgress(index, totalEntries);
                progress.incrementProgress();
            }

            // Save only the global database to disk after computation
            if (computed > 0) {
                progress.log("Saving global embedding cache to disk ...");
                globalDb.saveUserCache(modelId);
            }

            progress.log("Done. Computed: " + computed + ", Skipped (cached): " + skipped + ", Failed: " + failed);
        }
    }
}
