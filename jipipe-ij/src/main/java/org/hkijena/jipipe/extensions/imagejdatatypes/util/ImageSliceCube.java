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

package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import java.util.Objects;

public class ImageSliceCube {
    private ImageSliceIndex first = new ImageSliceIndex();
    private ImageSliceIndex second = new ImageSliceIndex();

    public ImageSliceCube() {
    }

    public ImageSliceCube(ImageSliceIndex first, ImageSliceIndex second) {
        this.first = first;
        this.second = second;
    }

    public ImageSliceCube(ImageSliceCube other) {
        this.first = new ImageSliceIndex(other.first);
        this.second = new ImageSliceIndex(other.second);
    }

    public ImageSliceIndex getFirst() {
        return first;
    }

    public void setFirst(ImageSliceIndex first) {
        this.first = first;
    }

    public ImageSliceIndex getSecond() {
        return second;
    }

    public void setSecond(ImageSliceIndex second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageSliceCube that = (ImageSliceCube) o;
        return first.equals(that.first) && second.equals(that.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
