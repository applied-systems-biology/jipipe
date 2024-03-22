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

package org.hkijena.jipipe.desktop.app.extensions;

import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import org.hkijena.jipipe.JIPipeImageJUpdateSiteDependency;
import org.hkijena.jipipe.JIPipePlugin;
import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.StringUtils;

import java.nio.file.Path;

/**
 * Helper class that encapsulates an ImageJ update site into a {@link JIPipePlugin}
 */
public class JIPipeDesktopUpdateSitePlugin implements JIPipePlugin {

    private final JIPipeMetadata metadata = new JIPipeMetadata();
    private JIPipeImageJUpdateSiteDependency dependency;
    private UpdateSite updateSite;

    public JIPipeDesktopUpdateSitePlugin(UpdateSite updateSite) {
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

    public JIPipeDesktopUpdateSitePlugin(JIPipeImageJUpdateSiteDependency dependency) {
        this.dependency = dependency;
        metadata.setName(dependency.getName());
        metadata.setDescription(new HTMLText(dependency.getDescription()));
        metadata.setSummary(new HTMLText(dependency.getDescription()));
        metadata.setWebsite(dependency.getUrl());
        metadata.setThumbnail(new ImageParameter(ResourceUtils.getPluginResource("thumbnails/imagej-update-site.png")));
        if (!StringUtils.isNullOrEmpty(dependency.getMaintainer())) {
            metadata.setAuthors(new JIPipeAuthorMetadata.List(new JIPipeAuthorMetadata("", dependency.getMaintainer(), "", new StringList(), "", "", true, true)));
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
        if (updateSite != null)
            return updateSite.isActive();
        else
            return false;
    }

    @Override
    public boolean isScheduledForActivation() {
        return false;
    }

    @Override
    public boolean isScheduledForDeactivation() {
        return false;
    }

    public UpdateSite getUpdateSite(FilesCollection updateSites) {
        if (updateSite != null) {
            return updateSite;
        } else {
            this.updateSite = updateSites.getUpdateSite(dependency.getName(), true);
            if (updateSite == null)
                this.updateSite = updateSites.addUpdateSite(dependency.toUpdateSite());
            return this.updateSite;
        }
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {

    }
}
