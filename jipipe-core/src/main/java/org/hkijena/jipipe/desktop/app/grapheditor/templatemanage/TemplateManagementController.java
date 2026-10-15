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

package org.hkijena.jipipe.desktop.app.grapheditor.templatemanage;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import ij.IJ;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.nodes.database.entries.TemplateScope;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.service.components.JIPipeNodeTemplatesServiceComponent;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The Swing-free store logic behind the "Configure templates" modal (task 9,
 * spec §8.3). This is the controller the modal and the node-browser context
 * menu delegate to; it holds <b>no Swing</b> (no dialog, list, or chooser) so
 * it is unit-testable headlessly.
 * <p>
 * It partitions the template store by {@link TemplateScope} (R9d) and performs
 * the copy / delete / edit / export / import operations over the
 * {@link JIPipeNodeTemplatesServiceComponent}:
 * <ul>
 * <li><b>System</b> — bundled / plugin-provided templates. Read-only:
 *     {@link #delete}/{@link #edit} are no-ops (return {@code false}) and
 *     {@link #importTemplates} to System is a no-op. System is a valid
 *     <em>source</em> for {@link #copyTo} (the "Copy to Global" escape hatch)
 *     but never a target.</li>
 * <li><b>Global</b> — the user's {@code node-templates/} directory. The
 *     critical partition detail: {@code getGlobalTemplates()} now INCLUDES the
 *     bundled System templates (R8g), so the Global view filters
 *     {@code !isPluginTemplate}, or the shipped patterns would show up as
 *     user-global templates.</li>
 * <li><b>Project</b> — the current project's {@code node-templates} metadata
 *     (travels with saved projects).</li>
 * </ul>
 * Every mutation delegates to a service-component method that emits
 * {@code NodeTemplatesRefreshedEvent} on success (single-add/remove methods
 * emit only on success; the collection overloads and {@code editTemplate}
 * always emit). The controller therefore relies on that single delegated emit
 * rather than firing a second one, so the tree / list / relations refresh
 * exactly once per mutation (the same refresh the now-removed standalone
 * template box triggered).
 */
public class TemplateManagementController {

    private final JIPipeNodeTemplatesServiceComponent service;

    public TemplateManagementController() {
        this(JIPipe.getNodeTemplates());
    }

    public TemplateManagementController(JIPipeNodeTemplatesServiceComponent service) {
        this.service = service;
    }

    /**
     * The templates that belong to the given {@link TemplateScope} (the R9d
     * partition, reusing the Task 4 builder logic exactly).
     *
     * @param project the project; nullable (a null project has no Project scope)
     * @param scope   the scope to read
     * @return the templates for that scope (never null; empty when not applicable)
     */
    public List<JIPipeNodeTemplate> forScope(JIPipeProject project, TemplateScope scope) {
        return switch (scope) {
            case System -> service.getGlobalTemplates().stream()
                .filter(service::isPluginTemplate)
                .toList();
            case Global -> service.getGlobalTemplates().stream()
                .filter(t -> service.isInGlobal(t) && !service.isPluginTemplate(t))
                .toList();
            case Project -> project == null ?
                List.of() :
                List.copyOf(project.getMetadata().getNodeTemplates());
        };
    }

    /**
     * Copies a template from one scope into another (always a <b>copy</b>, never
     * a move — the original is left in place). The copy is re-stamped with the
     * user source, so a System template can be "escaped" into Global/Project
     * (its copy becomes a user-owned template that is editable/deletable).
     * <p>
     * The delegated service methods emit the refresh event on success, so this
     * method does not emit again (exactly-once, matching {@link #edit}).
     *
     * @param project the project; required when {@code to} is {@link TemplateScope#Project}
     * @param from    the source scope (System is a valid source; never a target).
     *                Drives the System -> Global branch, which uses the
     *                dedicated user-copy path (see below).
     * @param to      the target scope (Global or Project)
     * @param t       the template to copy
     * @return whether the copy was actually stored (false when {@code to} is
     *         System, the target project is missing, or an equal copy already
     *         exists)
     */
    public boolean copyTo(JIPipeProject project, TemplateScope from, TemplateScope to, JIPipeNodeTemplate t) {
        if (to == TemplateScope.System) {
            // System is read-only: it is a valid source, never a target.
            return false;
        }
        if (to == TemplateScope.Global && from == TemplateScope.System) {
            // The System -> Global "Copy to Global" escape hatch (spec §8.3):
            // the copy is value-equal to the bundled original, so the normal
            // addToGlobal (whose isPluginTemplate / globalTemplates.contains
            // guards are value-based) would reject it. Use the dedicated
            // user-copy path, which dedups only against existing user copies.
            return service.addToGlobalAsUserCopy(t);
        }
        JIPipeNodeTemplate copy = new JIPipeNodeTemplate(t);
        copy.setSource(JIPipeNodeTemplate.SOURCE_USER);
        return switch (to) {
            case Global -> service.addToGlobal(copy);
            case Project -> project != null && service.addToProject(copy, project);
            case System -> false;
        };
    }

    /**
     * Deletes a template from the given scope. System scope is a no-op
     * (read-only), returning {@code false}. {@code removeFromGlobal} /
     * {@code removeFromProject} emit the refresh event on success, so no
     * explicit emit here (exactly-once, matching {@link #edit}).
     *
     * @param project the project; required when {@code scope} is {@link TemplateScope#Project}
     * @param scope   the scope the template is being deleted from
     * @param t       the template to delete
     * @return whether the template was actually removed
     */
    public boolean delete(JIPipeProject project, TemplateScope scope, JIPipeNodeTemplate t) {
        if (scope == TemplateScope.System) {
            return false;
        }
        return switch (scope) {
            case Global -> service.removeFromGlobal(t);
            case Project -> project != null && service.removeFromProject(t, project);
            case System -> false;
        };
    }

    /**
     * Edits a template in place: the live {@code original} is overwritten with
     * the contents of the {@code editedCopy}. System scope is a no-op
     * (read-only), returning {@code false}.
     *
     * @param project     the project; nullable
     * @param scope       the scope the template is being edited in
     * @param original    the live template object to mutate
     * @param editedCopy  the copy carrying the edited values
     * @return whether the edit was applied (false for System scope)
     */
    public boolean edit(JIPipeProject project, TemplateScope scope, JIPipeNodeTemplate original, JIPipeNodeTemplate editedCopy) {
        if (scope == TemplateScope.System) {
            return false;
        }
        service.editTemplate(original, editedCopy, project);
        return true;
    }

    /**
     * Exports the given templates to a file in the legacy JSON-array format
     * (a {@code JsonUtils} pretty-JSON array of {@link JIPipeNodeTemplate}).
     * Exporting is a read-only operation on the store, so all scopes are
     * allowed.
     *
     * @param project   the project; nullable (unused by the store, kept for signature symmetry)
     * @param scope     the scope the templates come from
     * @param templates the templates to export
     * @param file      the target file
     * @return whether the file was written successfully
     */
    public boolean exportTemplates(JIPipeProject project, TemplateScope scope, Collection<JIPipeNodeTemplate> templates, Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(file, JsonUtils.toPrettyJsonString(new ArrayList<>(templates)).getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (IOException e) {
            IJ.handleException(e);
            return false;
        }
    }

    /**
     * Imports templates from a legacy JSON-array file into the given scope.
     * Importing to System is a no-op (read-only). Each parsed template gets its
     * legacy {@code menu-path} key migrated (idempotent) before being stored,
     * matching the service component's disk-reload path.
     *
     * @param project the project; required when {@code scope} is {@link TemplateScope#Project}
     * @param scope   the scope to import into (Global or Project; System is a no-op)
     * @param file    the source file
     * @return the templates that were read and stored (empty on a System import
     *         or a parse failure)
     */
    public List<JIPipeNodeTemplate> importTemplates(JIPipeProject project, TemplateScope scope, Path file) {
        if (scope == TemplateScope.System) {
            return List.of();
        }
        List<JIPipeNodeTemplate> templates = new ArrayList<>();
        try {
            JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(file.toFile());
            for (JsonNode element : ImmutableList.copyOf(node.elements())) {
                JIPipeNodeTemplate template = JsonUtils.getObjectMapper().readerFor(JIPipeNodeTemplate.class).readValue(element);
                // One-shot legacy migration (idempotent, order-safe) so an
                // imported file with a "menu-path" key gets its taxonomy
                // function mapped immediately, not only on the next disk reload.
                template.migrateLegacyMenuPath();
                templates.add(template);
            }
        } catch (IOException e) {
            IJ.handleException(e);
            return List.of();
        }
        // The collection overloads (addToGlobal / addToProject) always emit the
        // refresh event, so no explicit emit here (exactly-once, matching edit).
        switch (scope) {
            case Global -> service.addToGlobal(templates);
            case Project -> {
                if (project != null) {
                    service.addToProject(templates, project);
                }
            }
            case System -> {
            }
        }
        return templates;
    }
}
