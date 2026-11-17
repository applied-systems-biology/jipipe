package org.hkijena.jipipe.api.automation.operations;

import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.project.JIPipeProject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Shared helpers for the built-in automation operations.
 */
final class AutomationOperationUtils {

    /**
     * Name of the export directory inside the system temp directory (allowed root and default
     * target of the file-writing operations).
     */
    private static final String EXPORT_DIRECTORY_NAME = "jipipe-mcp-export";

    private AutomationOperationUtils() {
    }

    /**
     * Resolves a compartment by UUID, alias, or name.
     * {@link JIPipeProject#findCompartment(String)} matches UUIDs and aliases: this helper
     * additionally falls back to the display name.
     */
    static JIPipeProjectCompartment findCompartment(JIPipeProject project, String compartmentRef) {
        var resolved = project.findCompartment(compartmentRef);
        if (resolved != null) {
            return resolved;
        }
        for (var entry : project.getCompartments().entrySet()) {
            if (entry.getValue().getName() != null && entry.getValue().getName().equals(compartmentRef)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Returns the allowed file system roots for operations that write to disk (export_data,
     * the screenshot save-to targets): the jipipe-mcp-export subdirectory of the system temp
     * directory and the project work directory if set. The project temporary base directory is
     * deliberately not a root: JIPipeProject.getTemporaryBaseDirectory() is not stable across
     * calls (it mints a fresh temporary tree per invocation), so it cannot anchor a
     * reproducible guard or default target.
     */
    static List<Path> allowedRoots(JIPipeProject project) {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();
        roots.add(systemExportRoot());
        if (project != null && project.getWorkDirectory() != null) {
            roots.add(project.getWorkDirectory().toAbsolutePath().normalize());
        }
        return new ArrayList<>(roots);
    }

    /**
     * The jipipe-mcp-export subdirectory of the system temp directory (default export target
     * and always-present allowed root).
     */
    static Path systemExportRoot() {
        return Path.of(System.getProperty("java.io.tmpdir"), EXPORT_DIRECTORY_NAME).toAbsolutePath().normalize();
    }

    /**
     * Resolution outcome of the write-target guard. The path is null if the target was refused.
     */
    record TargetPath(Path path, String errorMessage) {
    }

    /**
     * Resolves a user-provided write target (a file or directory path) against the allowed
     * roots. Relative paths are resolved against the process working directory. Refuses paths
     * outside all allowed roots; '..' traversal is dead because all paths are normalized
     * before the component-wise startsWith check. The refusal message names the parameter and
     * lists the allowed roots.
     *
     * @param project       the project (its work directory extends the roots; may be null)
     * @param parameterName the parameter name used in the refusal message (e.g. "target-dir")
     * @param target        the non-blank target path text
     * @return the resolved path, or a TargetPath with a null path and the refusal message
     */
    static TargetPath resolveWriteTarget(JIPipeProject project, String parameterName, String target) {
        List<Path> roots = allowedRoots(project);
        Path normalized = Path.of(target).toAbsolutePath().normalize();
        for (Path root : roots) {
            if (normalized.startsWith(root)) {
                return new TargetPath(normalized, null);
            }
        }
        List<String> rootNames = roots.stream().map(Path::toString).toList();
        return new TargetPath(null, parameterName + " '" + normalized
                + "' is not inside an allowed export root. Allowed roots: " + String.join(", ", rootNames));
    }
}
