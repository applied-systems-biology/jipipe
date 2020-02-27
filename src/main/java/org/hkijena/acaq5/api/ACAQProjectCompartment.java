package org.hkijena.acaq5.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;

import java.io.IOException;

/**
 * Sample within an {@link ACAQProject}
 */
@JsonSerialize(using = ACAQProjectCompartment.Serializer.class)
public class ACAQProjectCompartment implements Comparable<ACAQProjectCompartment>, ACAQValidatable {
    private ACAQProject project;
    private String name;
    private ACAQCompartmentOutput compartmentOutputCache;

    public ACAQProjectCompartment(ACAQProject project, String name) {
        this.project = project;
        this.name = name;
    }

    public ACAQProjectCompartment(ACAQProjectCompartment other) {
        this.name = other.name;
        this.project = other.project;
    }

    public ACAQCompartmentOutput getOutputNode() {
        if(compartmentOutputCache == null) {
            for (ACAQAlgorithm algorithm : project.getGraph().getAlgorithmNodes().values()) {
                if(algorithm instanceof ACAQCompartmentOutput && name.equals(algorithm.getCompartment())) {
                    compartmentOutputCache = (ACAQCompartmentOutput) algorithm;
                    break;
                }
            }
        }
        return compartmentOutputCache;
    }

    public void insertNode(ACAQAlgorithm algorithm) {
        project.getGraph().insertNode(algorithm, getName());
    }

    public ACAQProject getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(ACAQProjectCompartment o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    /**
     * Only used internally by {@link ACAQProject}
     * @param name
     */
    void setName(String name) {
        this.name = name;
    }

    public static class Serializer extends JsonSerializer<ACAQProjectCompartment> {
        @Override
        public void serialize(ACAQProjectCompartment acaqProjectCompartment, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }
    }

    /**
     * Deserializes the sample's content from JSON
     * @param node
     */
    public void fromJson(JsonNode node) {
    }
}
