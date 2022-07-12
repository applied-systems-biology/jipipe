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
 *
 */

package org.hkijena.jipipe.ui.extensions;

import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipeExtension;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;

/**
 * Helper class that encapsulates an ImageJ update site into a {@link JIPipeExtension}
 */
public class UpdateSiteExtension implements JIPipeExtension {

    private final UpdateSite updateSite;
    private final JIPipeMetadata metadata = new JIPipeMetadata();

    public UpdateSiteExtension(UpdateSite updateSite) {
        this.updateSite = updateSite;
        metadata.setName(updateSite.getName());
        metadata.setDescription(new HTMLText(updateSite.getDescription()));
        metadata.setSummary(new HTMLText(updateSite.getDescription()));
        metadata.setWebsite(updateSite.getURL());
        metadata.setThumbnail(new ImageParameter(ResourceUtils.getPluginResource("thumbnails/imagej-update-site.png")));
        if (!StringUtils.isNullOrEmpty(updateSite.getMaintainer())) {
            metadata.setAuthors(new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("", updateSite.getMaintainer(), "", new StringList(), "", "", true, true)));
        }
    }

    @Override
    public JIPipeMetadata getMetadata() {
        return metadata;
    }

    @Override
    public String getDependencyId() {
        return updateSite.getName();
    }

    @Override
    public String getDependencyVersion() {
        return "N/A";
    }

    @Override
    public Path getDependencyLocation() {
        return null;
    }

    @Override
    public boolean isActivated() {
        return updateSite.isActive();
    }

    @Override
    public boolean isScheduledForActivation() {
        return false;
    }

    @Override
    public boolean isScheduledForDeactivation() {
        return false;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {

    }
}
