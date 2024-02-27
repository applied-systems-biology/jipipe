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

package org.hkijena.jipipe;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;

/**
 * A dependency that points to an ImageJ update site.
 * The ID contains the update site name and the URL is the site URL that might be used if the site does not exist in the repository.
 */
public class JIPipeImageJUpdateSiteDependency extends AbstractJIPipeParameterCollection {

    private String name;
    private String url;
    private String description;
    private String maintainer;

    public JIPipeImageJUpdateSiteDependency() {
    }

    public JIPipeImageJUpdateSiteDependency(String name, String url) {
        this.name = name;
        this.url = url;
    }

    /**
     * Initializes the dependency from an update site
     *
     * @param updateSite the update site
     */
    public JIPipeImageJUpdateSiteDependency(UpdateSite updateSite) {
        setName(updateSite.getName());
        setDescription(updateSite.getDescription());
        setUrl(updateSite.getURL());
        setMaintainer(updateSite.getMaintainer());
    }

    public JIPipeImageJUpdateSiteDependency(JIPipeImageJUpdateSiteDependency other) {
        this.name = other.name;
        this.url = other.url;
        this.description = other.description;
        this.maintainer = other.maintainer;
    }

    public UpdateSite toUpdateSite() {
        return new UpdateSite(getName(), getUrl(), "", "", getDescription(), getMaintainer(), 0);
    }

    @SetJIPipeDocumentation(name = "Name", description = "Unique name of the update site.")
    @JsonGetter("name")
    @JIPipeParameter(value = "name", uiOrder = 0)
    @StringParameterSettings(monospace = true)
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @SetJIPipeDocumentation(name = "URL", description = "Update site URL. Please note that JIPipe prefers existing update sites with the same name and therefor might " +
            " ignore this property.")
    @JIPipeParameter(value = "url", uiOrder = 1)
    @JsonGetter("url")
    @StringParameterSettings(monospace = true, icon = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/web-browser.png")
    public String getUrl() {
        return url;
    }

    @JIPipeParameter("url")
    @JsonSetter("url")
    public void setUrl(String url) {
        this.url = url;
    }

    @SetJIPipeDocumentation(name = "Description", description = "Optional description")
    @JIPipeParameter(value = "description", uiOrder = 2)
    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JIPipeParameter("description")
    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @SetJIPipeDocumentation(name = "Maintainer", description = "Optional maintainer")
    @JIPipeParameter(value = "maintainer", uiOrder = 3)
    @JsonGetter("maintainer")
    public String getMaintainer() {
        return maintainer;
    }

    @JIPipeParameter("maintainer")
    @JsonSetter("maintainer")
    public void setMaintainer(String maintainer) {
        this.maintainer = maintainer;
    }

    /**
     * the List class
     */
    public static class List extends ListParameter<JIPipeImageJUpdateSiteDependency> {
        public List() {
            super(JIPipeImageJUpdateSiteDependency.class);
        }

        public List(List other) {
            super(JIPipeImageJUpdateSiteDependency.class);
            for (JIPipeImageJUpdateSiteDependency dependency : other) {
                add(new JIPipeImageJUpdateSiteDependency(dependency));
            }
        }
    }
}
