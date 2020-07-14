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

package org.hkijena.jipipe.ui.extensionbuilder;

import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.grouping.JsonNodeInfo;
import org.hkijena.jipipe.api.registries.JIPipeAlgorithmRegistry;
import org.hkijena.jipipe.utils.StringUtils;

/**
 * Supplies additional validation only for projects
 */
public class JIPipeJsonExtensionProjectValidation implements JIPipeValidatable {
    private final JIPipeJsonExtension extension;

    /**
     * Creates a new instance
     *
     * @param extension the extension
     */
    public JIPipeJsonExtensionProjectValidation(JIPipeJsonExtension extension) {
        this.extension = extension;
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        extension.reportValidity(report);
        for (JsonNodeInfo info : extension.getNodeInfos()) {
            if (!StringUtils.isNullOrEmpty(info.getId())) {
                if (JIPipeAlgorithmRegistry.getInstance().hasAlgorithmWithId(info.getId())) {
                    report.forCategory("Algorithms").forCategory(info.getName()).reportIsInvalid("Already registered: " + info.getId(),
                            "Currently there is already an algorithm with the same ID.",
                            "If this is intenional, you do not need to do something. If not, please assign an unique identifier.",
                            info);
                }
            }
        }
    }
}
