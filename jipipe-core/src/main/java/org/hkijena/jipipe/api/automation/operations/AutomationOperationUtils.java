package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.compartments.algorithms.JIPipeProjectCompartment;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.plugins.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionEvaluator;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeDynamicEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.Color;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared helpers for the built-in automation operations.
 * Public because {@code JIPipeAutomationAPI} (in the parent package) uses the
 * composite parameter value decoding for set_parameter.
 */
public final class AutomationOperationUtils {

    /**
     * Name of the export directory inside the system temp directory (allowed root and default
     * target of the file-writing operations).
     */
    private static final String EXPORT_DIRECTORY_NAME = "jipipe-mcp-export";

    private AutomationOperationUtils() {
    }

    /**
     * Resolves a compartment by UUID, alias, or name. Delegates to
     * {@link JIPipeAutomationAPI#findCompartment(JIPipeProject, String)}, the shared
     * resolution of the automation layer (well-formed UUID match, then alias, then
     * case-sensitive display name; a well-formed UUID that matches nothing resolves
     * to null). Called on the EDT by the window-touching operations, where the
     * window lookup is EDT-confined.
     */
    static JIPipeProjectCompartment findCompartment(JIPipeProject project, String compartmentRef) {
        return JIPipeAutomationAPI.findCompartment(project, compartmentRef);
    }

