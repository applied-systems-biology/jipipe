package org.hkijena.acaq5.api.algorithm;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.AddsTrait;
import org.hkijena.acaq5.api.traits.BadForTrait;
import org.hkijena.acaq5.api.traits.GoodForTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * {@link ACAQAlgorithmDeclaration} for an algorithm that is defined in Java code
 * All necessary properties are extracted from class attributes
 */
public class ACAQDefaultAlgorithmDeclaration extends ACAQMutableAlgorithmDeclaration {

    public ACAQDefaultAlgorithmDeclaration(Class<? extends ACAQAlgorithm> algorithmClass) {
        setAlgorithmClass(algorithmClass);
        setName(getNameOf(algorithmClass));
        setDescription(getDescriptionOf(algorithmClass));
        setCategory(getCategoryOf(algorithmClass));

        initializeSlots();
        initializeTraits();
    }

    private void initializeSlots() {
        for(AlgorithmInputSlot slot : getAlgorithmClass().getAnnotationsByType(AlgorithmInputSlot.class)) {
            getInputSlots().add(slot);
        }
        for(AlgorithmOutputSlot slot : getAlgorithmClass().getAnnotationsByType(AlgorithmOutputSlot.class)) {
            getOutputSlots().add(slot);
        }
    }

    private void initializeTraits() {
        for(GoodForTrait trait : getAlgorithmClass().getAnnotationsByType(GoodForTrait.class)) {
            getPreferredTraits().add(trait.value());
        }
        for(BadForTrait trait : getAlgorithmClass().getAnnotationsByType(BadForTrait.class)) {
            getUnwantedTraits().add(trait.value());
        }
        getAddedTraits().addAll(Arrays.asList(getAlgorithmClass().getAnnotationsByType(AddsTrait.class)));
        getRemovedTraits().addAll(Arrays.asList(getAlgorithmClass().getAnnotationsByType(RemovesTrait.class)));
    }

    @Override
    public ACAQAlgorithm clone(ACAQAlgorithm algorithm) {
        return null;
    }

    @Override
    public boolean matches(JsonNode node) {
        JsonNode classNode = node.path("acaq:algorithm-class");
        if(!classNode.isMissingNode()) {
            String className = classNode.asText();
            return getAlgorithmClass().getCanonicalName().equals(className);
        }
        return false;
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
     * Returns the name of an algorithm
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

    public static class Serializer extends JsonSerializer<ACAQDefaultAlgorithmDeclaration> {
        @Override
        public void serialize(ACAQDefaultAlgorithmDeclaration declaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeObjectField("acaq:algorithm-class", declaration.getAlgorithmClass().getCanonicalName());
        }
    }
}
