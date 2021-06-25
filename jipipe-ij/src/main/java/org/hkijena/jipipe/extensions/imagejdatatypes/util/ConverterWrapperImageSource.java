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

public class ConverterWrapperImageSource implements ImageSource {

    private final ImageSource wrappedSource;
    private final Function<ImagePlus, ImagePlus> converter;

    public ConverterWrapperImageSource(ImageSource wrappedSource, Function<ImagePlus, ImagePlus> converter) {
        this.wrappedSource = wrappedSource;
        this.converter = converter;
    }

    @Override
    public String getLabel() {
        return wrappedSource.getLabel();
    }

    @Override
    public Icon getIcon() {
        return wrappedSource.getIcon();
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        wrappedSource.saveTo(storageFilePath, name, forceName, progressInfo);
    }

    @Override
    public ImagePlus get() {
        return converter.apply(wrappedSource.get());
    }
}
