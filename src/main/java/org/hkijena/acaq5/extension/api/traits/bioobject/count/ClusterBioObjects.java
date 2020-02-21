package org.hkijena.acaq5.extension.api.traits.bioobject.count;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.extension.api.traits.bioobject.BioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.morphology.BioObjectsMorphology;

@ACAQDocumentation(name = "Cluster", description = "A cluster of multiple objects that cannot be trivially separated")
public interface ClusterBioObjects extends BioObjectsCount {
}
