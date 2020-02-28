package org.hkijena.acaq5.api.traits;

import com.google.common.reflect.TypeToken;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class ACAQDefaultTraitDeclaration extends ACAQMutableTraitDeclaration {

    public ACAQDefaultTraitDeclaration(Class<? extends ACAQTrait> klass) {

        if(klass.isInterface())
            throw new IllegalArgumentException("Trait class instances cannot be interfaces!");

        setTraitClass(klass);
        setDiscriminator(ACAQDiscriminator.class.isAssignableFrom(klass));
        setName(getNameOf(klass));
        setDescription(getDescriptionOf(klass));
        setId(getDeclarationIdOf(klass));

        // Discover inherited traits
        for (Class<? extends ACAQTrait> inheritedTraitClass : getInheritedTraitClasses(klass)) {
            ACAQTraitDeclaration declaration = ACAQTraitRegistry.getInstance().getDefaultDeclarationFor(inheritedTraitClass);
            if(declaration == null) {
                ACAQTraitRegistry.getInstance().register(inheritedTraitClass);
                declaration = ACAQTraitRegistry.getInstance().getDefaultDeclarationFor(inheritedTraitClass);
            }
            getInherited().add(declaration);
        }
    }

    public static String getDeclarationIdOf(Class<? extends ACAQTrait> klass) {
        return "acaq:default:" + klass.getCanonicalName();
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
        if(!isDiscriminator())
            return newInstance();
        try {
            return getTraitClass().getConstructor(ACAQTraitDeclaration.class, String.class).newInstance(this, value);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the name of a trait
     *
     * @param klass
     * @return
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
     * @param klass
     * @return
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
     * @param klass
     * @return
     */
    static Set<Class<? extends ACAQTrait>> getInheritedTraitClasses(Class<? extends ACAQTrait> klass) {
        Set<Class<? extends ACAQTrait>> result = new HashSet<>();
        for(TypeToken<?> type : TypeToken.of(klass).getTypes().interfaces()) {
            if(!type.getRawType().isInterface()) {
                result.add((Class<? extends ACAQTrait>) type.getRawType());
            }
        }
        return result;
    }

    /**
     * Returns true if the trait is hidden from the user
     * @param klass
     * @return
     */
    static boolean isHidden(Class<? extends ACAQTrait> klass) {
        return klass.getAnnotationsByType(ACAQHidden.class).length > 0;
    }
}
