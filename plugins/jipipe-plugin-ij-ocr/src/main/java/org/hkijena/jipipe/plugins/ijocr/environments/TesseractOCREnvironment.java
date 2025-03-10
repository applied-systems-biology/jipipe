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

package org.hkijena.jipipe.plugins.ijocr.environments;

import org.apache.commons.lang3.SystemUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.artifacts.JIPipeLocalArtifact;
import org.hkijena.jipipe.api.environments.JIPipeArtifactProcessEnvironment;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

/**
 * Parameter that describes a TSOAX environment
 */
public class TesseractOCREnvironment extends JIPipeArtifactProcessEnvironment {

    public TesseractOCREnvironment() {

    }

    public TesseractOCREnvironment(TesseractOCREnvironment other) {
        super(other);
    }

    @Override
    public void applyConfigurationFromArtifact(JIPipeLocalArtifact artifact, JIPipeProgressInfo progressInfo) {
        Path appDir = artifact.getLocalPath().resolve("tesseract-ocr");
        if (SystemUtils.IS_OS_WINDOWS) {
            setExecutablePath(appDir.resolve("tesseract.exe"));
            setArguments(new JIPipeExpressionParameter("cli_parameters"));
        } else if (SystemUtils.IS_OS_LINUX) {
            setExecutablePath(appDir.resolve("tesseract-ocr"));
            setArguments(new JIPipeExpressionParameter("cli_parameters"));
            PathUtils.makeUnixExecutable(getExecutablePath());
            PathUtils.makeAllUnixExecutable(appDir.resolve("bin"), progressInfo);
        } else {
            setExecutablePath(appDir.resolve("tesseract-ocr"));
            setArguments(new JIPipeExpressionParameter("cli_parameters"));
            PathUtils.makeUnixExecutable(getExecutablePath());
            PathUtils.makeAllUnixExecutable(appDir.resolve("bin"), progressInfo);
        }

    }

    @Override
    public Icon getNonArtifactIcon() {
        return UIUtils.getIconFromResources("actions/text_outer_style.png");
    }

    /**
     * A list of {@link TesseractOCREnvironment}
     */
    public static class List extends ListParameter<TesseractOCREnvironment> {
        public List() {
            super(TesseractOCREnvironment.class);
        }

        public List(List other) {
            super(TesseractOCREnvironment.class);
            for (TesseractOCREnvironment environment : other) {
                add(new TesseractOCREnvironment(environment));
            }
        }
    }
}
