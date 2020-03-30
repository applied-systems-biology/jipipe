package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQOrganization;
import org.hkijena.acaq5.api.data.traits.*;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Set;

/**
 * {@link ACAQAlgorithmDeclaration} for an algorithm that is defined in Java code
 * All necessary properties are extracted from class attributes
 */
@JsonSerialize(using = ACAQJavaAlgorithmDeclaration.Serializer.class)
public class ACAQJavaAlgorithmDeclaration extends ACAQMutableAlgorithmDeclaration {

    public ACAQJavaAlgorithmDeclaration(String id, Class<? extends ACAQAlgorithm> algorithmClass) {
        setAlgorithmClass(algorithmClass);
        setId(id);
        setName(getNameOf(algorithmClass));
        setDescription(getDescriptionOf(algorithmClass));
        setCategory(getCategoryOf(algorithmClass));
        setMenuPath(getMenuPathOf(algorithmClass));
        initializeSlots();
        initializeTraits();
    }

    private void initializeSlots() {
        for (AlgorithmInputSlot slot : getAlgorithmClass().getAnnotationsByType(AlgorithmInputSlot.class)) {
            getInputSlots().add(slot);
        }
        for (AlgorithmOutputSlot slot : getAlgorithmClass().getAnnotationsByType(AlgorithmOutputSlot.class)) {
            getOutputSlots().add(slot);
        }
    }

    private void initializeTraits() {
        for (GoodForTrait trait : getAlgorithmClass().getAnnotationsByType(GoodForTrait.class)) {
            getPreferredTraits().add(ACAQTraitRegistry.getInstance().getDeclarationById(trait.value()));
        }
        for (BadForTrait trait : getAlgorithmClass().getAnnotationsByType(BadForTrait.class)) {
            getUnwantedTraits().add(ACAQTraitRegistry.getInstance().getDeclarationById(trait.value()));
        }
        for (AddsTrait addsTrait : getAlgorithmClass().getAnnotationsByType(AddsTrait.class)) {
            getSlotTraitConfiguration().set(
                    ACAQTraitRegistry.getInstance().getDeclarationById(addsTrait.value()),
                    ACAQTraitModificationOperation.Add
            );
        }
        for (RemovesTrait removesTrait : getAlgorithmClass().getAnnotationsByType(RemovesTrait.class)) {
            getSlotTraitConfiguration().set(
                    ACAQTraitRegistry.getInstance().getDeclarationById(removesTrait.value()),
                    ACAQTraitModificationOperation.RemoveCategory
            );
        }
    }

    @Override
    public ACAQAlgorithm clone(ACAQAlgorithm algorithm) {
        try {
            return ConstructorUtils.getMatchingAccessibleConstructor(getAlgorithmClass(), algorithm.getClass()).newInstance(algorithm);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<ACAQDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public ACAQAlgorithm newInstance() {
        try {
            return getAlgorithmClass().getConstructor(ACAQAlgorithmDeclaration.class).newInstance(this);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the name of an algorithm
     *
     * @param klass
     * @return
     */
    public static String getNameOf(Class<? extends ACAQAlgorithm> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if (annotations.length > 0) {
            return annotations[0].name();
        } else {
            return klass.getSimpleName();
        }
    }

    /**
     * Returns the description of an algorithm
     *
     * @param klass
     * @return
     */
    public static String getDescriptionOf(Class<? extends ACAQAlgorithm> klass) {
        ACAQDocumentation[] annotations = klass.getAnnotationsByType(ACAQDocumentation.class);
        if (annotations.length > 0) {
            return annotations[0].description();
        } else {
            return null;
        }
    }

    /**
     * Returns the category of an algorithm
     *
     * @param klass
     * @return
     */
    public static ACAQAlgorithmCategory getCategoryOf(Class<? extends ACAQAlgorithm> klass) {
        AlgorithmMetadata[] annotations = klass.getAnnotationsByType(AlgorithmMetadata.class);
        if (annotations.length > 0) {
            return annotations[0].category();
        } else {
            return ACAQAlgorithmCategory.Internal;
        }
    }

    /**
     * Returns the menu path of the algorithm
     *
     * @param klass
     * @return
     */
    static String getMenuPathOf(Class<? extends ACAQAlgorithm> klass) {
        ACAQOrganization[] annotations = klass.getAnnotationsByType(ACAQOrganization.class);
        if (annotations.length > 0) {
            return annotations[0].menuPath();
        } else {
            return "";
        }
    }

    public static class Serializer extends JsonSerializer<ACAQJavaAlgorithmDeclaration> {
        @Override
        public void serialize(ACAQJavaAlgorithmDeclaration declaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("acaq:algorithm-class", declaration.getAlgorithmClass().getCanonicalName());
            jsonGenerator.writeEndObject();
        }
    }
}
