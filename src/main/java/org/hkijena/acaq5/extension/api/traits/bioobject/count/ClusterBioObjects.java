package org.hkijena.acaq5.extension.api.traits.bioobject.count;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Clusters", description = "A cluster of multiple objects that cannot be trivially separated")
public class ClusterBioObjects extends BioObjectsCount {
    public ClusterBioObjects(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
