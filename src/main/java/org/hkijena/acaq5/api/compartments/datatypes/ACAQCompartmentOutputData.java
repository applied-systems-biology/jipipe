package org.hkijena.acaq5.api.compartments.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;

@ACAQDocumentation(name = "Output data", description = "Output of a compartment")
@ACAQHidden
public class ACAQCompartmentOutputData implements ACAQData {
    @Override
    public void saveTo(Path storageFilePath, String name) {

    }
}
