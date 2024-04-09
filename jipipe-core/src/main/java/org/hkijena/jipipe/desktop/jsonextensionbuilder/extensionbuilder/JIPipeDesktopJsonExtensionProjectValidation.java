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

package org.hkijena.jipipe.desktop.jsonextensionbuilder.extensionbuilder;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJsonPlugin;
import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.JsonNodeInfoValidationReportContext;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

/**
 * Supplies additional validation only for projects
 */
public class JIPipeDesktopJsonExtensionProjectValidation implements JIPipeValidatable {
    private final JIPipeJsonPlugin extension;

    /**
     * Creates a new instance
     *
     * @param extension the extension
     */
    public JIPipeDesktopJsonExtensionProjectValidation(JIPipeJsonPlugin extension) {
        this.extension = extension;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        extension.reportValidity(reportContext, report);
        for (JsonNodeInfo info : extension.getNodeInfos()) {
            if (!StringUtils.isNullOrEmpty(info.getId())) {
                if (JIPipe.getNodes().hasNodeInfoWithId(info.getId())) {
                    report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Warning, new JsonNodeInfoValidationReportContext(info),
                            "Already registered: " + info.getId(),
                            "Currently there is already an algorithm with the same ID.",
                            "If this is intentional, you do not need to do something. If not, please assign an unique identifier.",
                            JsonUtils.toPrettyJsonString(info)));
                }
            }
        }
    }
}
