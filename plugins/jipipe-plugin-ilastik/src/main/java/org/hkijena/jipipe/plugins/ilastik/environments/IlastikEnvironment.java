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

package org.hkijena.jipipe.plugins.ilastik.environments;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeArtifactProcessEnvironment;
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
import org.hkijena.jipipe.plugins.ilastik.IlastikPlugin;
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
public class IlastikEnvironment extends JIPipeArtifactProcessEnvironment {

    public IlastikEnvironment() {

    }

    public IlastikEnvironment(IlastikEnvironment other) {
        super(other);
    }

    @Override
    public void applyConfigurationFromArtifact(JIPipeLocalArtifact artifact, JIPipeProgressInfo progressInfo) {
        Path ilastikDir = artifact.getLocalPath().resolve("ilastik");
        Path binaryDir = artifact.getLocalPath().resolve("ilastik").resolve("bin");
        if (SystemUtils.IS_OS_WINDOWS) {
            setExecutablePath(ilastikDir.resolve("ilastik.exe"));
            setArguments(new JIPipeExpressionParameter("cli_parameters"));
        } else if (SystemUtils.IS_OS_LINUX) {
            setExecutablePath(ilastikDir.resolve("run_ilastik.sh"));
            setArguments(new JIPipeExpressionParameter("cli_parameters"));

            PathUtils.makeUnixExecutable(getExecutablePath());
            PathUtils.makeAllUnixExecutable(binaryDir, progressInfo);
        } else {
            setExecutablePath(ilastikDir.resolve("run_ilastik.sh"));
            setArguments(new JIPipeExpressionParameter("cli_parameters"));

            PathUtils.makeUnixExecutable(getExecutablePath());
            PathUtils.makeAllUnixExecutable(binaryDir, progressInfo);
        }

    }

    @Override
    public Icon getNonArtifactIcon() {
        return IlastikPlugin.RESOURCES.getIconFromResources("ilastik.png");
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
