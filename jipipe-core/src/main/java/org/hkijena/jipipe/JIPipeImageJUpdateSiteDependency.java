package org.hkijena.jipipe;

import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.utils.StringUtils;

/**
 * A dependency that points to an ImageJ update site.
 * The ID contains the update site name and the URL is the site URL that might be used if the site does not exist in the repository.
 */
public class JIPipeImageJUpdateSiteDependency extends JIPipeMutableDependency {
    public JIPipeImageJUpdateSiteDependency() {
    }

    /**
     * Initializes the dependency from an update site
     *
     * @param updateSite the update site
     */
    public JIPipeImageJUpdateSiteDependency(UpdateSite updateSite) {
        this.setDependencyId("ij:update-site:" + updateSite.getName());
        this.getMetadata().setName(updateSite.getName());
        this.getMetadata().setDescription(updateSite.getDescription());
        this.getMetadata().setWebsite(updateSite.getURL());
        if (!StringUtils.isNullOrEmpty(updateSite.getMaintainer())) {
            this.getMetadata().getAuthors().add(new JIPipeAuthorMetadata(updateSite.getMaintainer(), "", ""));
        }
    }

    public JIPipeImageJUpdateSiteDependency(JIPipeDependency other) {
        super(other);
    }

    public UpdateSite toUpdateSite() {
        return new UpdateSite(getName(), getURL(), "", "", getMetadata().getDescription(), "", 0);
    }

    public String getName() {
        return getMetadata().getName();
    }

    public String getURL() {
        return getMetadata().getWebsite();
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
