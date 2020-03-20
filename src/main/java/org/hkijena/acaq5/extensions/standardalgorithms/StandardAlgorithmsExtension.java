package org.hkijena.acaq5.extensions.standardalgorithms;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
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
import org.scijava.service.AbstractService;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Plugin(type = ACAQExtensionService.class)
public class StandardAlgorithmsExtension extends AbstractService implements ACAQExtensionService {
    @Override
    public String getName() {
        return "ACAQ5 standard library";
    }

    @Override
    public String getDescription() {
        return "Standard data types and algorithms";
    }

    @Override
    public List<String> getAuthors() {
        return Arrays.asList("Zoltán Cseresnyés", "Ruman Gerst");
    }

    @Override
    public String getURL() {
        return "https://applied-systems-biology.github.io/acaq5/";
    }

    @Override
    public String getLicense() {
        return "BSD-2";
    }

    @Override
    public URL getIconURL() {
        return ResourceUtils.getPluginResource("logo-400.png");
    }

    @Override
    public void register(ACAQRegistryService registryService) {

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

    private void registerAlgorithmResources(ACAQRegistryService registryService) {
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
