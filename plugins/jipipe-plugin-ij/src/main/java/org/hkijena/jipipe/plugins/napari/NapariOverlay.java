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

package org.hkijena.jipipe.plugins.napari;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import java.nio.file.Path;
import java.util.List;

/**
 * Type that can export overlay data for Napari
 */
public interface NapariOverlay {
    List<Path> exportOverlayToNapari(ImagePlus imp, Path outputDirectory, String prefix, JIPipeProgressInfo progressInfo);
}
