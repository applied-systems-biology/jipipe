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

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.scijava.plugin.PluginInfo;

import java.util.*;

/**
 * Contains reportable items that happen during initialization
 */
public class JIPipeInitializationReport implements JIPipeValidatable {
    private final Set<String> registeredExtensionIds = new HashSet<>();
    private final List<JIPipeDependency> registeredExtensions = new ArrayList<>();
    private final List<JIPipeDependency> failedExtensions = new ArrayList<>();
    private final List<Throwable> errors = new ArrayList<>();

    private final Set<JIPipeImageJUpdateSiteDependency> missingImageJSites = new HashSet<>();
    private final Set<PluginInfo<JIPipeJavaPlugin>> erroneousPlugins = new HashSet<>();
    private final Set<Class<? extends JIPipeData>> erroneousDataTypes = new HashSet<>();
    private final Set<JIPipeParameterTypeInfo> erroneousParameterTypes = new HashSet<>();
    private final Set<JIPipeNodeInfo> erroneousNodes = new HashSet<>();
    private final Map<String, JIPipeValidationReport> preActivationIssues = new HashMap<>();

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report) {
        for (Map.Entry<String, JIPipeValidationReport> entry : preActivationIssues.entrySet()) {
            report.addAll(entry.getValue());
        }
        for (JIPipeImageJUpdateSiteDependency site : missingImageJSites) {
            reportContext.custom("ImageJ update site checker").error().title("Missing ImageJ site: " + site.getName()).explanation(String.format("An extension requests following ImageJ site to be activated: '%s' (%s)", site.getName(), site.getUrl())).solution("Please activate the site in the update manager.").report(report);
        }
        for (PluginInfo<JIPipeJavaPlugin> plugin : erroneousPlugins) {
            reportContext.custom("Extension initialization").error().title("Could not load extension '" + plugin.getIdentifier() + "'").explanation("There was an error while loading an extension.").solution("Please install necessary dependencies via ImageJ. Then restart  ImageJ.").details(plugin.toString()).report(report);
        }
        for (JIPipeNodeInfo info : erroneousNodes) {
            reportContext.custom("Node initialization").error().title("Invalid node type '" + info.getName() + "'").explanation("There was an error while loading a node type.").solution("Please install necessary dependencies via ImageJ. Then restart ImageJ.").details(info.toString()).report(report);
        }
        for (Class<? extends JIPipeData> dataType : erroneousDataTypes) {
            reportContext.custom("Data type initialization").error().title("Invalid data type '" + dataType + "'").explanation("There was an error while loading a data type.").solution("Please install necessary dependencies via ImageJ. Then restart ImageJ.").details(dataType.getCanonicalName()).report(report);
        }
        for (JIPipeParameterTypeInfo parameterType : erroneousParameterTypes) {
            reportContext.custom("Parameter type initialization").error().title("Invalid parameter type '" + parameterType.getId() + "'").explanation("There was an error while loading a parameter type.").solution("Please install necessary dependencies via ImageJ. Then restart ImageJ.").details(parameterType.getFieldClass().getCanonicalName()).report(report);
        }
    }

    public Map<String, JIPipeValidationReport> getPreActivationIssues() {
        return preActivationIssues;
    }

    public Set<JIPipeImageJUpdateSiteDependency> getMissingImageJSites() {
        return missingImageJSites;
    }

    public Set<PluginInfo<JIPipeJavaPlugin>> getErroneousPlugins() {
        return erroneousPlugins;
    }

    public Set<JIPipeParameterTypeInfo> getErroneousParameterTypes() {
        return erroneousParameterTypes;
    }

    public Set<JIPipeNodeInfo> getErroneousNodes() {
        return erroneousNodes;
    }

    public Set<Class<? extends JIPipeData>> getErroneousDataTypes() {
        return erroneousDataTypes;
    }

    public List<Throwable> getErrors() {
        return errors;
    }

    public Set<String> getRegisteredExtensionIds() {
        return registeredExtensionIds;
    }

    public List<JIPipeDependency> getRegisteredExtensions() {
        return registeredExtensions;
    }

    public List<JIPipeDependency> getFailedExtensions() {
        return failedExtensions;
    }
}
