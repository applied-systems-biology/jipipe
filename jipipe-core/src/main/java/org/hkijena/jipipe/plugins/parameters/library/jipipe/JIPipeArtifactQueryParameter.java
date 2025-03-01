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

package org.hkijena.jipipe.plugins.parameters.library.jipipe;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * Parameter that allows the user to select or query an artifact
 */
public class JIPipeArtifactQueryParameter {
    private String query;

    public JIPipeArtifactQueryParameter() {
    }

    public JIPipeArtifactQueryParameter(String query) {
        this.query = query;
    }

    public JIPipeArtifactQueryParameter(JIPipeArtifactQueryParameter other) {
        this.query = other.query;
    }

    @JsonGetter("query")
    public String getQuery() {
        return query;
    }

    @JsonSetter("query")
    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public String toString() {
        return query;
    }

    public boolean isStatic() {
        return query.contains(":") && !query.contains("*");
    }

    public String getBaseQuery() {
        if(isStatic()) {
            return query.split(":")[0] + ":*";
        }
        return query;
    }
}
