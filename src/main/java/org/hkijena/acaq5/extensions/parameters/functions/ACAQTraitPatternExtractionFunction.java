package org.hkijena.acaq5.extensions.parameters.functions;

import org.hkijena.acaq5.extensions.annotation.algorithms.ExtractAndReplaceAnnotation;
import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.patterns.StringPatternExtraction;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

/**
 * A function entry that applies pattern extraction to an {@link ACAQTraitDeclarationRef}
 */
public class ACAQTraitPatternExtractionFunction extends FunctionParameter<ACAQTraitDeclarationRef, StringPatternExtraction, ACAQTraitDeclarationRef> {

    /**
     * Creates a new instance
     */
    public ACAQTraitPatternExtractionFunction() {
        super(ACAQTraitDeclarationRef.class, StringPatternExtraction.class, ACAQTraitDeclarationRef.class);
        setInput(new ACAQTraitDeclarationRef());
        setOutput(new ACAQTraitDeclarationRef());
        setParameter(new StringPatternExtraction());
    }

    /**
     * Creates a copy
     * @param other the original
     */
    public ACAQTraitPatternExtractionFunction(ACAQTraitPatternExtractionFunction other) {
        super(other);
    }

    /**
     * List of {@link ACAQTraitPatternExtractionFunction}
     */
    public static class List extends ListParameter<ACAQTraitPatternExtractionFunction> {
        /**
         * Creates new instance
         */
        public List() {
            super(ACAQTraitPatternExtractionFunction.class);
        }

        /**
         * Creates a copy
         * @param other the original
         */
        public List(List other) {
            super(ACAQTraitPatternExtractionFunction.class);
            for (ACAQTraitPatternExtractionFunction function : other) {
                add(new ACAQTraitPatternExtractionFunction(function));
            }
        }
    }
}
