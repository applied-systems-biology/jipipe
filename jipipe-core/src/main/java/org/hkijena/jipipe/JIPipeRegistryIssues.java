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

import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.scijava.plugin.PluginInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class that describes issues with the current registration process
 */
public class JIPipeRegistryIssues implements JIPipeValidatable {
    private Set<JIPipeImageJUpdateSiteDependency> missingImageJSites = new HashSet<>();
    private Set<PluginInfo<JIPipeJavaExtension>> erroneousPlugins = new HashSet<>();
    private Set<Class<? extends JIPipeData>> erroneousDataTypes = new HashSet<>();
    private Set<JIPipeParameterTypeInfo> erroneousParameterTypes = new HashSet<>();
    private Set<JIPipeNodeInfo> erroneousNodes = new HashSet<>();
    private Map<String, JIPipeValidationReport> preActivationIssues = new HashMap<>();

    @Override
    public void reportValidity(JIPipeValidationReportContext context, JIPipeValidationReport report) {
        for (Map.Entry<String, JIPipeValidationReport> entry : preActivationIssues.entrySet()) {
            report.addAll(entry.getValue());
        }
        for (JIPipeImageJUpdateSiteDependency site : missingImageJSites) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new CustomValidationReportContext(context, "ImageJ update site checker"),
                    "Missing ImageJ site: " + site.getName(),
                    String.format("An extension requests following ImageJ site to be activated: '%s' (%s)", site.getName(), site.getUrl()),
                    "Please activate the site in the update manager."));
        }
        for (PluginInfo<JIPipeJavaExtension> plugin : erroneousPlugins) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new CustomValidationReportContext(context, "Extension initialization"),
                    "Could not load extension '" + plugin.getIdentifier() + "'",
                    "There was an error while loading an extension.",
                    "Please install necessary dependencies via ImageJ. Then restart  ImageJ.",
                    plugin.toString()));
        }
        for (JIPipeNodeInfo info : erroneousNodes) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new CustomValidationReportContext(context, "Node initialization"),
                    "Invalid node type '" + info.getName() + "'",
                    "There was an error while loading a node type.",
                    "Please install necessary dependencies via ImageJ. Then restart ImageJ.",
                    info.toString()));
        }
        for (Class<? extends JIPipeData> dataType : erroneousDataTypes) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new CustomValidationReportContext(context, "Data type initialization"),
                    "Invalid data type '" + dataType + "'",
                    "There was an error while loading a data type.",
                    "Please install necessary dependencies via ImageJ. Then restart ImageJ.",
                    dataType.getCanonicalName()));
        }
        for (JIPipeParameterTypeInfo parameterType : erroneousParameterTypes) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new CustomValidationReportContext(context, "Parameter type initialization"),
                    "Invalid parameter type '" + parameterType.getId() + "'",
                    "There was an error while loading a parameter type.",
                    "Please install necessary dependencies via ImageJ. Then restart ImageJ.",
                    parameterType.getFieldClass().getCanonicalName()));
        }
    }

    public Map<String, JIPipeValidationReport> getPreActivationIssues() {
        return preActivationIssues;
    }

    public void setPreActivationIssues(Map<String, JIPipeValidationReport> preActivationIssues) {
        this.preActivationIssues = preActivationIssues;
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

    public Set<JIPipeParameterTypeInfo> getErroneousParameterTypes() {
        return erroneousParameterTypes;
    }

    public void setErroneousParameterTypes(Set<JIPipeParameterTypeInfo> erroneousParameterTypes) {
        this.erroneousParameterTypes = erroneousParameterTypes;
    }

    public Set<JIPipeNodeInfo> getErroneousNodes() {
        return erroneousNodes;
    }

    public void setErroneousNodes(Set<JIPipeNodeInfo> erroneousNodes) {
        this.erroneousNodes = erroneousNodes;
    }

    public Set<Class<? extends JIPipeData>> getErroneousDataTypes() {
        return erroneousDataTypes;
    }

    public void setErroneousDataTypes(Set<Class<? extends JIPipeData>> erroneousDataTypes) {
        this.erroneousDataTypes = erroneousDataTypes;
    }
}
