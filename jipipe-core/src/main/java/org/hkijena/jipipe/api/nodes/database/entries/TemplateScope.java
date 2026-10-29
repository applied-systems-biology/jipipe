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

package org.hkijena.jipipe.api.nodes.database.entries;

/**
 * The storage scope of a node template, used to group template database entries
 * under the synthetic {@code Templates / <scope>} branch of the node browser.
 * <p>
 * Scope is derived from <em>where a template is stored</em>, not a stored field:
 * <ul>
 * <li>{@link #System} - bundled / plugin-provided templates (registered at
 *     plugin init, or carrying the extension source);</li>
 * <li>{@link #Downloaded} - templates in the user's global store that came
 *     from a repository (their source is the repository URL, not the user
 *     source). Read-only like System: they can be copied into the user's own
 *     templates (the copy becomes editable) and removed, but not edited;</li>
 * <li>{@link #Global} - the user's own templates in the
 *     {@code node-templates/} directory
 *     ({@code JIPipeNodeTemplatesServiceComponent});</li>
 * <li>{@link #Project} - the current project's {@code node-templates} metadata
 *     (travels with saved projects).</li>
 * </ul>
 */
public enum TemplateScope {
    System,
    Global,
    Project,
    Downloaded
}
