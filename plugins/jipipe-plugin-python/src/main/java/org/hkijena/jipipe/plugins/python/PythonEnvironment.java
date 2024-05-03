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

package org.hkijena.jipipe.plugins.python;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeArtifactQueryParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
import org.hkijena.jipipe.utils.EnvironmentVariablesSource;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Parameter that describes a Python environment
 */
public class PythonEnvironment extends JIPipeEnvironment {

    public static final String ENVIRONMENT_ID = "python";

    private PythonEnvironmentType type = PythonEnvironmentType.System;
    private JIPipeExpressionParameter arguments = new JIPipeExpressionParameter("ARRAY(script_file)");
    private Path executablePath = Paths.get("");
    private StringQueryExpressionAndStringPairParameter.List environmentVariables = new StringQueryExpressionAndStringPairParameter.List();
    private JIPipeArtifactQueryParameter artifactQuery = new JIPipeArtifactQueryParameter();

    public PythonEnvironment() {

    }

    public PythonEnvironment(PythonEnvironmentType type, JIPipeExpressionParameter arguments, Path executablePath) {
        this.type = type;
        this.arguments = arguments;
        this.executablePath = executablePath;
    }

    public PythonEnvironment(PythonEnvironment other) {
        super(other);
        this.type = other.type;
        this.arguments = new JIPipeExpressionParameter(other.arguments);
        this.executablePath = other.executablePath;
        this.environmentVariables = new StringQueryExpressionAndStringPairParameter.List(other.environmentVariables);
        this.artifactQuery = new JIPipeArtifactQueryParameter(other.artifactQuery);
    }

    @SetJIPipeDocumentation(name = "Environment type", description = "The kind of environment that should be executed. " +
            "Depending on the environment, you need to set the executable path to the Python executable, " +
            "to the Conda executable, or the environment directory (venv).")
    @JsonGetter("environment-type")
    @JIPipeParameter(value = "environment-type", important = true, uiOrder = -100)
    public PythonEnvironmentType getType() {
        return type;
    }

    @JIPipeParameter("environment-type")
    @JsonSetter("environment-type")
    public void setType(PythonEnvironmentType type) {
        this.type = type;
        emitParameterUIChangedEvent();
    }

    @SetJIPipeDocumentation(name = "Arguments", description = "Arguments passed to the Python/Conda executable (depending on the environment type). " +
            "This expression must return an array. You have two variables 'script_file' and 'python_executable'. 'script_file' is always " +
            "replaced by the Python script that is currently executed.")
    @JIPipeParameter("arguments")
    @JIPipeExpressionParameterSettings(variableSource = PythonArgumentsVariablesInfo.class)
    @JsonGetter("arguments")
    public JIPipeExpressionParameter getArguments() {
        return arguments;
    }

    @JsonSetter("arguments")
    @JIPipeParameter("arguments")
    public void setArguments(JIPipeExpressionParameter arguments) {
        this.arguments = arguments;
    }

    @SetJIPipeDocumentation(name = "Executable path", description = "Points to the main executable or directory used by Python. " +
            "For system environments, point it to the Python executable. For Conda environments, point it to the Conda executable. " +
            "For virtual environments, point it to the Python executable inside ")
    @JIPipeParameter(value = "executable-path", uiOrder = -90, important = true)
    @JsonGetter("executable-path")
    public Path getExecutablePath() {
        return executablePath;
    }

    @JIPipeParameter("executable-path")
    @JsonSetter("executable-path")
    public void setExecutablePath(Path executablePath) {
        this.executablePath = executablePath;
    }

    public Path getAbsoluteExecutablePath() {
        return PathUtils.relativeJIPipeUserDirToAbsolute(getExecutablePath());
    }

    @SetJIPipeDocumentation(name = "Environment variables", description = "These variables are provided to the Python executable. Existing environment " +
            "variables are available as variables")
    @JIPipeParameter("environment-variables")
    @PairParameterSettings(keyLabel = "Value", valueLabel = "Key")
    @JIPipeExpressionParameterSettings(variableSource = EnvironmentVariablesSource.class)
    public StringQueryExpressionAndStringPairParameter.List getEnvironmentVariables() {
        return environmentVariables;
    }

    @JIPipeParameter("environment-variables")
    public void setEnvironmentVariables(StringQueryExpressionAndStringPairParameter.List environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @SetJIPipeDocumentation(name = "Artifact", description = "Determines the artifact that contains the Python executable")
    @JIPipeParameter(value = "artifact", uiOrder = -90, important = true)
    public JIPipeArtifactQueryParameter getArtifactQuery() {
        return artifactQuery;
    }

    @JIPipeParameter("artifact")
    public void setArtifactQuery(JIPipeArtifactQueryParameter artifactQuery) {
        this.artifactQuery = artifactQuery;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if(type != PythonEnvironmentType.Artifact) {
            if (StringUtils.isNullOrEmpty(getExecutablePath()) || !Files.isRegularFile(getAbsoluteExecutablePath())) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, reportContext,
                        "Executable does not exist",
                        "You need to provide a Python executable",
                        "Provide a Python executable"));
            }
        }
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if(type == PythonEnvironmentType.Artifact) {
            if("executable-path".equals(access.getKey())) {
                return false;
            }
        }
        else {
            if("artifact".equals(access.getKey())) {
                return false;
            }
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/python.png");
    }

    @Override
    public String getInfo() {
        return StringUtils.orElse(getExecutablePath(), "<Not set>");
    }

    @Override
    public String toString() {
        return "PythonEnvironment{" +
                "type=" + type +
                ", arguments=" + arguments +
                ", executablePath=" + executablePath +
                ", environmentVariables=" + environmentVariables +
                ", artifactQuery=" + artifactQuery +
                '}';
    }

    public static class PythonArgumentsVariablesInfo implements ExpressionParameterVariablesInfo {
        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
            result.add(new JIPipeExpressionParameterVariableInfo("python_executable", "Python executable", "The Python executable"));
            result.add(new JIPipeExpressionParameterVariableInfo("script_file", "Script file", "The Python script file to be executed"));
            return result;
        }
    }

    /**
     * A list of {@link PythonEnvironment}
     */
    public static class List extends ListParameter<PythonEnvironment> {
        public List() {
            super(PythonEnvironment.class);
        }

        public List(List other) {
            super(PythonEnvironment.class);
            for (PythonEnvironment environment : other) {
                add(new PythonEnvironment(environment));
            }
        }
    }
}
