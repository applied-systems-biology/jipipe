package org.hkijena.jipipe.extensions.python;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.api.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.pairs.StringQueryExpressionAndStringPairParameter;
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
    }

    @JIPipeDocumentation(name = "Environment type", description = "The kind of environment that should be executed. " +
            "Depending on the environment, you need to set the executable path to the Python executable, " +
            "to the Conda executable, or the environment directory (venv).")
    @JsonGetter("environment-type")
    @JIPipeParameter("environment-type")
    public PythonEnvironmentType getType() {
        return type;
    }

    @JIPipeParameter("environment-type")
    @JsonSetter("environment-type")
    public void setType(PythonEnvironmentType type) {
        this.type = type;
    }

    @JIPipeDocumentation(name = "Arguments", description = "Arguments passed to the Python/Conda executable (depending on the environment type). " +
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

    @JIPipeDocumentation(name = "Executable path", description = "Points to the main executable or directory used by Python. " +
            "For system environments, point it to the Python executable. For Conda environments, point it to the Conda executable. " +
            "For virtual environments, point it to the Python executable inside ")
    @JIPipeParameter("executable-path")
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

    @JIPipeDocumentation(name = "Environment variables", description = "These variables are provided to the Python executable. Existing environment " +
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

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (StringUtils.isNullOrEmpty(getExecutablePath()) || !Files.isRegularFile(getAbsoluteExecutablePath())) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context,
                    "Executable does not exist",
                    "You need to provide a Python executable",
                    "Provide a Python executable"));
        }
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
        return "Python environment {" +
                "Type=" + type +
                ", Arguments=" + arguments +
                ", Executable=" + executablePath +
                '}';
    }

    public static class PythonArgumentsVariablesInfo implements ExpressionParameterVariablesInfo {
        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> result = new HashSet<>();
            result.add(new ExpressionParameterVariable("Python executable", "The Python executable", "python_executable"));
            result.add(new ExpressionParameterVariable("Script file", "The Python script file to be executed", "script_file"));
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
