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

package org.hkijena.acaq5.api.algorithm;

import org.hkijena.acaq5.api.data.ACAQAnnotation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Structure that can hold multiple {@link ACAQAnnotation} instances to make a set of traits a key
 */
public class ACAQDataSetKey {
    private Map<String, ACAQAnnotation> entries = new HashMap<>();

    public Map<String, ACAQAnnotation> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, ACAQAnnotation> entries) {
        this.entries = entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ACAQDataSetKey that = (ACAQDataSetKey) o;
        return entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }

    @Override
    public String toString() {
        return entries.values().stream().map(acaqTrait -> "" + acaqTrait).collect(Collectors.joining(", "));
    }
}