    /**
     * Finds the open project window whose session id equals the given project key (the
     * mapping the MCP registration announces: key = window session id). Returns null for a
     * null key or when no open window matches. Must run on the EDT
     * ({@link JIPipeDesktopProjectWindow#getOpenWindows()} is EDT-confined).
     */
    static JIPipeDesktopProjectWindow findProjectWindowByKey(String projectKey) {
        if (projectKey == null) {
            return null;
        }
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            if (window.getSessionId().toString().equals(projectKey)) {
                return window;
            }
        }
        return null;
    }

    /**
     * The default project window for desktop-touching operations: the window the dispatch
     * context is bound to (the MCP layer routes calls by project key, which is the window
     * session id), else the focused project window (via the keyboard focus owner's window
     * chain), else the first open one. Returns null if no project window is open.
     * Must run on the EDT.
     */
    static JIPipeDesktopProjectWindow resolveDefaultProjectWindow(AutomationContext ctx) {
        JIPipeDesktopProjectWindow bound = findProjectWindowByKey(ctx.getProjectId());
        if (bound != null) {
            return bound;
        }
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (activeWindow != null) {
            for (Window current = activeWindow; current != null; current = current.getOwner()) {
                if (current instanceof JIPipeDesktopProjectWindow projectWindow) {
                    return projectWindow;
                }
            }
        }
        for (JIPipeDesktopProjectWindow window : JIPipeDesktopProjectWindow.getOpenWindows()) {
            return window;
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

    /**
     * Pattern of the supported color encodings: #RRGGBB or #RRGGBBAA (alpha last, the project
     * convention), case-insensitive.
     */
    private static final Pattern COLOR_HEX_PATTERN = Pattern.compile("#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})");

    /**
     * Converts a raw string into an instance of a supported composite parameter type. Supported:
     * {@link Color} (encoding #RRGGBB or #RRGGBBAA hex, alpha last), expression parameters (the
     * {@link DefaultExpressionParameter} family: the expression string, syntax-checked),
     * {@link Quantity} (encoding '&lt;value&gt; &lt;unit&gt;', the unit optional), and dynamic enum
     * parameters (the {@link JIPipeDynamicEnumParameter} family: the registered item string).
     * Returns null when the type is not supported: the caller then keeps the raw string
     * (the pre-existing behavior for unsupported leaf types).
     *
     * @param value      the raw string value
     * @param fieldClass the declared class of the parameter field
     * @return the decoded object, or null when the type is not supported
     * @throws IllegalArgumentException when the type is supported but the value cannot be decoded
     */
    public static Object parseCompositeValue(String value, Class<?> fieldClass) {
        if (fieldClass == null) {
            return null;
        }
        if (fieldClass == Color.class) {
            return parseColorHex(value);
        }
        if (DefaultExpressionParameter.class.isAssignableFrom(fieldClass)) {
            return parseExpressionParameter(value, fieldClass);
        }
        if (fieldClass == Quantity.class) {
            Quantity quantity = Quantity.parse(value);
            if (quantity == null) {
                throw new IllegalArgumentException("Invalid quantity '" + value
                        + "'. Expected encoding: '<value> <unit>' where the unit is optional, for example '5 px' or '2.5 mm'.");
            }
            return quantity;
        }
        if (JIPipeDynamicEnumParameter.class.isAssignableFrom(fieldClass)) {
            return parseDynamicEnumParameter(value, fieldClass);
        }
        return null;
    }

    /**
     * Parses a color from the #RRGGBB or #RRGGBBAA encoding: alpha, when present, is the last
     * byte (the project convention, see {@link ColorUtils#colorToHexString(Color)}, the writer
     * the color parameter editor displays). The 6-digit form is opaque. Case-insensitive.
     * The strict pattern check runs first so that a malformed value always produces this
     * method's error message naming the accepted encoding. The decode is local instead of a
     * delegation to {@link ColorUtils#hexStringToColor(String)} because that parser reads the
     * alpha pair as a decimal number (Integer.parseInt without a radix), so it neither
     * round-trips colorToHexString (an alpha of 128, written as hex "80", would read back
     * as decimal 80) nor accepts alpha bytes with hex letters.
     */
    private static Color parseColorHex(String value) {
        Matcher matcher = COLOR_HEX_PATTERN.matcher(value == null ? "" : value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid color '" + value
                    + "'. Expected encoding: '#RRGGBB' or '#RRGGBBAA' (alpha last) hex, for example '#FF8000' or '#FF800080'.");
        }
        String hex = matcher.group(1);
        int decoded = Integer.parseUnsignedInt(hex, 16);
        if (hex.length() == 8) {
            return new Color((decoded >>> 24) & 0xFF, (decoded >>> 16) & 0xFF, (decoded >>> 8) & 0xFF, decoded & 0xFF);
        }
        return new Color(decoded);
    }

    /**
     * Creates an instance of an expression parameter class (a {@link DefaultExpressionParameter}
     * subclass) from the expression string. The string is syntax-checked with the evaluator of
     * the parameter class (the same check the expression editor UI applies) before it is stored.
     * Returns null when the class has no usable standard constructor (the caller keeps the raw
     * string, the pre-existing behavior).
     */
    private static Object parseExpressionParameter(String value, Class<?> fieldClass) {
        DefaultExpressionParameter parameter;
        try {
            var constructor = fieldClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            parameter = (DefaultExpressionParameter) constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            return null;
        }
        if (parameter.getEvaluator() instanceof JIPipeExpressionEvaluator evaluator) {
            Exception syntaxError = evaluator.checkSyntax(value == null ? "" : value);
            if (syntaxError != null) {
                throw new IllegalArgumentException("Invalid expression '" + value + "': " + syntaxError.getMessage()
                        + ". Expected encoding: a valid JIPipe expression string.");
            }
        }
        parameter.setExpression(value);
        return parameter;
    }

    /**
     * Creates an instance of a dynamic enum parameter class (a {@link JIPipeDynamicEnumParameter}
     * subclass) and sets its value from the registered item string (the same write the JSON
     * deserializers of these classes perform). When the fresh instance reports registered items
     * (some classes populate them in their constructor) and all items are strings, the value must
     * be one of them. Returns null when the class has no usable standard constructor (the caller
     * keeps the raw string, the pre-existing behavior).
     */
    @SuppressWarnings("unchecked")
    private static Object parseDynamicEnumParameter(String value, Class<?> fieldClass) {
        JIPipeDynamicEnumParameter<?> parameter;
        try {
            var constructor = fieldClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            parameter = (JIPipeDynamicEnumParameter<?>) constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            return null;
        }
        List<?> allowedValues = parameter.getAllowedValues();
        if (!allowedValues.isEmpty() && allowedValues.stream().allMatch(String.class::isInstance)
                && !allowedValues.contains(value)) {
            throw new IllegalArgumentException("Invalid dynamic enum value '" + value
                    + "'. Expected encoding: one of the registered items: " + String.join(", ",
                    allowedValues.stream().map(item -> (String) item).toList()));
        }
        ((JIPipeDynamicEnumParameter<Object>) parameter).setValue(value);
        return parameter;
    }
}
