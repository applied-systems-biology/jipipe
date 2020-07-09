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

package org.hkijena.pipelinej.ui.extensionbuilder;

import org.hkijena.pipelinej.ACAQJsonExtension;
import org.hkijena.pipelinej.api.ACAQValidatable;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.grouping.JsonAlgorithmDeclaration;
import org.hkijena.pipelinej.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.pipelinej.utils.StringUtils;

/**
 * Supplies additional validation only for projects
 */
public class ACAQJsonExtensionProjectValidation implements ACAQValidatable {
    private final ACAQJsonExtension extension;

    /**
     * Creates a new instance
     *
     * @param extension the extension
     */
    public ACAQJsonExtensionProjectValidation(ACAQJsonExtension extension) {
        this.extension = extension;
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        extension.reportValidity(report);
        for (JsonAlgorithmDeclaration declaration : extension.getAlgorithmDeclarations()) {
            if (!StringUtils.isNullOrEmpty(declaration.getId())) {
                if (ACAQAlgorithmRegistry.getInstance().hasAlgorithmWithId(declaration.getId())) {
                    report.forCategory("Algorithms").forCategory(declaration.getName()).reportIsInvalid("Already registered: " + declaration.getId(),
                            "Currently there is already an algorithm with the same ID.",
                            "If this is intenional, you do not need to do something. If not, please assign an unique identifier.",
                            declaration);
                }
            }
        }
    }
}
