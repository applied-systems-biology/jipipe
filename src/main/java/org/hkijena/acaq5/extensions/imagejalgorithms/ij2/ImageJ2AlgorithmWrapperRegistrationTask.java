package org.hkijena.acaq5.extensions.imagejalgorithms.ij2;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistrationTask;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.extensions.imagejalgorithms.ImageJAlgorithmsExtension;
import org.hkijena.acaq5.extensions.imagejdatatypes.datatypes.ImagePlusData;

/**
 * Register an {@link ImageJ2AlgorithmWrapperDeclaration}
 */
public class ImageJ2AlgorithmWrapperRegistrationTask implements ACAQAlgorithmRegistrationTask {

    boolean alreadyRegistered = false;
    private ImageJAlgorithmsExtension source;
    private ImageJ2AlgorithmWrapperDeclaration declaration;

    /**
     * @param source      registering extension
     * @param declaration the registered declaration
     */
    public ImageJ2AlgorithmWrapperRegistrationTask(ImageJAlgorithmsExtension source, ImageJ2AlgorithmWrapperDeclaration declaration) {
        this.source = source;
        this.declaration = declaration;
    }

    @Override
    public void register() {
        if (alreadyRegistered)
            return;
        alreadyRegistered = true;
        declaration.initialize();
        ACAQAlgorithmRegistry.getInstance().register(declaration, source);
    }

    @Override
    public boolean canRegister() {
        return ACAQDatatypeRegistry.getInstance().hasDataType(ImagePlusData.class);
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (!ACAQDatatypeRegistry.getInstance().hasDataType(ImagePlusData.class)) {
            report.reportIsInvalid("Required data types are not registered! Requires: " + ImagePlusData.class);
        }
    }
}
