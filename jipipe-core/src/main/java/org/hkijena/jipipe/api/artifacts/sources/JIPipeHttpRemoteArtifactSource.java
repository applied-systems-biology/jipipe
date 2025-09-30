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

package org.hkijena.jipipe.api.artifacts.sources;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JIPipeHttpRemoteArtifactSource extends JIPipeRemoteArtifactSource {

    @JsonProperty("url")
    private String url;

    public JIPipeHttpRemoteArtifactSource() {
    }

    public JIPipeHttpRemoteArtifactSource(String url) {
        this.url = url;
    }

    public JIPipeHttpRemoteArtifactSource(JIPipeHttpRemoteArtifactSource other) {
        this.url = other.url;
    }

    @Override
    @JsonGetter("type")
    public JIPipeRemoteArtifactSourceType getType() {
        return JIPipeRemoteArtifactSourceType.HTTP;
    }

    @Override
    public JIPipeRemoteArtifactSource duplicate() {
        return new JIPipeHttpRemoteArtifactSource(this);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
