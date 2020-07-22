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
 */

package org.hkijena.jipipe;

import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.scijava.plugin.PluginInfo;

import java.util.HashSet;
import java.util.Set;

/**
 * Class that describes issues with the current registration process
 */
public class JIPipeRegistryIssues implements JIPipeValidatable {
    private Set<JIPipeImageJUpdateSiteDependency> missingImageJSites = new HashSet<>();
    private Set<PluginInfo<JIPipeJavaExtension>> erroneousPlugins = new HashSet<>();
    private Set<JIPipeNodeInfo> erroneousNodes = new HashSet<>();

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        for (JIPipeImageJUpdateSiteDependency site : missingImageJSites) {
            report.forCategory("ImageJ dependencies").forCategory("Sites").forCategory(site.getName()).reportIsInvalid("Missing ImageJ site: " + site.getName(),
                    String.format("An extension requests following ImageJ site to be activated: '%s' (%s)", site.getName(), site.getURL()),
                    "Please activate the site in the update manager.",
                    this);
        }
        for (PluginInfo<JIPipeJavaExtension> plugin : erroneousPlugins) {
            report.forCategory("Java Extensions").forCategory(plugin.getIdentifier()).reportIsInvalid("Could not load extension '" + plugin.getIdentifier() + "'",
                    "There was an error while loading an extension.",
                    "Please install necessary dependencies via ImageJ. Then restart  ImageJ.",
                    plugin);
        }
        for (JIPipeNodeInfo info : erroneousNodes) {
            report.forCategory("Node types").forCategory(info.getId())
                    .reportIsInvalid("Invalid node type '" + info.getName() + "'",
                            "There was an error while loading a node type.",
                    "Please install necessary dependencies via ImageJ. Then restart  ImageJ.",
                    info);
        }
    }

    public Set<JIPipeImageJUpdateSiteDependency> getMissingImageJSites() {
        return missingImageJSites;
    }

    public void setMissingImageJSites(Set<JIPipeImageJUpdateSiteDependency> missingImageJSites) {
        this.missingImageJSites = missingImageJSites;
    }

    public Set<PluginInfo<JIPipeJavaExtension>> getErroneousPlugins() {
        return erroneousPlugins;
    }

    public void setErroneousPlugins(Set<PluginInfo<JIPipeJavaExtension>> erroneousPlugins) {
        this.erroneousPlugins = erroneousPlugins;
    }

    public Set<JIPipeNodeInfo> getErroneousNodes() {
        return erroneousNodes;
    }

    public void setErroneousNodes(Set<JIPipeNodeInfo> erroneousNodes) {
        this.erroneousNodes = erroneousNodes;
    }
}
