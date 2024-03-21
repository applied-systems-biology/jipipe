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

package org.hkijena.jipipe.extensions.imp.utils;

import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageSliceIndex;
import org.hkijena.jipipe.extensions.imp.datatypes.ImpImageData;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class ImpImageDataBuilder {
    private final Map<ImageSliceIndex, BufferedImage> imageMap = new HashMap<>();

    public void put(ImageSliceIndex index, BufferedImage image) {
        imageMap.put(index, image);
    }

    public void put(int c, int z, int t, BufferedImage image) {
        imageMap.put(new ImageSliceIndex(c, z, t), image);
    }

    public BufferedImage get(ImageSliceIndex index) {
        return imageMap.get(index);
    }

    public BufferedImage get(int c, int z, int t) {
        return get(new ImageSliceIndex(c, z, t));
    }

    public ImpImageData build() {
        return new ImpImageData(imageMap);
    }
}
