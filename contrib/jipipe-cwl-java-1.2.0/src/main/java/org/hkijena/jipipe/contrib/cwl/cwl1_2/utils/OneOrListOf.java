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

package org.hkijena.jipipe.contrib.cwl.cwl1_2.utils;

import java.util.List;
import java.util.Optional;

public class OneOrListOf<T> {
    private Optional<T> object;
    private Optional<List<T>> objects;

    private OneOrListOf(final T object, final List<T> objects) {
        this.object = Optional.ofNullable(object);
        this.objects = Optional.ofNullable(objects);
    }

    public static <T> OneOrListOf<T> oneOf(T object) {
        return new OneOrListOf(object, null);
    }

    public static <T> OneOrListOf<T> listOf(List<T> objects) {
        assert objects != null;
        return new OneOrListOf(null, objects);
    }

    public boolean isOne() {
        return this.getOneOptional().isPresent();
    }

    public boolean isList() {
        return this.getListOptional().isPresent();
    }

    public Optional<T> getOneOptional() {
        return this.object;
    }
    
    public Optional<List<T>> getListOptional() {
        return this.objects;
    }

    public T getOne() {
        return this.getOneOptional().get();
    }

    public List<T> getList() {
        return this.getListOptional().get();
    }

}
