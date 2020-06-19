package org.hkijena.acaq5.ui.extensionbuilder;

import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.grouping.JsonAlgorithmDeclaration;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.utils.StringUtils;

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
