/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.imagejdatatypes.datasources;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSource;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImagePlusFromFileImageSource implements ImageSource {

    private final Path path;
    private final boolean removeLUT;
    private final boolean removeOverlay;

    public ImagePlusFromFileImageSource(Path path, boolean removeLUT, boolean removeOverlay) {
        this.path = path;
        this.removeLUT = removeLUT;
        this.removeOverlay = removeOverlay;
    }

    @Override
    public String getLabel() {
        return path.toString();
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("data-types/file.png");
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        try {
            Files.copy(path, storageFilePath.resolve(name + ".tif"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ImagePlus get() {
        ImagePlus image = ImagePlusFromFile.readImageFrom(path, new JIPipeProgressInfo());
        if (removeLUT) {
            ImageJUtils.removeLUT(image, true);
        }
        if(removeOverlay) {
            ImageJUtils.removeOverlay(image);
        }
        return image;
    }
}
