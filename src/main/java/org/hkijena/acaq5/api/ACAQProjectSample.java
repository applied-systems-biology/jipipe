package org.hkijena.acaq5.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.ACAQAlgorithmInputDeclaration;
import org.hkijena.acaq5.api.compartments.ACAQPreprocessingOutputDeclaration;
import org.hkijena.acaq5.api.compartments.ACAQPreprocessingOutputSlotConfiguration;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQAlgorithmInput;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQPreprocessingOutput;

import java.io.IOException;

/**
 * Sample within an {@link ACAQProject}
 */
@JsonSerialize(using = ACAQProjectSample.Serializer.class)
public class ACAQProjectSample implements Comparable<ACAQProjectSample>, ACAQValidatable {
    private ACAQProject project;
    private String name;
    private ACAQPreprocessingOutput preprocessingOutput;

    public ACAQProjectSample(ACAQProject project, String name) {
        this.project = project;
        this.name = name;
        initializePreprocessingGraph();
    }

    public ACAQProjectSample(ACAQProjectSample other) {
        this.name = other.name;
        this.project = other.project;
    }

    private void initializePreprocessingGraph() {
        preprocessingOutput = ACAQPreprocessingOutputDeclaration.createInstance(new ACAQPreprocessingOutputSlotConfiguration(project.getPreprocessingOutputConfiguration()),
                project.getPreprocessingTraitConfiguration());
        insertNode(preprocessingOutput);
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
    public int compareTo(ACAQProjectSample o) {
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

    public ACAQPreprocessingOutput getPreprocessingOutput() {
        return preprocessingOutput;
    }

    public static class Serializer extends JsonSerializer<ACAQProjectSample> {
        @Override
        public void serialize(ACAQProjectSample acaqProjectSample, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
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
