package org.hkijena.jipipe.extensions.processes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeExternalEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariable;
import org.hkijena.jipipe.extensions.expressions.ExpressionParameterVariableSource;
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

public class ProcessEnvironment extends JIPipeExternalEnvironment {

    private DefaultExpressionParameter arguments = new DefaultExpressionParameter("ARRAY()");
    private Path executablePathWindows = Paths.get("");
    private Path executablePathLinux = Paths.get("");
    private Path executablePathOSX = Paths.get("");
    private StringQueryExpressionAndStringPairParameter.List environmentVariables = new StringQueryExpressionAndStringPairParameter.List();

    public ProcessEnvironment() {

    }

    public ProcessEnvironment(ProcessEnvironment other) {
        super(other);
        this.arguments = new DefaultExpressionParameter(other.arguments);
        this.executablePathWindows = other.executablePathWindows;
        this.executablePathLinux = other.executablePathLinux;
        this.executablePathOSX = other.executablePathOSX;
        this.environmentVariables = new StringQueryExpressionAndStringPairParameter.List(other.environmentVariables);
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/utilities-terminal.png");
    }

    @Override
    public String getInfo() {
        if (SystemUtils.IS_OS_WINDOWS)
            return StringUtils.orElse(getExecutablePathWindows(), "<Not set>");
        else if (SystemUtils.IS_OS_LINUX)
            return StringUtils.orElse(getExecutablePathLinux(), "<Not set>");
        else if (SystemUtils.IS_OS_MAC_OSX)
            return StringUtils.orElse(getExecutablePathOSX(), "<Not set>");
        else {
            return "<Operating system not detected>";
        }
    }

    @JIPipeDocumentation(name = "Arguments", description = "Arguments passed to the process.")
    @JIPipeParameter("arguments")
    @ExpressionParameterSettings(variableSource = VariableSource.class)
    @JsonGetter("arguments")
    public DefaultExpressionParameter getArguments() {
        return arguments;
    }

    @JsonSetter("arguments")
    @JIPipeParameter("arguments")
    public void setArguments(DefaultExpressionParameter arguments) {
        this.arguments = arguments;
    }

    @JIPipeDocumentation(name = "Executable path (Windows)", description = "The executable path if executed on Windows.")
    @JIPipeParameter("executable-path-windows")
    @JsonGetter("executable-path-windows")
    public Path getExecutablePathWindows() {
        return executablePathWindows;
    }

    @JIPipeParameter("executable-path-windows")
    @JsonGetter("executable-path-windows")
    public void setExecutablePathWindows(Path executablePathWindows) {
        this.executablePathWindows = executablePathWindows;
    }

    @JIPipeDocumentation(name = "Executable path (Linux)", description = "The executable path if executed on Linux.")
    @JIPipeParameter("executable-path-linux")
    @JsonGetter("executable-path-linux")
    public Path getExecutablePathLinux() {
        return executablePathLinux;
    }

    @JIPipeParameter("executable-path-linux")
    @JsonGetter("executable-path-linux")
    public void setExecutablePathLinux(Path executablePathLinux) {
        this.executablePathLinux = executablePathLinux;
    }

    @JIPipeDocumentation(name = "Executable path (OSX)", description = "The executable path if executed on Mac OSX.")
    @JIPipeParameter("executable-path-osx")
    @JsonGetter("executable-path-osx")
    public Path getExecutablePathOSX() {
        return executablePathOSX;
    }

    @JIPipeParameter("executable-path-osx")
    @JsonGetter("executable-path-osx")
    public void setExecutablePathOSX(Path executablePathOSX) {
        this.executablePathOSX = executablePathOSX;
    }

    public Path getAbsoluteExecutablePath() {
        if (SystemUtils.IS_OS_WINDOWS) {
            if (StringUtils.isNullOrEmpty(getExecutablePathWindows()))
                return Paths.get("");
            return PathUtils.relativeToImageJToAbsolute(getExecutablePathWindows());
        } else if (SystemUtils.IS_OS_LINUX) {
            if (StringUtils.isNullOrEmpty(getExecutablePathLinux()))
                return Paths.get("");
            return PathUtils.relativeToImageJToAbsolute(getExecutablePathLinux());
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            if (StringUtils.isNullOrEmpty(getExecutablePathOSX()))
                return Paths.get("");
            return PathUtils.relativeToImageJToAbsolute(getExecutablePathOSX());
        } else {
            System.err.println("Operating system not detected.");
            if (StringUtils.isNullOrEmpty(getExecutablePathWindows()))
                return Paths.get("");
            return PathUtils.relativeToImageJToAbsolute(getExecutablePathWindows());
        }
    }

    @JIPipeDocumentation(name = "Environment variables", description = "These variables are provided to the Python executable. Existing environment " +
            "variables are available as variables")
    @JIPipeParameter("environment-variables")
    @PairParameterSettings(keyLabel = "Value", valueLabel = "Key")
    @ExpressionParameterSettings(variableSource = EnvironmentVariablesSource.class)
    public StringQueryExpressionAndStringPairParameter.List getEnvironmentVariables() {
        return environmentVariables;
    }

    @JIPipeParameter("environment-variables")
    public void setEnvironmentVariables(StringQueryExpressionAndStringPairParameter.List environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (StringUtils.isNullOrEmpty(getAbsoluteExecutablePath()) || !Files.isRegularFile(getAbsoluteExecutablePath())) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context,
                    "Executable does not exist",
                    "You need to provide a Python executable",
                    "Provide an executable"));
        }
    }

    @Override
    public String toString() {
        return "Process environment {" +
                "Arguments=" + arguments +
                ", Executable=" + executablePathWindows +
                '}';
    }

    public static class VariableSource implements ExpressionParameterVariableSource {
        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> result = new HashSet<>();
            result.add(new ExpressionParameterVariable("Executable", "The executable", "executable"));
            return result;
        }
    }

    /**
     * A list of {@link ProcessEnvironment}
     */
    public static class List extends ListParameter<ProcessEnvironment> {
        public List() {
            super(ProcessEnvironment.class);
        }

        public List(ProcessEnvironment.List other) {
            super(ProcessEnvironment.class);
            for (ProcessEnvironment environment : other) {
                add(new ProcessEnvironment(environment));
            }
        }
    }
}
