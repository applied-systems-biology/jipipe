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

package org.hkijena.jipipe.plugins.ilastik;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterSettings;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameterVariable;
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

/**
 * Parameter that describes a Python environment
 */
public class IlastikEnvironment extends JIPipeArtifactEnvironment {

    private JIPipeExpressionParameter arguments = new JIPipeExpressionParameter("cli_parameters");
    private Path executablePath = Paths.get("");
    private StringQueryExpressionAndStringPairParameter.List environmentVariables = new StringQueryExpressionAndStringPairParameter.List();

    public IlastikEnvironment() {

    }

    public IlastikEnvironment(IlastikEnvironment other) {
        super(other);
        this.arguments = new JIPipeExpressionParameter(other.arguments);
        this.executablePath = other.executablePath;
        this.environmentVariables = new StringQueryExpressionAndStringPairParameter.List(other.environmentVariables);
    }

    @SetJIPipeDocumentation(name = "Arguments", description = "Arguments passed to the Ilastik executable.")
    @JIPipeParameter("arguments")
    @JsonGetter("arguments")
    @JIPipeExpressionParameterVariable(key = "cli_parameters", description = "CLI parameters that should be passed to Ilastik", name = "CLI parameters")
    public JIPipeExpressionParameter getArguments() {
        return arguments;
    }

    @JsonSetter("arguments")
    @JIPipeParameter("arguments")
    public void setArguments(JIPipeExpressionParameter arguments) {
        this.arguments = arguments;
    }

    @SetJIPipeDocumentation(name = "Executable path", description = "The Ilastik executable")
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

    @SetJIPipeDocumentation(name = "Environment variables", description = "These variables are provided to the Ilastik executable. Existing environment " +
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
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (!isLoadFromArtifact()) {
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
        if ("executable-path".equals(access.getKey())) {
            return !isLoadFromArtifact();
        }
        return super.isParameterUIVisible(tree, access);
    }

    @Override
    public Icon getIcon() {
        if (isLoadFromArtifact()) {
            return UIUtils.getIconFromResources("actions/run-install.png");
        } else {
            return IlastikPlugin.RESOURCES.getIconFromResources("ilastik.png");
        }
    }

    @Override
    public String getInfo() {
        if (isLoadFromArtifact()) {
            return StringUtils.orElse(getArtifactQuery().getQuery(), "<Not set>");
        } else {
            return StringUtils.orElse(getExecutablePath(), "<Not set>");
        }
    }

    @Override
    public String toString() {
        return "IlastikEnvironment {" +
                ", arguments=" + arguments +
                ", executablePath=" + executablePath +
                ", environmentVariables=" + environmentVariables +
                ", artifactQuery=" + getArtifactQuery() +
                '}';
    }

    /**
     * A list of {@link IlastikEnvironment}
     */
    public static class List extends ListParameter<IlastikEnvironment> {
        public List() {
            super(IlastikEnvironment.class);
        }

        public List(List other) {
            super(IlastikEnvironment.class);
            for (IlastikEnvironment environment : other) {
                add(new IlastikEnvironment(environment));
            }
        }
    }
}
