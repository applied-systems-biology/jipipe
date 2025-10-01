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

package org.hkijena.jipipe.api.artifacts.index.v1;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class JIPipeArtifactIndexV1OrasPackageSource extends JIPipeArtifactIndexV1PackageSource {

    @JsonProperty("oci-ref")
    private String ociRef;

    @JsonProperty("rel")
    private String rel;

    public JIPipeArtifactIndexV1OrasPackageSource() {
    }

    @JsonGetter("type")
    @Override
    public String getType() {
        return "oras";
    }

    public String getOciRef() {
        return ociRef;
    }

    public void setOciRef(String ociRef) {
        this.ociRef = ociRef;
    }

    public String getRel() {
        return rel;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }
}
