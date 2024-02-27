/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.omero.parameters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalTextAnnotationNameParameter;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;

public class AnnotationsToOMEROTagExporter extends AbstractJIPipeParameterCollection {
    private OptionalTextAnnotationNameParameter tagListAnnotation = new OptionalTextAnnotationNameParameter("OMERO:Tags", true);

    public AnnotationsToOMEROTagExporter() {

    }

    public AnnotationsToOMEROTagExporter(AnnotationsToOMEROTagExporter other) {
        this.tagListAnnotation = new OptionalTextAnnotationNameParameter(other.tagListAnnotation);
    }

    @SetJIPipeDocumentation(name = "Tag list annotation", description = "If enabled, extract all tag names from a single annotation that contains a JSON-serialized list of names. If no JSON data is found, " +
            "the whole annotation is converted into a tag. Nested JSON arrays are flattened. Numbers are converted into strings. JSON objects are ignored.")
    @JIPipeParameter("tag-list-annotation")
    public OptionalTextAnnotationNameParameter getTagListAnnotation() {
        return tagListAnnotation;
    }

    @JIPipeParameter("tag-list-annotation")
    public void setTagListAnnotation(OptionalTextAnnotationNameParameter tagListAnnotation) {
        this.tagListAnnotation = tagListAnnotation;
    }

    public void createTags(Set<String> target, Collection<JIPipeTextAnnotation> annotations) {
        if(tagListAnnotation.isEnabled()) {
            for (JIPipeTextAnnotation annotation : annotations) {
                if(Objects.equals(tagListAnnotation.getContent(), annotation.getName())) {
                    try {
                        JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(annotation.getValue());
                        createTags(target, node);
                    } catch (JsonProcessingException e) {
                        target.add(annotation.getValue());
                    }
                }
            }
        }
    }

    private void createTags(Set<String> target, JsonNode node) {
        if(node.isArray()) {
            for (JsonNode child : ImmutableList.copyOf(node.elements())) {
                createTags(target, child);
            }
        }
        else if(node.isTextual()) {
            target.add(node.textValue());
        }
        else if(node.isNumber()) {
            target.add(String.valueOf(node.doubleValue()));
        }
//        else {
//            target.add(JsonUtils.toJsonString(node));
//        }
    }
}
