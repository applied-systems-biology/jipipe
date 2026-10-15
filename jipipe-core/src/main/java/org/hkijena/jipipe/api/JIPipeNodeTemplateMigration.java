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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * One-shot, idempotent migration of the legacy {@code menu-path} key of
 * {@link JIPipeNodeTemplate} files to the {@code taxonomy-function} reference
 * (spec §9 of the template-taxonomy integration design).
 * <p>
 * The first segment of the old menu path is matched case-insensitively against
 * the display names of the registered taxonomy functions — top-level branches
 * first (on a name tie), then child functions — with a small abbreviation
 * table for the legacy menu categories
 * (e.g. {@code "Image" → "Images"}). The result is the referenced function's id;
 * when nothing matches (or the segment is blank), an <b>unset</b> reference is
 * returned — the catch-all placement ({@code jipipe:function:templates}) is
 * deliberately NOT applied here, but by the template's database entry instead,
 * so a future function registration can absorb previously unmatched templates.
 */
public final class JIPipeNodeTemplateMigration {

    /**
     * Legacy menu-category abbreviations → taxonomy function display names
     */
    private static final Map<String, String> ABBREVIATIONS = new LinkedHashMap<>();

    static {
        ABBREVIATIONS.put("image", "Images");
        ABBREVIATIONS.put("table", "Tables");
        ABBREVIATIONS.put("track", "Tracking");
        ABBREVIATIONS.put("roi", "ROIs");
        ABBREVIATIONS.put("plot", "Plotting");
        ABBREVIATIONS.put("annotation", "Annotations");
        ABBREVIATIONS.put("export", "Export");
        ABBREVIATIONS.put("data", "Data Sources");
    }

    private JIPipeNodeTemplateMigration() {
    }

    /**
     * Migrates the first segment of a legacy menu path to a taxonomy function
     * reference.
     * <p>
     * The segment is normalized (trimmed and, if it is a legacy abbreviation,
     * replaced by the matching function name) and then matched
     * case-insensitively against the display names of all registered taxonomy
     * functions, preferring top-level branches (parent == null) on a name tie
     * (the export sub-leaves "Images"/"Tables" shadow the top-level branches
     * of the same name), then any child function.
     *
     * @param firstMenuSegment the first segment of the legacy menu path, or
     *                         {@code null}/blank for no path
     * @return a reference holding the matched function's id, or an unset
     * reference when nothing matched. Never throws: with an unbooted JIPipe
     * instance the registry is treated as empty (unset).
     */
    public static JIPipeTaxonomyFunctionRef migrate(String firstMenuSegment) {
        if (StringUtils.isNullOrEmpty(firstMenuSegment)) {
            return new JIPipeTaxonomyFunctionRef();
        }
        if (JIPipe.getInstance() == null) {
            return new JIPipeTaxonomyFunctionRef();
        }
        String name = firstMenuSegment.trim();
        String candidate = ABBREVIATIONS.getOrDefault(name.toLowerCase(Locale.ROOT), name);
        var functions = JIPipe.getNodes().getFunctions();

        // Pass 1: top-level branches (parent == null). On a display-name tie the
        // top-level branch wins over any child — the registry contains childless
        // export sub-leaves literally named "Images" and "Tables"
        // (jipipe:function:export-images / export-tables) that shadow the
        // top-level Images/Tables branches, but a legacy menu-path FIRST segment
        // is a top-level menu category and must map to the branch
        // ("Image" → jipipe:function:images, NOT jipipe:function:export-images).
        // Do not "fix" this back to leaf-first.
        JIPipeNodeTaxonomyFunction match = null;
        for (JIPipeNodeTaxonomyFunction function : functions.topLevel()) {
            if (equalsIgnoreCase(candidate, function.getName())) {
                match = function;
                break;
            }
        }
        // Pass 2: any child function (leaf or branch) — a segment without a
        // same-named top-level branch (e.g. "Thresholding") falls through here
        if (match == null) {
            for (JIPipeNodeTaxonomyFunction function : functions.all()) {
                if (function.getParent() != null && equalsIgnoreCase(candidate, function.getName())) {
                    match = function;
                    break;
                }
            }
        }
        if (match == null) {
            return new JIPipeTaxonomyFunctionRef();
        }
        return new JIPipeTaxonomyFunctionRef(match.getId());
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        return a != null && a.equalsIgnoreCase(b);
    }

    /**
     * Appends an auditable italic footer to the template description that
     * records the legacy menu path, so the automatic mapping can be traced
     * back. The footer is only appended when the description does not already
     * contain a "Previously" footer (idempotency).
     *
     * @param desc    the template description
     * @param oldPath the legacy menu path's segments joined with " → "
     *                (e.g. {@code "Image → Threshold"})
     * @return the description with the footer appended (a new instance; the
     * input is not mutated)
     */
    public static HTMLText appendPreviouslyFooter(HTMLText desc, String oldPath) {
        String current = (desc == null) ? "" : desc.getHtml();
        if (current.contains("Previously")) {
            return desc == null ? new HTMLText() : desc;
        }
        String body = (desc == null) ? "" : desc.getBody();
        String footer = "<i><br>Previously: " + oldPath + "</i>";
        return new HTMLText(body.isEmpty() ? footer : body + footer);
    }
}
