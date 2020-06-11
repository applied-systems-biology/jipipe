package org.hkijena.acaq5.extensions.parameters.pairs;

import org.hkijena.acaq5.extensions.parameters.collections.ListParameter;
import org.hkijena.acaq5.extensions.parameters.predicates.StringPredicate;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A parameter that renames a matching string into another string
 */
public class ACAQTraitDeclarationRefAndStringPredicatePair extends Pair<ACAQTraitDeclarationRef, StringPredicate>  {

    /**
     * Creates a new instance
     */
    public ACAQTraitDeclarationRefAndStringPredicatePair() {
        super(ACAQTraitDeclarationRef.class, StringPredicate.class);
        setKey(new ACAQTraitDeclarationRef());
        setValue(new StringPredicate());
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public ACAQTraitDeclarationRefAndStringPredicatePair(ACAQTraitDeclarationRefAndStringPredicatePair other) {
        super(other);
    }

    /**
     * A collection of multiple {@link ACAQTraitDeclarationRefAndStringPredicatePair}
     */
    public static class List extends ListParameter<ACAQTraitDeclarationRefAndStringPredicatePair> {
        /**
         * Creates a new instance
         */
        public List() {
            super(ACAQTraitDeclarationRefAndStringPredicatePair.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(List other) {
            super(ACAQTraitDeclarationRefAndStringPredicatePair.class);
            for (ACAQTraitDeclarationRefAndStringPredicatePair filter : other) {
                add(new ACAQTraitDeclarationRefAndStringPredicatePair(filter));
            }
        }
    }
}
