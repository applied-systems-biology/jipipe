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

package org.hkijena.jipipe.plugins.ijfilaments.environments;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.environments.JIPipeProcessArtifactEnvironment;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.ijfilaments.FilamentsPlugin;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.utils.PathUtils;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Parameter that describes a TSOAX environment
 */
public class TSOAXEnvironment extends JIPipeProcessArtifactEnvironment {

    public TSOAXEnvironment() {

    }

    public TSOAXEnvironment(TSOAXEnvironment other) {
        super(other);
    }

    @Override
    public void applyConfigurationFromArtifact(JIPipeLocalArtifact artifact, JIPipeProgressInfo progressInfo) {
        Path tsoaxDir = artifact.getLocalPath().resolve("tsoax");
        if (SystemUtils.IS_OS_WINDOWS) {
            setExecutablePath(tsoaxDir.resolve("tsoax.exe"));
            setArguments(new JIPipeExpressionParameter("cli_parameters"));
        } else if (SystemUtils.IS_OS_LINUX) {
            setExecutablePath(tsoaxDir.resolve("tsoax"));
            setArguments(new JIPipeExpressionParameter("cli_parameters"));
            PathUtils.makeUnixExecutable(getExecutablePath());
            PathUtils.makeAllUnixExecutable(tsoaxDir.resolve("bin"), progressInfo);
        } else {
            setExecutablePath(tsoaxDir.resolve("tsoax"));
            setArguments(new JIPipeExpressionParameter("cli_parameters"));
            PathUtils.makeUnixExecutable(getExecutablePath());
            PathUtils.makeAllUnixExecutable(tsoaxDir.resolve("bin"), progressInfo);
        }

    }

    @Override
    public Icon getNonArtifactIcon() {
        return FilamentsPlugin.RESOURCES.getIconFromResources("tsoax.png");
    }

    /**
     * A list of {@link TSOAXEnvironment}
     */
    public static class List extends ListParameter<TSOAXEnvironment> {
        public List() {
            super(TSOAXEnvironment.class);
        }

        public List(List other) {
            super(TSOAXEnvironment.class);
            for (TSOAXEnvironment environment : other) {
                add(new TSOAXEnvironment(environment));
            }
        }
    }
}
