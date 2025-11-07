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

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

public class JavaImportImageBackend extends PathMatchedImportImageBackend {

    public static final String[] IMAGEIO_ONLY_EXTENSIONS = {
            ".arw", ".cr2", ".cr3", ".nef", ".orf", ".rw2", ".dng",
            ".heic", ".heif", ".avif", ".ico", ".icns", ".webp", ".wbmp"
    };

    private boolean greyscaleCorrection = true;

    public JavaImportImageBackend() {
    }

    public JavaImportImageBackend(JavaImportImageBackend other) {
        super(other);
        this.greyscaleCorrection = other.greyscaleCorrection;
    }

    @Override
    public String getDefaultPathMatching() {
        return "PATH_MATCHES_EXTENSION(path, " +
                Arrays.stream(IMAGEIO_ONLY_EXTENSIONS)
                        .map(ext -> "\"" + ext + "\"")
                        .collect(Collectors.joining(", ")) +
                ")";
    }


    @Override
    public ImagePlus doImport(Path path, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        BufferedImage image = BufferedImageUtils.read(path, greyscaleCorrection);
        return new ImagePlus(path.getFileName().toString(), image);
    }
}
