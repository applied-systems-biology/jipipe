package org.hkijena.acaq5.api.traits;

import com.google.common.reflect.TypeToken;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link ACAQTraitDeclaration} generated from a Java class
 */
public class ACAQJavaTraitDeclaration extends ACAQMutableTraitDeclaration {

    /**
     * Creates a new instance
     *
     * @param id    The trait id
     * @param klass The trait class
     */
    public ACAQJavaTraitDeclaration(String id, Class<? extends ACAQTrait> klass) {

        if (klass.isInterface())
            throw new IllegalArgumentException("Trait class instances cannot be interfaces!");

        setTraitClass(klass);
        setDiscriminator(ACAQDiscriminator.class.isAssignableFrom(klass));
        setName(getNameOf(klass));
        setDescription(getDescriptionOf(klass));
        setId(id);
        setHidden(getIsHidden(klass));

        // Try to find inherited classes
        Map<Class<? extends ACAQTrait>, ACAQTraitDeclaration> idMap = new HashMap<>();
        for (ACAQTraitDeclaration declaration : ACAQTraitRegistry.getInstance().getRegisteredTraits().values()) {
            idMap.put(declaration.getTraitClass(), declaration);
        }
        Set<Class<? extends ACAQTrait>> inheritedTraitClasses = getInheritedTraitClasses(klass);
        inheritedTraitClasses.remove(ACAQTrait.class);
        inheritedTraitClasses.remove(ACAQDiscriminator.class);
        setInherited(inheritedTraitClasses.stream().map(idMap::get).filter(Objects::nonNull).collect(Collectors.toSet()));
    }

    @Override
    public ACAQTrait newInstance() {
        try {
            return getTraitClass().getConstructor(ACAQTraitDeclaration.class).newInstance(this);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ACAQTrait newInstance(String value) {
        if (!isDiscriminator())
            return newInstance();
        try {
            return getTraitClass().getConstructor(ACAQTraitDeclaration.class, String.class).newInstance(this, value);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<ACAQDependency> getDependencies() {
        Set<ACAQDependency> result = new HashSet<>();
        for (ACAQTraitDeclaration declaration : getInherited()) {
            result.addAll(declaration.getDependencies());
        }
        return result;
    }

    @Override
    public String toString() {
        return "Trait type: " + getId();
    }

    /**
     * Returns the name of a trait
     *
     * @param klass trait class
     * @return name
     */
    public static String getNameOf(Class<? extends ACAQTrait> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if (annotations.length > 0) {
            return annotations[0].name();
        } else {
            return klass.getSimpleName();
        }
    }

    /**
     * Returns the description of a trait
     *
     * @param klass trait class
     * @return name
     */
    public static String getDescriptionOf(Class<? extends ACAQTrait> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if (annotations.length > 0) {
            return annotations[0].description();
        } else {
            return null;
        }
    }

    /**
     * Returns all inherited traits
     *
     * @param klass trait class
     * @return name
     */
    static Set<Class<? extends ACAQTrait>> getInheritedTraitClasses(Class<? extends ACAQTrait> klass) {
        Set<Class<? extends ACAQTrait>> result = new HashSet<>();
        for (TypeToken<?> type : TypeToken.of(klass).getTypes()) {
            if (type.getRawType() != klass && ACAQTrait.class.isAssignableFrom(type.getRawType()) && !type.getRawType().isInterface()) {
                result.add((Class<? extends ACAQTrait>) type.getRawType());
            }
        }
        return result;
    }

    /**
     * Returns true if the trait is hidden from the user
     *
     * @param klass The trait class
     * @return If the trait should be hidden
     */
    static boolean getIsHidden(Class<? extends ACAQTrait> klass) {
        return klass.getAnnotationsByType(ACAQHidden.class).length > 0;
    }


}
