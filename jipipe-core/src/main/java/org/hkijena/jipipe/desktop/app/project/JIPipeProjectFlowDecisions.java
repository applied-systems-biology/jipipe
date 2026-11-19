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

package org.hkijena.jipipe.desktop.app.project;

import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * The pure, window-free decisions of the project open and create flows (issue #1379).
 * The GUI dialogs show their prompt and feed the user's answer into these decisions;
 * the automation operations (create_project, open_project) feed the explicit parameter
 * values. Both routes run the same downstream flow, so the interactive and the
 * automated behavior cannot fork.
 * <p>
 * The decisions are static and testable without a GUI (the prompt dialogs themselves
 * are verified by eye in the app, per the project's test policy).
 */
public final class JIPipeProjectFlowDecisions {

    private JIPipeProjectFlowDecisions() {
    }

    /**
     * The explicit open format of the automation open_project operation. AUTO is the
     * GUI's no-prompt default (a .crate.zip file imports through the RO-Crate flow, a
     * legacy .jip with a .jipx sibling opens the sibling), JIPX and LEGACY force the
     * choice the sibling prompt offers, CRATE forces the RO-Crate import.
     */
    public enum OpenFormat {
        AUTO,
        /**
         * Prefer the modern project crate (.jipx).
         */
        JIPX,
        /**
         * Prefer the legacy project file (.jip).
         */
        LEGACY,
        /**
         * Open through the RO-Crate import.
         */
        CRATE;

        /**
         * Parses an automation parameter value (case-insensitive). Returns AUTO for a
         * null or blank value and null for an unknown value.
         */
        public static OpenFormat parse(String value) {
            if (value == null || value.isBlank()) {
                return AUTO;
            }
            return switch (value.trim().toLowerCase(Locale.ROOT)) {
                case "auto" -> AUTO;
                case "jipx" -> JIPX;
                case "legacy" -> LEGACY;
                case "crate" -> CRATE;
                default -> null;
            };
        }
    }

    /**
     * The choice behind the interactive "Project crate available" prompt: which file
     * the open proceeds with when the target legacy .jip has a .jipx sibling.
     */
    public enum SiblingChoice {
        /**
         * Open the .jipx sibling (the prompt's default button).
         */
        OPEN_CRATE,
        /**
         * Continue with the legacy .jip.
         */
        OPEN_LEGACY,
        /**
         * Abort the open (the prompt's dismissal).
         */
        CANCELLED
    }

    /**
     * The choice behind the interactive "Project already open" prompt.
     */
    public enum AlreadyOpenChoice {
        /**
         * Bring the existing window to the front instead of loading a second instance
         * (the prompt's default button).
         */
        BRING_TO_FRONT,
        /**
         * Load a second instance of the project.
         */
        OPEN_ANYWAY,
        /**
         * Abort the open (the prompt's dismissal).
         */
        ABORT
    }

    /**
     * The choice behind the interactive "Open JIPipe output" prompt (the output
     * folder flow): what happens with the results after the project opened.
     */
    public enum OutputFolderChoice {
        /**
         * Open the results in a new document tab (the prompt's default button).
         */
        NEW_TAB,
        /**
         * Load the results into the cache (memory-heavy).
         */
        INTO_CACHE,
        /**
         * Skip loading the results (the prompt's dismissal: the project opens
         * without them).
         */
        SKIP
    }

    /**
     * Maps an open format to the sibling choice the open proceeds with (only consulted
     * when a .jipx sibling actually exists): auto and jipx open the sibling (the
     * prompt's default), legacy opens the legacy file. CRATE never reaches this
     * decision (the crate routing runs first).
     */
    public static SiblingChoice siblingChoiceFor(OpenFormat format) {
        return format == OpenFormat.LEGACY ? SiblingChoice.OPEN_LEGACY : SiblingChoice.OPEN_CRATE;
    }

    /**
     * Whether the path opens through the RO-Crate import: AUTO follows the GUI's
     * name-based routing (a .crate.zip file), CRATE forces it for .crate.zip files.
     * CRATE for any other file name is refused by the caller (the import expects a
     * RO-Crate container).
     */
    public static boolean routesToCrateImport(Path path, OpenFormat format) {
        boolean isCrateName = path.getFileName() != null
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".crate.zip");
        return switch (format) {
            case AUTO, CRATE -> isCrateName;
            case JIPX, LEGACY -> false;
        };
    }

    /**
     * The extract target of an automated crate import (the non-interactive branch of
     * {@code JIPipeDesktopProjectWindow.importROCrate}): a directory named after the
     * crate file inside the given base directory (the default projects directory for
     * the GUI's crate routing).
     */
    public static Path crateExtractTarget(Path cratePath, Path baseDirectory) {
        String fileName = cratePath.getFileName().toString();
        String baseName = fileName.substring(0, fileName.length() - 4);
        return uniqueSubDirectory(baseDirectory, baseName);
    }

    /**
     * A unique directory "name", "name-1", "name-2", ... inside the base directory
     * (the unique-name convention of the automated crate import).
     */
    public static Path uniqueSubDirectory(Path baseDirectory, String baseName) {
        String unique = StringUtils.makeUniqueString(baseName, "-", str -> Files.exists(baseDirectory.resolve(str)));
        return baseDirectory.resolve(unique);
    }

    /**
     * Resolves the project file of a JIPipe output folder (the openProject
     * convention): project.jip if it exists, else project.jipx, else project.jip
     * (which the load will then report as missing).
     */
    public static Path resolveOutputFolderProjectFile(Path directory) {
        Path projectPath = directory.resolve("project.jip");
        if (!Files.isRegularFile(projectPath)) {
            Path legacyProjectPath = projectPath;
            projectPath = directory.resolve("project.jipx");
            if (!Files.isRegularFile(projectPath)) {
                projectPath = legacyProjectPath;
            }
        }
        return projectPath;
    }

    /**
     * The data mode of a template load (the decision behind the interactive
     * "Load template" prompt).
     */
    public enum TemplateDataMode {
        /**
         * Extract the template's zipped data into a directory and open the extracted
         * project.
         */
        WITH_DATA,
        /**
         * Load only the pipeline from the template.
         */
        PIPELINE_ONLY,
        /**
         * Abort the load (the prompt's dismissal).
         */
        CANCELLED
    }

    /**
     * Decides how a template load proceeds. A template without zipped data always
     * loads the pipeline alone (the prompt never fires, the explicit choice has no
     * effect). A null choice (the prompt's dismissal) cancels.
     *
     * @param hasZipData whether the template carries zipped data
     * @param chosen     the explicit choice (the prompt answer or the automation
     *                   template-data value), null for a dismissal
     * @return the data mode the load proceeds with
     */
    public static TemplateDataMode decideTemplateDataMode(boolean hasZipData, TemplateDataMode chosen) {
        if (!hasZipData) {
            return TemplateDataMode.PIPELINE_ONLY;
        }
        return chosen != null ? chosen : TemplateDataMode.CANCELLED;
    }
}
