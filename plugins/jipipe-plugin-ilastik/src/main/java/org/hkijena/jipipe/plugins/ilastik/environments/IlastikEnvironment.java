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

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.environments.JIPipeProcessArtifactEnvironment;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.ilastik.IlastikPlugin;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Parameter that describes a Python environment
 */
public class IlastikEnvironment extends JIPipeProcessArtifactEnvironment {

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
