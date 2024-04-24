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

package org.hkijena.jipipe.api.artifacts;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class JIPipeRemoteArtifact extends JIPipeArtifact {
    private String url;
    private long size;

    public JIPipeRemoteArtifact() {
    }

    public JIPipeRemoteArtifact(JIPipeRemoteArtifact other) {
        super(other);
        this.url = other.url;
        this.size = other.size;
    }

    @JsonGetter("url")
    public String getUrl() {
        return url;
    }

    @JsonSetter("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @JsonGetter("size")
    public long getSize() {
        return size;
    }

    @JsonSetter("size")
    public void setSize(long size) {
        this.size = size;
    }
}
