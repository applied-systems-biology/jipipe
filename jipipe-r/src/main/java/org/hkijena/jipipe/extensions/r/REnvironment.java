package org.hkijena.jipipe.extensions.r;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
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

public class REnvironment extends JIPipeEnvironment {
    public static final String ENVIRONMENT_ID = "r";
    private Path RExecutablePath = Paths.get("");
    private Path RScriptExecutablePath = Paths.get("");
    private DefaultExpressionParameter arguments = new DefaultExpressionParameter("ARRAY(script_file)");
    private StringQueryExpressionAndStringPairParameter.List environmentVariables = new StringQueryExpressionAndStringPairParameter.List();

    public REnvironment() {
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC_OSX) {
            RExecutablePath = PathUtils.orElse(Paths.get("/usr/local/bin/R"), Paths.get("/usr/bin/R"));
            RScriptExecutablePath = PathUtils.orElse(Paths.get("/usr/local/bin/Rscript"), Paths.get("/usr/bin/Rscript"));
        }
    }

    public REnvironment(REnvironment other) {
        super(other);
        this.RExecutablePath = other.RExecutablePath;
        this.RScriptExecutablePath = other.RScriptExecutablePath;
        this.arguments = new DefaultExpressionParameter(other.arguments);
        this.environmentVariables = new StringQueryExpressionAndStringPairParameter.List(other.environmentVariables);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        if (StringUtils.isNullOrEmpty(getRExecutablePath()) || !Files.isRegularFile(getRExecutablePath())) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new UnspecifiedValidationReportContext(),
                    "R executable does not exist",
                    "You need to provide a R executable",
                    "Provide a R executable"));
        }
        if (StringUtils.isNullOrEmpty(getRScriptExecutablePath()) || !Files.isRegularFile(getRScriptExecutablePath())) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new UnspecifiedValidationReportContext(),
                    "RScript executable does not exist",
                    "You need to provide a RScript executable",
                    "Provide a RScript executable"));
        }
    }

    @JIPipeDocumentation(name = "R executable", description = "The main R executable (R.exe on Windows)")
    @JIPipeParameter("r-executable-path")
    @JsonGetter("r-executable-path")
    public Path getRExecutablePath() {
        return RExecutablePath;
    }

    @JIPipeParameter("r-executable-path")
    @JsonSetter("r-executable-path")
    public void setRExecutablePath(Path RExecutablePath) {
        this.RExecutablePath = RExecutablePath;
    }

    @JIPipeDocumentation(name = "RScript executable", description = "The RScript executable (RScript.exe on Windows)")
    @JIPipeParameter("rscript-executable-path")
    @JsonGetter("rscript-executable-path")
    public Path getRScriptExecutablePath() {
        return RScriptExecutablePath;
    }

    @JIPipeParameter("rscript-executable-path")
    @JsonSetter("rscript-executable-path")
    public void setRScriptExecutablePath(Path RScriptExecutablePath) {
        this.RScriptExecutablePath = RScriptExecutablePath;
    }

    @JIPipeDocumentation(name = "Arguments", description = "Arguments passed to the Python/Conda executable (depending on the environment type). " +
            "This expression must return an array. You have two variables 'script_file' and 'r_executable'. 'script_file' is always " +
            "replaced by the Python script that is currently executed.")
    @JIPipeParameter("arguments")
    @ExpressionParameterSettings(variableSource = RArgumentsVariableSource.class)
    @JsonGetter("arguments")
    public DefaultExpressionParameter getArguments() {
        return arguments;
    }

    @JsonSetter("arguments")
    @JIPipeParameter("arguments")
    public void setArguments(DefaultExpressionParameter arguments) {
        this.arguments = arguments;
    }

    @JIPipeDocumentation(name = "Environment variables", description = "These variables are provided to the R executable. Existing environment " +
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
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/rlogo_icon.png");
    }

    @Override
    public String getInfo() {
        return StringUtils.orElse(RExecutablePath, "<Not set>");
    }

    public static class RArgumentsVariableSource implements ExpressionParameterVariableSource {
        @Override
        public Set<ExpressionParameterVariable> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            Set<ExpressionParameterVariable> result = new HashSet<>();
            result.add(new ExpressionParameterVariable("R executable", "The R executable", "r_executable"));
            result.add(new ExpressionParameterVariable("Script file", "The R script file to be executed", "script_file"));
            return result;
        }
    }

    /**
     * A list of {@link REnvironment}
     */
    public static class List extends ListParameter<REnvironment> {
        public List() {
            super(REnvironment.class);
        }

        public List(REnvironment.List other) {
            super(REnvironment.class);
            for (REnvironment environment : other) {
                add(new REnvironment(environment));
            }
        }
    }
}
