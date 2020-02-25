package org.hkijena.acaq5.api.batchimporter.traits;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.HiddenTrait;

@ACAQDocumentation(name = "Project sample", description = "Indicates that a filesystem entry is associated to a sample")
@HiddenTrait
public interface ProjectSampleTrait extends ACAQTrait {
}
