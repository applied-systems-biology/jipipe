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
 * <li>{@link #System} — bundled / plugin-provided templates (registered at
 *     plugin init, or carrying the extension source);</li>
 * <li>{@link #Global} — the user's {@code node-templates/} directory
 *     ({@code JIPipeNodeTemplatesServiceComponent});</li>
 * <li>{@link #Project} — the current project's {@code node-templates} metadata
 *     (travels with saved projects).</li>
 * </ul>
 */
public enum TemplateScope {
    System,
    Global,
    Project
}
