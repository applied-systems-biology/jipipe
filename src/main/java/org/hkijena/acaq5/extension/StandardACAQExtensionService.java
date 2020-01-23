package org.hkijena.acaq5.extension;

import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.extension.algorithms.converters.MaskToParticleConverter;
import org.hkijena.acaq5.extension.algorithms.enhancers.CLAHEImageEnhancer;
import org.hkijena.acaq5.extension.algorithms.enhancers.IlluminationCorrectionEnhancer;
import org.hkijena.acaq5.extension.algorithms.enhancers.WatershedMaskEnhancer;
import org.hkijena.acaq5.extension.algorithms.segmenters.AutoThresholdSegmenter;
import org.hkijena.acaq5.extension.algorithms.segmenters.BrightSpotsSegmenter;
import org.hkijena.acaq5.extension.algorithms.segmenters.HoughSegmenter;
import org.hkijena.acaq5.extension.algorithms.segmenters.InternalGradientSegmenter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.datatypes.ACAQROIData;

@Plugin(type = ACAQExtensionService.class)
public class StandardACAQExtensionService extends AbstractService implements ACAQExtensionService {
    @Override
    public void register(ACAQRegistryService registryService) {

        // Register data types
        registryService.getDatatypeRegistry().register(ACAQGreyscaleImageData.class);
        registryService.getDatatypeRegistry().register(ACAQMaskData.class);
        registryService.getDatatypeRegistry().register(ACAQROIData.class);

        // Register algorithms
        registryService.getAlgorithmRegistry().register(MaskToParticleConverter.class);
        registryService.getAlgorithmRegistry().register(CLAHEImageEnhancer.class);
        registryService.getAlgorithmRegistry().register(IlluminationCorrectionEnhancer.class);
        registryService.getAlgorithmRegistry().register(WatershedMaskEnhancer.class);
        registryService.getAlgorithmRegistry().register(AutoThresholdSegmenter.class);
        registryService.getAlgorithmRegistry().register(BrightSpotsSegmenter.class);
        registryService.getAlgorithmRegistry().register(HoughSegmenter.class);
        registryService.getAlgorithmRegistry().register(InternalGradientSegmenter.class);
    }
}
