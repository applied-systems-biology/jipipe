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

package org.hkijena.jipipe.api.service.components;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import ij.IJ;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeNodeTemplateMigration;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceComponent;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEvent;
import org.hkijena.jipipe.plugins.nodetemplate.NodeTemplatesRefreshedEventEmitter;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class JIPipeNodeTemplatesServiceComponent extends JIPipeServiceComponent {
    // Identity-based, NOT value-based: plugin-ness is a property of the exact
    // instances registered from plugins (via addFromPlugin), not of a template's
    // value. JIPipeNodeTemplate.equals/hashCode compare only name+description+
    // data, so a value-equal HashSet would make isPluginTemplate() return true for
    // a user COPY of a bundled template (misclassifying it as System) and would
    // make addToGlobal_ reject it (pluginTemplates.contains(copy) == true) —
    // silently killing the System -> Global "Copy to Global" escape hatch.
    private final Set<JIPipeNodeTemplate> pluginTemplates = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<JIPipeNodeTemplate> globalTemplates = new ArrayList<>();
    private final BiMap<JIPipeNodeTemplate, Path> globalTemplatesPaths = HashBiMap.create();
    private final NodeTemplatesRefreshedEventEmitter nodeTemplatesRefreshedEventEmitter = new NodeTemplatesRefreshedEventEmitter();

    public JIPipeNodeTemplatesServiceComponent(JIPipeService service) {
        super(service);
    }

    public Path getStoragePath() {
        return PathUtils.getJIPipeUserDir().resolve("node-templates");
    }

    public Path getLegacyStoragePath() {
        return PathUtils.getLegacyJIPipeUserDir().resolve("node-templates");
    }

    public void reloadGlobalTemplates(JIPipeProgressInfo progressInfo) {
        // Plugin templates (registered via addFromPlugin, e.g. the bundled
        // corpus-mined System patterns) are a persistent subset of
        // globalTemplates that does NOT come from the user's node-templates
        // directory. Preserve them across the clear() so the disk reload only
        // affects the user's own templates.
        List<JIPipeNodeTemplate> pluginSubset =
            new ArrayList<>(globalTemplates).stream().filter(this::isPluginTemplate).toList();
        globalTemplates.clear();
        globalTemplatesPaths.clear();

        try {
            Path storagePath = getStoragePath();
            progressInfo.log("Storage path is " + storagePath);
            Files.createDirectories(storagePath);

            for (Path jsonPath : PathUtils.findFilesByExtensionIn(storagePath, ".json")) {
                try {
                    progressInfo.log("Loading " + jsonPath);
                    JIPipeNodeTemplate nodeTemplate = JsonUtils.readFromFile(jsonPath, JIPipeNodeTemplate.class);
                    // One-shot migration of the legacy "menu-path" key to the
                    // taxonomy-function reference (idempotent, order-safe)
                    nodeTemplate.migrateLegacyMenuPath();
                    globalTemplates.add(nodeTemplate);
                    globalTemplatesPaths.put(nodeTemplate, jsonPath);
                } catch (Throwable e) {
                    progressInfo.resolve(jsonPath.toString()).log(ExceptionUtils.getStackTrace(e));
                    e.printStackTrace();
                }
            }
        } catch (Throwable e) {
            progressInfo.log(ExceptionUtils.getStackTrace(e));
            e.printStackTrace();
            IJ.handleException(e);
        }

        // Re-add the preserved plugin templates (they are not on disk in the
        // user store; they live in the bundle/plugin registry).
        globalTemplates.addAll(pluginSubset);

        emitRefreshedEvent();
    }

    public void addFromPlugin(JIPipeNodeTemplate nodeTemplate) {
        pluginTemplates.add(nodeTemplate);
        nodeTemplate.setSource(JIPipeNodeTemplate.SOURCE_EXTENSION);
        globalTemplates.add(nodeTemplate);
        pluginTemplates.add(nodeTemplate);
    }

    public List<JIPipeNodeTemplate> getGlobalTemplates() {
        return Collections.unmodifiableList(globalTemplates);
    }

    /**
     * Gets all templates
     *
     * @param project the project. can be null (no project templates).
     * @return all templates in a mutable list
     */
    public List<JIPipeNodeTemplate> getAllTemplates(JIPipeProject project) {
        List<JIPipeNodeTemplate> result = new ArrayList<>(globalTemplates);
        if (project != null) {
            result.addAll(project.getMetadata().getNodeTemplates());
        }
        return result;
    }

    public boolean isPluginTemplate(JIPipeNodeTemplate template) {
        return JIPipeNodeTemplate.SOURCE_EXTENSION.equals(template.getSource()) || pluginTemplates.contains(template);
    }

    public boolean addToGlobal_(JIPipeNodeTemplate template) {
        if (isPluginTemplate(template)) {
            return false;
        }
        if (globalTemplates.contains(template)) {
            return false;
        }
        globalTemplates.add(template);
        Path targetPath = getStoragePath().resolve(UUID.randomUUID() + ".json");
        JsonUtils.saveToFile(template, targetPath);
        // Record the file so removeFromGlobal can delete it (mirrors the
        // reloadGlobalTemplates bookkeeping); without this the file is orphaned
        // and the "deleted" template resurrects on the next reload.
        globalTemplatesPaths.put(template, targetPath);
        return true;
    }

    public boolean isInGlobal(JIPipeNodeTemplate template) {
        return globalTemplates.contains(template);
    }

    public boolean isInProject(JIPipeNodeTemplate template, JIPipeProject project) {
        return project.getMetadata().getNodeTemplates().contains(template);
    }

    public boolean addToGlobal(JIPipeNodeTemplate template) {
        if (addToGlobal_(template)) {
            emitRefreshedEvent();
            return true;
        }
        return false;
    }

    public void addToGlobal(Collection<JIPipeNodeTemplate> templates) {
        for (JIPipeNodeTemplate template : templates) {
            addToGlobal_(template);
        }
        emitRefreshedEvent();
    }

    /**
     * The System -> Global "Copy to Global" escape-hatch path (spec §8.3): makes
     * a user-owned copy of a bundled/plugin template and stores it globally.
     * <p>
     * This deliberately bypasses {@link #addToGlobal_}'s two value-based guards:
     * the {@code isPluginTemplate} guard (a value-equal copy of a bundled template
     * would trip it) and the blanket {@code globalTemplates.contains} guard (the
     * bundled original is value-equal and already present in the list). Instead it
     * dedups ONLY against existing USER (non-plugin) global copies, so a user who
     * clicks "Copy to Global" twice does not get two identical copies — while the
     * bundled original (a plugin template) never blocks the copy. Persistence
     * mirrors {@link #addToGlobal_} verbatim (add to {@code globalTemplates} +
     * write the per-template JSON file).
     *
     * @param pluginTemplate the bundled (System-scope) template to copy
     * @return whether the user copy was actually stored (false when a value-equal
     *         user copy already exists in the global store)
     */
    public boolean addToGlobalAsUserCopy(JIPipeNodeTemplate pluginTemplate) {
        JIPipeNodeTemplate copy = new JIPipeNodeTemplate(pluginTemplate);
        // Re-stamp user-owned (belt-and-suspenders with the identity-based
        // pluginTemplates set, so the copy is classified Global, not System).
        copy.setSource(JIPipeNodeTemplate.SOURCE_USER);
        // Dedup ONLY against existing USER global copies (the bundled original is
        // a plugin template, so isPluginTemplate(existing) is true and skipped).
        for (JIPipeNodeTemplate existing : globalTemplates) {
            if (!isPluginTemplate(existing) && existing.equals(copy)) {
                return false;
            }
        }
        globalTemplates.add(copy);
        Path targetPath = getStoragePath().resolve(UUID.randomUUID() + ".json");
        JsonUtils.saveToFile(copy, targetPath);
        // Record the file (keyed by the added `copy` instance) so removeFromGlobal
        // can delete it — without this the escape copy's file is orphaned and the
        // "deleted" template resurrects on the next reload.
        globalTemplatesPaths.put(copy, targetPath);
        emitRefreshedEvent();
        return true;
    }

    public boolean addToProject_(JIPipeNodeTemplate template, JIPipeProject project) {
        if (isPluginTemplate(template)) {
            return false;
        }
        if (project.getMetadata().getNodeTemplates().contains(template)) {
            return false;
        }

        project.getMetadata().getNodeTemplates().add(template);

        return true;
    }

    public boolean addToProject(JIPipeNodeTemplate template, JIPipeProject project) {
        if (addToProject_(template, project)) {
            project.getMetadata().emitParameterChangedEvent("node-templates");
            emitRefreshedEvent();
            return true;
        }
        return false;
    }

    public void addToProject(Collection<JIPipeNodeTemplate> templates, JIPipeProject project) {
        for (JIPipeNodeTemplate template : templates) {
            addToProject_(template, project);
        }
        project.getMetadata().emitParameterChangedEvent("node-templates");
        emitRefreshedEvent();
    }

    public NodeTemplatesRefreshedEventEmitter getNodeTemplatesRefreshedEventEmitter() {
        return nodeTemplatesRefreshedEventEmitter;
    }

    public void emitRefreshedEvent() {
        nodeTemplatesRefreshedEventEmitter.emit(new NodeTemplatesRefreshedEvent());
    }

    public void editTemplate(JIPipeNodeTemplate target, JIPipeNodeTemplate source, JIPipeProject project) {
        JIPipeTaxonomyFunctionRef oldFunction = target.getTaxonomyFunction();
        target.copyFrom(source);
        target.setSource(JIPipeNodeTemplate.SOURCE_USER);
        // §9.4: re-picking the function in the editor clears the
        // auto-mapping "Previously:" footer (the function is now an explicit
        // user choice). A save that does NOT change the function keeps it.
        JIPipeTaxonomyFunctionRef newFunction = target.getTaxonomyFunction();
        if (oldFunction != null && newFunction != null
            && !Objects.equals(oldFunction.getId(), newFunction.getId())) {
            target.setDescription(JIPipeNodeTemplateMigration.clearPreviouslyFooter(target.getDescription()));
        }
        if (project != null) {
            project.getMetadata().emitParameterChangedEvent("node-templates");
        }
        emitRefreshedEvent();
    }

    public boolean removeFromProject(JIPipeNodeTemplate template, JIPipeProject project) {
        if (project.getMetadata().getNodeTemplates().remove(template)) {
            project.getMetadata().emitParameterChangedEvent("node-templates");
            emitRefreshedEvent();
            return true;
        }
        return false;
    }

    public boolean removeFromGlobal(JIPipeNodeTemplate template) {
        // Remove by INSTANCE, not value: JIPipeNodeTemplate.equals compares
        // name+description+data only, so a value-based remove would evict the
        // first value-equal entry — e.g. deleting an unedited user copy of a
        // bundled pattern would delete the bundled original from the in-memory
        // store (the on-disk file mapping below is still per-instance, so only
        // the list membership was wrong). The membership check uses the same
        // instance match; the file lookup keeps the value-based get (the
        // BiMap is value-keyed) and resolves to the removed instance's own file.
        if (globalTemplates.removeIf(t -> t == template)) {
            Path targetPath = globalTemplatesPaths.get(template);
            if (targetPath != null && Files.isRegularFile(targetPath)) {
                try {
                    Files.delete(targetPath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            // Drop the mapping so it does not go stale after deletion (the map
            // is cleared at reload anyway).
            globalTemplatesPaths.remove(template);
            emitRefreshedEvent();
            return true;
        }
        return false;
    }
}
