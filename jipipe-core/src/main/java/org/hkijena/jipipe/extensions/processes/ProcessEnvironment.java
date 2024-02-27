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

package org.hkijena.jipipe.extensions.processes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.extensions.expressions.*;
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

public class ProcessEnvironment extends JIPipeEnvironment {

    private JIPipeExpressionParameter arguments = new JIPipeExpressionParameter("ARRAY()");
    private Path executablePathWindows = Paths.get("");
    private Path executablePathLinux = Paths.get("");
    private Path executablePathOSX = Paths.get("");
    private JIPipeExpressionParameter workDirectory = new JIPipeExpressionParameter("executable_dir");
    private StringQueryExpressionAndStringPairParameter.List environmentVariables = new StringQueryExpressionAndStringPairParameter.List();

    public ProcessEnvironment() {

    }

    public ProcessEnvironment(ProcessEnvironment other) {
        super(other);
        this.arguments = new JIPipeExpressionParameter(other.arguments);
        this.executablePathWindows = other.executablePathWindows;
        this.executablePathLinux = other.executablePathLinux;
        this.executablePathOSX = other.executablePathOSX;
        this.environmentVariables = new StringQueryExpressionAndStringPairParameter.List(other.environmentVariables);
        this.workDirectory = new JIPipeExpressionParameter(other.workDirectory);
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

    @SetJIPipeDocumentation(name = "Arguments", description = "Arguments passed to the process.")
    @JIPipeParameter("arguments")
    @JIPipeExpressionParameterVariable(key = "executable", name = "Executable path", description = "The path to the executable")
    @JIPipeExpressionParameterVariable(key = "executable_dir", name = "Executable containing directory", description = "The path to the directory that contains the executable")
    @JsonGetter("arguments")
    public JIPipeExpressionParameter getArguments() {
        return arguments;
    }

    @JsonSetter("arguments")
    @JIPipeParameter("arguments")
    public void setArguments(JIPipeExpressionParameter arguments) {
        this.arguments = arguments;
    }

    @SetJIPipeDocumentation(name = "Executable path (Windows)", description = "The executable path if executed on Windows.")
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

    @SetJIPipeDocumentation(name = "Executable path (Linux)", description = "The executable path if executed on Linux.")
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

    @SetJIPipeDocumentation(name = "Executable path (OSX)", description = "The executable path if executed on Mac OSX.")
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

    @SetJIPipeDocumentation(name = "Work directory", description = "The work directory of the process")
    @JIPipeParameter("work-directory")
    @JsonGetter("work-directory")
    @JIPipeExpressionParameterVariable(key = "executable", name = "Executable path", description = "The path to the executable")
    @JIPipeExpressionParameterVariable(key = "executable_dir", name = "Executable containing directory", description = "The path to the directory that contains the executable")
    public JIPipeExpressionParameter getWorkDirectory() {
        return workDirectory;
    }

    @JIPipeParameter("work-directory")
    @JsonSetter("work-directory")
    public void setWorkDirectory(JIPipeExpressionParameter workDirectory) {
        this.workDirectory = workDirectory;
    }

    public Path getAbsoluteExecutablePath() {
        if (SystemUtils.IS_OS_WINDOWS) {
            if (StringUtils.isNullOrEmpty(getExecutablePathWindows()))
                return Paths.get("");
            return PathUtils.relativeJIPipeUserDirToAbsolute(getExecutablePathWindows());
        } else if (SystemUtils.IS_OS_LINUX) {
            if (StringUtils.isNullOrEmpty(getExecutablePathLinux()))
                return Paths.get("");
            return PathUtils.relativeJIPipeUserDirToAbsolute(getExecutablePathLinux());
        } else if (SystemUtils.IS_OS_MAC_OSX) {
            if (StringUtils.isNullOrEmpty(getExecutablePathOSX()))
                return Paths.get("");
            return PathUtils.relativeJIPipeUserDirToAbsolute(getExecutablePathOSX());
        } else {
            System.err.println("Operating system not detected.");
            if (StringUtils.isNullOrEmpty(getExecutablePathWindows()))
                return Paths.get("");
            return PathUtils.relativeJIPipeUserDirToAbsolute(getExecutablePathWindows());
        }
    }

    @SetJIPipeDocumentation(name = "Environment variables", description = "These variables are provided to the Python executable. Existing environment " +
            "variables are available as variables")
    @JIPipeParameter("environment-variables")
    @PairParameterSettings(keyLabel = "Value", valueLabel = "Key")
    @JIPipeExpressionParameterVariable(fromClass = EnvironmentVariablesSource.class)
    public StringQueryExpressionAndStringPairParameter.List getEnvironmentVariables() {
        return environmentVariables;
    }

    @JIPipeParameter("environment-variables")
    public void setEnvironmentVariables(StringQueryExpressionAndStringPairParameter.List environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (StringUtils.isNullOrEmpty(getAbsoluteExecutablePath()) || !Files.isRegularFile(getAbsoluteExecutablePath())) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, reportContext,
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
                ", WorkDirectory=" + workDirectory +
                '}';
    }

    public static class VariablesInfo implements ExpressionParameterVariablesInfo {
        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
            result.add(new JIPipeExpressionParameterVariableInfo("executable", "Executable", "The executable"));
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
