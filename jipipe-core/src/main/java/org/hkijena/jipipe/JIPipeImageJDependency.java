package org.hkijena.jipipe;

import com.google.common.eventbus.EventBus;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.JIPipeValidityReport;

import java.nio.file.Path;

/**
 * A dependency that points to an ImageJ update site.
 * The ID is the update site name and the URL is the site URL that might be used if the site does not exist in the repository.
 */
public class JIPipeImageJDependency extends JIPipeMutableDependency {
    public JIPipeImageJDependency() {
    }

    /**
     * Initializes the dependency from an update site
     * @param updateSite the update site
     */
    public JIPipeImageJDependency(UpdateSite updateSite) {

    }

    public JIPipeImageJDependency(JIPipeDependency other) {
        super(other);
    }
}
