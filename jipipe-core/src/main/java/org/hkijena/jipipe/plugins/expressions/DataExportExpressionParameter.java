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

package org.hkijena.jipipe.plugins.expressions;

import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;
import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.plugins.expressions.variables.JIPipeTextAnnotationsExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.settings.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.utils.PathType;
import org.hkijena.jipipe.utils.StringUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An expression parameter designed for data export
 */
@JIPipeExpressionParameterSettings(hint = "per data item")
@AddJIPipeExpressionParameterVariable(fromClass = JIPipeTextAnnotationsExpressionParameterVariablesInfo.class)
@AddJIPipeExpressionParameterVariable(name = "Annotations", description = "Map of annotations", key = "annotations")
@AddJIPipeExpressionParameterVariable(name = "Data string", description = "String representation of the data (if available)", key = "data_string")
@AddJIPipeExpressionParameterVariable(name = "Data row", description = "Source row of the data (if available, otherwise -1)", key = "data_row")
@AddJIPipeExpressionParameterVariable(name = "Data directory", description = "The default data directory (run scratch dir or user-selected directory)", key = "data_dir")
@AddJIPipeExpressionParameterVariable(fromClass = JIPipeProjectDirectoriesVariablesInfo.class)
@AddJIPipeExpressionParameterVariable(name = "Automatically generated name", description = "A file name that was automatically generated based on annotations", key = "auto_file_name")
@AddJIPipeDocumentationDescription(description = "This function should return a valid file path string, which can be either hardcoded or built using string or <code>PATH_COMBINE</code> operations.\n\n" +
        "Hard-cording file paths: Just type the path into quotes as shown below " +
        "<pre>" +
        "\"C:/MyData/Outputs/Output_file\"" +
        "</pre>\n\n" +
        "Using path combination: You can combine combine variables provided from annotations and expression variables to, for example, save create a path relative to the project directory or to a project user directory" +
        "<pre>PATH_COMBINE(project_dir, \"Outputs\", \"Output_file\")</pre>" +
        "<pre>PATH_COMBINE(project_data_dirs [\"outputs\"], \"Outputs\", \"Output_file\")</pre>" +
        "Alternative: <pre>PATH_COMBINE(project_data_dir.outputs, \"Outputs\", \"Output_file\")</pre>\n\n" +
        "Using expression functions: You can also use the standard string concatenation function to generate paths" +
        "<pre>project_dir + \"/Outputs/Output_file\"</pre>")
public class DataExportExpressionParameter extends JIPipeExpressionParameter {
    public DataExportExpressionParameter() {
        super("PATH_COMBINE(data_dir, auto_file_name)");
    }

    public DataExportExpressionParameter(String expression) {
        super(expression);
    }

    public DataExportExpressionParameter(AbstractExpressionParameter other) {
        super(other);
    }

