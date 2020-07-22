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

package org.hkijena.jipipe.api.nodes;

import org.hkijena.jipipe.api.data.JIPipeAnnotation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Structure that can hold multiple {@link JIPipeAnnotation} instances to make a set of traits a key
 */
public class JIPipeDataBatchKey {
    private Map<String, JIPipeAnnotation> entries = new HashMap<>();

    public Map<String, JIPipeAnnotation> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, JIPipeAnnotation> entries) {
        this.entries = entries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeDataBatchKey that = (JIPipeDataBatchKey) o;
        return entries.equals(that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }

    @Override
    public String toString() {
        return entries.values().stream().map(jipipeTrait -> "" + jipipeTrait).collect(Collectors.joining(", "));
    }
}
