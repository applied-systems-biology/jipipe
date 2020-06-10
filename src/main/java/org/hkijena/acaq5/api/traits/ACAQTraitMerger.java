package org.hkijena.acaq5.api.traits;

import java.util.*;

/**
 * Helper for merging traits
 */
public class ACAQTraitMerger {
    private Map<ACAQTraitDeclaration, ACAQTrait> data = new HashMap<>();
    private Set<ACAQTrait> addedTraits = new HashSet<>();

    /**
     * Merges a trait into the merger
     *
     * @param trait the trait
     */
    public void add(ACAQTrait trait) {
        if (trait == null)
            return;
        if (addedTraits.contains(trait))
            return;
        ACAQTrait existing = data.getOrDefault(trait.getDeclaration(), null);
        if (existing != null) {
            String value = existing.getValue();
            if (value == null) {
                value = trait.getValue();
            } else {
                value = value + "; " + trait.getValue();
            }
            existing = trait.getDeclaration().newInstance(value);
        } else {
            existing = trait;
        }
        addedTraits.add(trait);
        data.put(existing.getDeclaration(), existing);
    }

    /**
     * Removes a trait type from the merger
     *
     * @param declaration the type
     */
    public void removeAnnotationType(ACAQTraitDeclaration declaration) {
        data.remove(declaration);
    }

    public Set<ACAQTraitDeclaration> getAnnotationTypes() {
        return data.keySet();
    }

    public List<ACAQTrait> getResult() {
        return new ArrayList<>(data.values());
    }

    /**
     * Adds multiple annotations
     *
     * @param annotations list of annotations
     */
    public void addAll(Collection<ACAQTrait> annotations) {
        for (ACAQTrait annotation : annotations) {
            add(annotation);
        }
    }
}
