package org.hkijena.acaq5.extensions.annotation;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.annotation.algorithms.AnnotateAll;
import org.hkijena.acaq5.extensions.annotation.algorithms.RemoveAnnotations;
import org.hkijena.acaq5.extensions.annotation.algorithms.SplitByAnnotation;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides data types and algorithms to modify data annotations
 */
@Plugin(type = ACAQJavaExtension.class)
public class AnnotationsExtension extends ACAQPrepackagedDefaultJavaExtension {
    @Override
    public String getName() {
        return "Annotation data types and algorithms";
    }

    @Override
    public String getDescription() {
        return "Provides data types and algorithms to modify data annotations";
    }

    @Override
    public void register() {
        registerAlgorithm("annotate-all", AnnotateAll.class);
        registerAlgorithm("annotate-remove", RemoveAnnotations.class);
        registerAlgorithm("annotate-split-by-annotation", SplitByAnnotation.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:annotations";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
