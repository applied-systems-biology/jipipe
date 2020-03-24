package org.hkijena.acaq5.extensions.standardalgorithms;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQDefaultRegistry;
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
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro.MacroWrapperAlgorithm;
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
    public void register(ACAQDefaultRegistry registryService) {

        registryService.getAlgorithmRegistry().register(MaskToParticleConverter.class);
        registryService.getAlgorithmRegistry().register(CLAHEImageEnhancer.class);
        registryService.getAlgorithmRegistry().register(IlluminationCorrectionEnhancer.class);
        registryService.getAlgorithmRegistry().register(WatershedMaskEnhancer.class);
        registryService.getAlgorithmRegistry().register(AutoThresholdSegmenter.class);
        registryService.getAlgorithmRegistry().register(BrightSpotsSegmenter.class);
        registryService.getAlgorithmRegistry().register(HoughSegmenter.class);
        registryService.getAlgorithmRegistry().register(InternalGradientSegmenter.class);
        registryService.getAlgorithmRegistry().register(MultiChannelSplitterConverter.class);
        registryService.getAlgorithmRegistry().register(MergeROIEnhancer.class);
        registryService.getAlgorithmRegistry().register(HessianSegmenter.class);
        registryService.getAlgorithmRegistry().register(AnnotateAll.class);
        registryService.getAlgorithmRegistry().register(RemoveAnnotations.class);
        registryService.getAlgorithmRegistry().register(SplitByAnnotation.class);
        registryService.getAlgorithmRegistry().register(MacroWrapperAlgorithm.class);

        registerAlgorithmResources(registryService);
    }

    private void registerAlgorithmResources(ACAQDefaultRegistry registryService) {
        Set<String> algorithmFiles = ResourceUtils.walkInternalResourceFolder("extensions/standardalgorithms/api/algorithms");
        for (String resourceFile : algorithmFiles) {
            try {
                JsonNode node = JsonUtils.getObjectMapper().readValue(ResourceUtils.class.getResource(resourceFile), JsonNode.class);
                GraphWrapperAlgorithmRegistrationTask task = new GraphWrapperAlgorithmRegistrationTask(node);
                registryService.getAlgorithmRegistry().scheduleRegister(task);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
