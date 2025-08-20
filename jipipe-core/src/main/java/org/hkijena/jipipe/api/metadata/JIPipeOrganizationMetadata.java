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

package org.hkijena.jipipe.api.metadata;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Objects;

public class JIPipeOrganizationMetadata extends AbstractJIPipeParameterCollection {
    private String name;
    private String ror;
    private String website;

    public JIPipeOrganizationMetadata() {

    }

    public JIPipeOrganizationMetadata(String name, String ror, String website) {
        this.name = name;
        this.ror = ror;
        this.website = website;
    }

    public JIPipeOrganizationMetadata(JIPipeOrganizationMetadata other) {
        this.name = other.name;
        this.ror = other.ror;
        this.website = other.website;
    }

    @SetJIPipeDocumentation(name = "Name", description = "The name of the organization")
    @JIPipeParameter(value = "name", uiOrder = -100)
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JIPipeParameter("name")
    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
    }

    @SetJIPipeDocumentation(name = "Website", description = "The website of the organization")
    @JIPipeParameter(value = "website", uiOrder = -90)
    @JsonGetter("website")
    @StringParameterSettings(icon = "actions/web-browser.png", monospace = true)
    public String getWebsite() {
        return website;
    }

    @JIPipeParameter("website")
    @JsonSetter("website")
    public void setWebsite(String website) {
        this.website = website;
    }

    @SetJIPipeDocumentation(name = "ROR", description = "The ROR ID or URL. See https://ror.org/ for more information")
    @JIPipeParameter(value = "ror", uiOrder = -80)
    @JsonGetter("ror")
    @StringParameterSettings(icon = "actions/web-browser.png", monospace = true)
    public String getRor() {
        return ror;
    }

    @JIPipeParameter("ror")
    @JsonSetter("ror")
    public void setRor(String ror) {
        this.ror = ror;
    }

    public String getRorUrl() {
        if (!StringUtils.isNullOrEmpty(getRor())) {
            if (getRor().startsWith("http") || getRor().contains("ror.org")) {
                return getRor();
            }
            return "https://ror.org/" + getRor();
        }
        return "";
    }

    public String getUniqueId() {
        if (!StringUtils.isNullOrEmpty(getRorUrl())) {
            return getRorUrl();
        } else {
            return getWebsite();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        JIPipeOrganizationMetadata other = (JIPipeOrganizationMetadata) obj;

        // Prefer ROR if available
        if (ror != null && other.ror != null)
            return ror.equalsIgnoreCase(other.ror);

        // Fallback to website
        if (website != null && other.website != null)
            return website.equalsIgnoreCase(other.website);

        // Fallback to name
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        if (ror != null)
            return ror.toLowerCase().hashCode();
        if (website != null)
            return website.toLowerCase().hashCode();
        return name != null ? name.hashCode() : 0;
    }

    public void mergeWith(JIPipeOrganizationMetadata affiliation) {
        if (StringUtils.isNullOrEmpty(website) && !StringUtils.isNullOrEmpty(affiliation.website)) {
            website = affiliation.website;
        }
        if (StringUtils.isNullOrEmpty(ror) && !StringUtils.isNullOrEmpty(affiliation.ror)) {
            ror = affiliation.ror;
        }
    }

    public static class List extends ListParameter<JIPipeOrganizationMetadata> {
        public List() {
            super(JIPipeOrganizationMetadata.class);
        }

        public List(List other) {
            super(JIPipeOrganizationMetadata.class);
            for (JIPipeOrganizationMetadata metadata : other) {
                add(new JIPipeOrganizationMetadata(metadata));
            }
        }
    }

    public static class Builder {
        private final JIPipeOrganizationMetadata instance = new JIPipeOrganizationMetadata();

        public Builder name(String name) {
            instance.setName(name);
            return this;
        }

        public Builder ror(String ror) {
            instance.setRor(ror);
            return this;
        }

        public Builder website(String website) {
            instance.setWebsite(website);
            return this;
        }

        public JIPipeOrganizationMetadata build() {
            return instance;
        }
    }

}
