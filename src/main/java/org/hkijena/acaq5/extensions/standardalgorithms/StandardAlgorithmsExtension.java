package org.hkijena.acaq5.extensions.standardalgorithms;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.annotation.AnnotateAll;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.annotation.RemoveAnnotations;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.annotation.SplitByAnnotation;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.converters.MaskToParticleConverter;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.converters.MultiChannelSplitterConverter;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers.CLAHEImageEnhancer;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers.IlluminationCorrectionEnhancer;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers.MergeROIEnhancer;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.enhancers.WatershedMaskEnhancer;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.segmenters.*;
import org.hkijena.acaq5.extensions.standardalgorithms.api.registries.GraphWrapperAlgorithmRegistrationTask;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.util.Set;

@Plugin(type = ACAQJavaExtension.class)
public class StandardAlgorithmsExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Standard algorithms";
    }

    @Override
    public String getDescription() {
        return "A set of standard image processing algorithms";
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

        registerAlgorithm("convert-imagej-analyze-particles", MaskToParticleConverter.class);
        registerAlgorithm("enhance-imagej-clahe", CLAHEImageEnhancer.class);
        registerAlgorithm("enhance-imagej-illumination-correction", IlluminationCorrectionEnhancer.class);
        registerAlgorithm("enhance-imagej-watershed", WatershedMaskEnhancer.class);
        registerAlgorithm("enhance-imagej-auto-threshold", AutoThresholdSegmenter.class);
        registerAlgorithm("segment-imagej-bright-spots", BrightSpotsSegmenter.class);
        registerAlgorithm("segment-imagej-hough", HoughSegmenter.class);
        registerAlgorithm("segment-imagej-internal-gradient", InternalGradientSegmenter.class);
        registerAlgorithm("convert-imagej-multichannel-splitter", MultiChannelSplitterConverter.class);
        registerAlgorithm("enhance-imagej-merge-roi", MergeROIEnhancer.class);
        registerAlgorithm("segment-imagej-hessian", HessianSegmenter.class);
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
