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

package org.hkijena.jipipe.plugins.r;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.expressions.ExpressionParameterVariablesInfo;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariableInfo;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.api.pairs.PairParameterSettings;
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

public class REnvironment extends JIPipeArtifactEnvironment {
    public static final String ENVIRONMENT_ID = "r";
    private Path RExecutablePath = Paths.get("");
    private Path RScriptExecutablePath = Paths.get("");
    private JIPipeExpressionParameter arguments = new JIPipeExpressionParameter("ARRAY(script_file)");
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
        this.arguments = new JIPipeExpressionParameter(other.arguments);
        this.environmentVariables = new StringQueryExpressionAndStringPairParameter.List(other.environmentVariables);
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (!isLoadFromArtifact()) {
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
    }

    @Override
    public boolean isParameterUIVisible(JIPipeParameterTree tree, JIPipeParameterAccess access) {
        if ("r-executable-path".equals(access.getKey())) {
            return !isLoadFromArtifact();
        }
        if ("rscript-executable-path".equals(access.getKey())) {
            return !isLoadFromArtifact();
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    public void applyConfigurationFromArtifact(JIPipeLocalArtifact artifact, JIPipeProgressInfo progressInfo) {
        setArguments(new JIPipeExpressionParameter("ARRAY(script_file)"));
        Path binaryDir = artifact.getLocalPath().resolve("r").resolve("bin");
        if (SystemUtils.IS_OS_WINDOWS) {
            setRExecutablePath(binaryDir.resolve("R.exe"));
            setRScriptExecutablePath(binaryDir.resolve("Rscript.exe"));
        } else {
            setRExecutablePath(binaryDir.resolve("R"));
            setRScriptExecutablePath(binaryDir.resolve("Rscript"));

            // Do chmod +x for all executables
            PathUtils.makeAllUnixExecutable(binaryDir, progressInfo);
        }

    }

    @SetJIPipeDocumentation(name = "R executable", description = "The main R executable (R.exe on Windows)")
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

    @SetJIPipeDocumentation(name = "RScript executable", description = "The RScript executable (RScript.exe on Windows)")
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

    @SetJIPipeDocumentation(name = "Arguments", description = "Arguments passed to the Python/Conda executable (depending on the environment type). " +
            "This expression must return an array. You have two variables 'script_file' and 'r_executable'. 'script_file' is always " +
            "replaced by the Python script that is currently executed.")
    @JIPipeParameter("arguments")
    @JIPipeExpressionParameterSettings(variableSource = RArgumentsVariablesInfo.class)
    @JsonGetter("arguments")
    public JIPipeExpressionParameter getArguments() {
        return arguments;
    }

    @JsonSetter("arguments")
    @JIPipeParameter("arguments")
    public void setArguments(JIPipeExpressionParameter arguments) {
        this.arguments = arguments;
    }

    @SetJIPipeDocumentation(name = "Environment variables", description = "These variables are provided to the R executable. Existing environment " +
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
    public Icon getIcon() {
        if (isLoadFromArtifact()) {
            return UIUtils.getIconFromResources("actions/run-install.png");
        } else {
            return UIUtils.getIconFromResources("apps/rlogo_icon.png");
        }
    }

    @Override
    public String getInfo() {
        if (isLoadFromArtifact()) {
            return getArtifactQuery().getQuery();
        } else {
            return StringUtils.orElse(RExecutablePath, "<Not set>");
        }
    }

    public static class RArgumentsVariablesInfo implements ExpressionParameterVariablesInfo {
        @Override
        public Set<JIPipeExpressionParameterVariableInfo> getVariables(JIPipeParameterTree parameterTree, JIPipeParameterAccess parameterAccess) {
            Set<JIPipeExpressionParameterVariableInfo> result = new HashSet<>();
            result.add(new JIPipeExpressionParameterVariableInfo("r_executable", "R executable", "The R executable"));
            result.add(new JIPipeExpressionParameterVariableInfo("script_file", "Script file", "The R script file to be executed"));
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
