package org.hkijena.acaq5.extensions.standardalgorithms;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.annotation.AnnotateAll;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.annotation.RemoveAnnotations;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.annotation.SplitByAnnotation;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.processors.*;
import org.hkijena.acaq5.extensions.standardalgorithms.api.registries.GraphWrapperAlgorithmRegistrationTask;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.Set;

/**
 * Provides some standard algorithms
 */
@Plugin(type = ACAQJavaExtension.class)
public class StandardAlgorithmsExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Standard algorithms";
    }

    @Override
    public String getDescription() {
        return "A set of standard algorithms to handle various ACAQ5-specific workloads";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:standard-algorithms";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {

        registerAlgorithm("enhance-merge-slots", MergeDataEnhancer.class);
        registerAlgorithm("annotate-all", AnnotateAll.class);
        registerAlgorithm("annotate-remove", RemoveAnnotations.class);
        registerAlgorithm("annotate-split-by-annotation", SplitByAnnotation.class);

        registerAlgorithmResources();
    }

    private void registerAlgorithmResources() {
        Set<String> algorithmFiles = ResourceUtils.walkInternalResourceFolder("extensions/standardalgorithms/api/algorithms");
        for (String resourceFile : algorithmFiles) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readValue(ResourceUtils.class.getResource(resourceFile), JsonNode.class);
                GraphWrapperAlgorithmRegistrationTask task = new GraphWrapperAlgorithmRegistrationTask(node, this);
                registerAlgorithm(task);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
