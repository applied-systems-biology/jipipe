package org.hkijena.jipipe.extensions.expressions;

import org.hkijena.jipipe.api.annotation.JIPipeAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.expressions.variables.TextAnnotationsExpressionParameterVariableSource;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An expression parameter designed for data export
 */
@ExpressionParameterSettings(hint = "per data item")
@ExpressionParameterSettingsVariable(fromClass = TextAnnotationsExpressionParameterVariableSource.class)
@ExpressionParameterSettingsVariable(name = "Annotations", description = "Map of annotations", key = "annotations")
@ExpressionParameterSettingsVariable(name = "Data string", description = "String representation of the data (if available)", key = "data_string")
@ExpressionParameterSettingsVariable(name = "Data row", description = "Source row of the data (if available, otherwise -1)", key = "data_row")
@ExpressionParameterSettingsVariable(name = "Data directory", description = "The default data directory (run scratch dir or user-selected directory)", key = "data_dir")
@ExpressionParameterSettingsVariable(name = "Project directory", description = "The project directory (if available; will be the same as the data directory otherwise)", key = "project_dir")
@ExpressionParameterSettingsVariable(name = "Project data directories", description = "The user-configured project data directories as map. Access entries by the key.", key = "project_data_dirs")
@ExpressionParameterSettingsVariable(name = "Automatically generated name", description = "A file name that was automatically generated based on annotations", key = "auto_file_name")
public class DataExportExpressionParameter extends DefaultExpressionParameter {
    public DataExportExpressionParameter() {
        super("PATH_COMBINE(data_dir, auto_file_name)");
    }

    public DataExportExpressionParameter(String expression) {
        super(expression);
    }

    public DataExportExpressionParameter(AbstractExpressionParameter other) {
        super(other);
    }

    public Path generatePath(Path dataDir, Path projectDir, Map<String, Path> projectDataDirs, String dataString, int dataRow, java.util.List<JIPipeTextAnnotation> annotationList) {
        if(StringUtils.isNullOrEmpty(projectDir)) {
            projectDir = dataDir;
        }
        ExpressionVariables variables = new ExpressionVariables();
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
            if(!StringUtils.isNullOrEmpty(entry.getKey())) {
                if(entry.getValue().isAbsolute()) {
                    projectDataDirs_.put(entry.getKey(), entry.getValue().toString());
                }
                else {
                    projectDataDirs_.put(entry.getKey(), projectDir.resolve(entry.getValue()).toString());
                }
            }
        }
        variables.set("project_data_dirs", projectDataDirs_);
        String autoName = StringUtils.makeFilesystemCompatible(annotationList.stream().sorted(Comparator.comparing(JIPipeTextAnnotation::getName))
                .map(JIPipeTextAnnotation::getValue).collect(Collectors.joining("_")));
        variables.set("auto_file_name", autoName);
        return Paths.get(evaluateToString(variables));
    }
}
