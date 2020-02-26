package org.hkijena.acaq5.api.compartments.datatypes;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;

@ACAQDocumentation(name = "Preprocessing output", description = "Output of each sample's preprocessing steps")
public class ACAQPreprocessingOutputData implements ACAQData {
    @Override
    public void saveTo(Path storageFilePath, String name) {

    }
}
