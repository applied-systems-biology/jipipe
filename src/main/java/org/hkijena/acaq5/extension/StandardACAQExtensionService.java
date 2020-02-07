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
import org.hkijena.acaq5.extension.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.dataslots.ACAQROIDataSlot;
import org.hkijena.acaq5.extension.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.datatypes.ACAQROIData;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

@Plugin(type = ACAQExtensionService.class)
public class StandardACAQExtensionService extends AbstractService implements ACAQExtensionService {
    @Override
    public void register(ACAQRegistryService registryService) {

        // Register data types
        registryService.getDatatypeRegistry().register(ACAQGreyscaleImageData.class, ACAQGreyscaleImageDataSlot.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQGreyscaleImageData.class,
                ResourceUtils.getPluginResource("icons/data-type-greyscale.png"));
        registryService.getDatatypeRegistry().register(ACAQMaskData.class, ACAQMaskDataSlot.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQMaskData.class,
                ResourceUtils.getPluginResource("icons/data-type-binary.png"));
        registryService.getDatatypeRegistry().register(ACAQROIData.class, ACAQROIDataSlot.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQROIData.class,
                ResourceUtils.getPluginResource("icons/data-type-roi.png"));

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
