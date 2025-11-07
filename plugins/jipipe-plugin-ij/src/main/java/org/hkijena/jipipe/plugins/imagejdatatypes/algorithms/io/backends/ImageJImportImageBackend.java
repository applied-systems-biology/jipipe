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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io.backends;

import ij.IJ;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionVariablesMap;
import org.hkijena.jipipe.utils.CoreImageJUtils;
import org.hkijena.jipipe.utils.IJLogToJIPipeProgressInfoPump;

import java.nio.file.Path;

public class ImageJImportImageBackend extends ImportImageBackend {

    public ImageJImportImageBackend() {
    }

    public ImageJImportImageBackend(ImageJImportImageBackend other) {
        super(other);
    }

    @Override
    public boolean canImport(Path path, JIPipeExpressionVariablesMap variablesMap) {
        return CoreImageJUtils.supportsNativeImageImport(path);
    }

    @Override
    public ImagePlus doImport(Path path, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        try (IJLogToJIPipeProgressInfoPump pump = new IJLogToJIPipeProgressInfoPump(progressInfo)) {
            return IJ.openImage(path.toString());
        }
    }
}
