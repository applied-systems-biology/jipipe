package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.data.ACAQData;

import java.nio.file.Path;

/**
 * Structural data for trait inheritance
 */
@ACAQDocumentation(name = "Inheritance", description = "An inheritance between annotation types")
@ACAQHidden
public class ACAQTraitNodeInheritanceData implements ACAQData {
    @Override
    public void saveTo(Path storageFilePath, String name) {

    }

    @Override
    public ACAQData duplicate() {
        return new ACAQTraitNodeInheritanceData();
    }
}
