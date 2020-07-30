package org.hkijena.jipipe;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

/**
 * A dependency that points to an ImageJ update site.
 * The ID contains the update site name and the URL is the site URL that might be used if the site does not exist in the repository.
 */
public class JIPipeImageJUpdateSiteDependency implements JIPipeParameterCollection  {

    private final EventBus eventBus = new EventBus();
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

    @JIPipeDocumentation(name = "Name", description = "Unique name of the update site.")
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

    @JIPipeDocumentation(name = "URL", description = "Update site URL. Please note that JIPipe prefers existing update sites with the same name and therefor might " +
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

    @JIPipeDocumentation(name = "Description", description = "Optional description")
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

    @JIPipeDocumentation(name = "Maintainer", description = "Optional maintainer")
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

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * the List class
     */
    public static class List extends ListParameter<JIPipeImageJUpdateSiteDependency> {
        public List() {
            super(JIPipeImageJUpdateSiteDependency.class);
        }
    }
}
