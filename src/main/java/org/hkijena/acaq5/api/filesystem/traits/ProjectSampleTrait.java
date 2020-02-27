package org.hkijena.acaq5.api.filesystem.traits;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.HiddenTrait;

@ACAQDocumentation(name = "Project sample", description = "Indicates that a filesystem entry is associated to a sample")
@HiddenTrait
public interface ProjectSampleTrait extends ACAQTrait {
    String FILESYSTEM_ANNOTATION_SAMPLE = "project-sample";
}