    public static DataExportExpressionParameter showPathChooser(Component parent, JIPipeWorkbench workbench, String title, PathType pathType, FileNameExtensionFilter... extensions) {
        Path path;
        switch (pathType) {
            case DirectoriesOnly:
                path = JIPipeFileChooserApplicationSettings.saveDirectory(parent, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, title);
                break;
            case FilesOnly:
                path = JIPipeFileChooserApplicationSettings.saveFile(parent, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, title, extensions);
                break;
            case FilesAndDirectories:
                path = JIPipeFileChooserApplicationSettings.savePath(parent, workbench, JIPipeFileChooserApplicationSettings.LastDirectoryKey.Data, title);
                break;
            default:
                throw new IllegalArgumentException("Unsupported path type: " + pathType);
        }

        if (path != null) {
            if (workbench instanceof JIPipeDesktopProjectWorkbench) {

                JIPipeProject project = workbench.getProject();
                path = path.toAbsolutePath();

                // Auto-replace project path
                Path projectPath = project.getWorkDirectory();
                if (projectPath != null) {
                    if (path.startsWith(projectPath)) {
                        if (JOptionPane.showConfirmDialog(parent, "The selected path '" + path + "' is located next to the project file.\n" +
                                        "Do you want to make the selected path portable for easier reproducibility?",
                                "Relative path detected",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                            return new DataExportExpressionParameter("PATH_COMBINE(project_dir, \"" + JIPipeExpressionEvaluator.escapeString(projectPath.relativize(path).toString().replace('\\', '/')) + "\")");
                        }
                    }
                }

                // Auto-replace data directory
                if (projectPath != null) {
                    for (Map.Entry<String, Path> entry : project.getMetadata().getDirectories().getDirectoryMap(projectPath).entrySet()) {
                        Path userPath = entry.getValue();
                        if (path.startsWith(userPath)) {
                            if (JOptionPane.showConfirmDialog(parent, "The selected path '" + path + "' is located in the user-defined directory '" + entry.getKey() + "'=" + entry.getValue() + ".\n" +
                                            "Do you want to make the selected path portable for easier reproducibility?",
                                    "Relative path detected",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                                if (JIPipeExpressionParameter.isValidVariableName(entry.getKey())) {
                                    return new DataExportExpressionParameter("PATH_COMBINE(project_data_dir." + entry.getKey() + ", \"" + JIPipeExpressionEvaluator.escapeString(userPath.relativize(path).toString().replace('\\', '/')) + "\")");
                                } else {
                                    return new DataExportExpressionParameter("PATH_COMBINE(project_data_dirs @ \"" + JIPipeExpressionEvaluator.escapeString(entry.getKey()) +
                                            "\", \"" + JIPipeExpressionEvaluator.escapeString(userPath.relativize(path).toString().replace('\\', '/')) + "\")");
                                }
                            }
                        }
                    }
                }
            }

            return new DataExportExpressionParameter("\"" + JIPipeExpressionEvaluator.escapeString(path.toAbsolutePath().toString().replace('\\', '/')) + "\"");
        } else {
            return null;
        }
    }

    public Path generatePath(Path dataDir, Path projectDir, Map<String, Path> projectDataDirs, String dataString, int dataRow, java.util.List<JIPipeTextAnnotation> annotationList) {
        if (StringUtils.isNullOrEmpty(projectDir)) {
            projectDir = dataDir;
        }
        JIPipeExpressionVariablesMap variables = new JIPipeExpressionVariablesMap();
        for (JIPipeTextAnnotation annotation : annotationList) {
            variables.set(annotation.getName(), annotation.getValue());
        }
        variables.set("annotations", JIPipeTextAnnotation.annotationListToMap(annotationList, JIPipeTextAnnotationMergeMode.OverwriteExisting));
        variables.set("data_string", dataString);
        variables.set("data_row", dataRow);
        variables.set("data_dir", dataDir.toAbsolutePath().toString());
        variables.set("project_dir", projectDir.toAbsolutePath().toString());
        Map<String, String> projectDataDirs_ = new HashMap<>();
        for (Map.Entry<String, Path> entry : projectDataDirs.entrySet()) {
            if (!StringUtils.isNullOrEmpty(entry.getKey())) {
                String value;
                if (entry.getValue().isAbsolute()) {
                    value = entry.getValue().toString();

                } else {
                    value = projectDir.resolve(entry.getValue()).toString();
                }

                variables.set("project_data_dir." + entry.getKey(), value);
                projectDataDirs_.put(entry.getKey(), value);
            }
        }
        variables.set("project_data_dirs", projectDataDirs_);
        String autoName = StringUtils.makeFilesystemCompatible(annotationList.stream().sorted(Comparator.comparing(JIPipeTextAnnotation::getName))
                .map(JIPipeTextAnnotation::getValue).collect(Collectors.joining("_")));
        variables.set("auto_file_name", autoName);
        return Paths.get(evaluateToString(variables));
    }
}
