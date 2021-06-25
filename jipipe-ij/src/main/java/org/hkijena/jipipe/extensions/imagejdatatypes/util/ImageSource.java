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

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;

import javax.swing.*;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A function that loads an {@link ImagePlus}
 */
public interface ImageSource extends Supplier<ImagePlus> {
    /**
     * @return Label that describes the image source
     */
    String getLabel();

    /**
     * @return Icon for this source shown in the UI
     */
    Icon getIcon();

    /**
     * Saves the data stored by this source according to the JIPipe standard format ({@link org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData}
     * @param storageFilePath the storage file path
     * @param name the name
     * @param forceName if the name should be forced
     * @param progressInfo progress info
     */
    void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo);
}
