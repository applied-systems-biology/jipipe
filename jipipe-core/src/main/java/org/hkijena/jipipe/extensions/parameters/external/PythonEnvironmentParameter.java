package org.hkijena.jipipe.extensions.parameters.external;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterVariableSource;
import org.hkijena.jipipe.extensions.parameters.external.installers.SelectCondaEnvPythonInstaller;
import org.hkijena.jipipe.extensions.parameters.external.installers.SelectSystemPythonInstaller;
import org.hkijena.jipipe.extensions.parameters.external.installers.SelectVirtualEnvPythonInstaller;
import org.hkijena.jipipe.extensions.parameters.pairs.PairParameterSettings;
import org.hkijena.jipipe.extensions.parameters.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.extensions.parameters.pairs.StringQueryExpressionAndStringPairParameter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Parameter that describes a Python environment
 */
@PythonEnvironmentSettings(installers = {SelectSystemPythonInstaller.class, SelectVirtualEnvPythonInstaller.class, SelectCondaEnvPythonInstaller.class})
public class PythonEnvironmentParameter implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private PythonEnvironmentType type = PythonEnvironmentType.System;
    private DefaultExpressionParameter arguments = new DefaultExpressionParameter();
    private Path executablePath = Paths.get("");
    private StringQueryExpressionAndStringPairParameter.List environmentVariables = new StringQueryExpressionAndStringPairParameter.List();

    public PythonEnvironmentParameter() {

    }

    public PythonEnvironmentParameter(PythonEnvironmentType type, DefaultExpressionParameter arguments, Path executablePath) {
        this.type = type;
        this.arguments = arguments;
        this.executablePath = executablePath;
    }

    public PythonEnvironmentParameter(PythonEnvironmentParameter other) {
        this.type = other.type;
        this.arguments = other.arguments;
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

    @JIPipeDocumentation(name ="Arguments", description = "Arguments passed to the Python/Conda executable (depending on the environment type). " +
            "This expression must return an array. You have two variables 'script_file' and 'python_executable'. 'script_file' is always " +
            "replaced by the Python script that is currently executed.")
    @JIPipeParameter("arguments")
    @ExpressionParameterSettings(variableSource = PythonArgumentsVariableSource.class)
    @JsonGetter("arguments")
    public DefaultExpressionParameter getArguments() {
        return arguments;
    }

    @JsonSetter("arguments")
    @JIPipeParameter("arguments")
    public void setArguments(DefaultExpressionParameter arguments) {
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

    @JIPipeDocumentation(name = "Environment variables", description = "These variables are provided to the Python executable. Existing environment " +
            "variables are available as variables")
    @JIPipeParameter("environment-variables")
    @PairParameterSettings(keyLabel = "Value", valueLabel = "Key")
    @ExpressionParameterSettings(variableSource = PythonEnvironmentVariablesSource.class)
    public StringQueryExpressionAndStringPairParameter.List getEnvironmentVariables() {
        return environmentVariables;
    }

    @JIPipeParameter("environment-variables")
    public void setEnvironmentVariables(StringQueryExpressionAndStringPairParameter.List environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public static class PythonArgumentsVariableSource implements ExpressionParameterVariableSource {
        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> result = new HashSet<>();
            result.add(new ExpressionParameterVariable("Python executable", "The Python executable", "python_executable"));
            result.add(new ExpressionParameterVariable("Script file", "The Python script file to be executed", "script_file"));
            return result;
        }
    }

    public static class PythonEnvironmentVariablesSource implements ExpressionParameterVariableSource {
        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> result = new HashSet<>();
            for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
                result.add(new ExpressionParameterVariable(entry.getKey(), entry.getValue(), entry.getKey()));
            }
            return result;
        }
    }

    @Override
    public String toString() {
        return "Python environment {" +
                "Type=" + type +
                ", Arguments=" + arguments +
                ", Executable=" + executablePath +
                '}';
    }
}
