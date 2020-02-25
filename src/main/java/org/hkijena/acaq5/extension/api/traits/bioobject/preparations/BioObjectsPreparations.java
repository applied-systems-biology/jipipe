package org.hkijena.acaq5.extension.api.traits.bioobject.preparations;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.HiddenTrait;
import org.hkijena.acaq5.extension.api.traits.bioobject.BioObjects;

/**
 * Base interface for all sample preparations
 */
@HiddenTrait
@ACAQDocumentation(name = "Sample preparation")
public interface BioObjectsPreparations extends BioObjects {
}
