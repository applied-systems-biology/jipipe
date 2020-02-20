package org.hkijena.acaq5.extension;

import ij.process.AutoThresholder;
import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.extension.api.algorithms.converters.MaskToParticleConverter;
import org.hkijena.acaq5.extension.api.algorithms.converters.MultiChannelSplitterConverter;
import org.hkijena.acaq5.extension.api.algorithms.enhancers.CLAHEImageEnhancer;
import org.hkijena.acaq5.extension.api.algorithms.enhancers.IlluminationCorrectionEnhancer;
import org.hkijena.acaq5.extension.api.algorithms.enhancers.MergeROIEnhancer;
import org.hkijena.acaq5.extension.api.algorithms.enhancers.WatershedMaskEnhancer;
import org.hkijena.acaq5.extension.api.algorithms.segmenters.AutoThresholdSegmenter;
import org.hkijena.acaq5.extension.api.algorithms.segmenters.BrightSpotsSegmenter;
import org.hkijena.acaq5.extension.api.algorithms.segmenters.HoughSegmenter;
import org.hkijena.acaq5.extension.api.algorithms.segmenters.InternalGradientSegmenter;
import org.hkijena.acaq5.extension.api.dataslots.ACAQGreyscaleImageDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMaskDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQMultichannelImageDataSlot;
import org.hkijena.acaq5.extension.api.dataslots.ACAQROIDataSlot;
import org.hkijena.acaq5.extension.api.datasources.ACAQGreyscaleImageDataFromFile;
import org.hkijena.acaq5.extension.api.datasources.ACAQMaskImageDataFromFile;
import org.hkijena.acaq5.extension.api.datasources.ACAQMultichannelImageDataFromFile;
import org.hkijena.acaq5.extension.api.datasources.ACAQROIDataFromFile;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMultichannelImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQROIData;
import org.hkijena.acaq5.extension.api.traits.LowBrightnessQuality;
import org.hkijena.acaq5.extension.api.traits.NonUniformBrightnessQuality;
import org.hkijena.acaq5.extension.api.traits.bioobject.*;
import org.hkijena.acaq5.extension.ui.parametereditors.*;
import org.hkijena.acaq5.extension.ui.resultanalysis.ImageDataSlotResultUI;
import org.hkijena.acaq5.extension.ui.resultanalysis.ROIDataSlotResultUI;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

import java.nio.file.Path;

@Plugin(type = ACAQExtensionService.class)
public class StandardACAQExtensionService extends AbstractService implements ACAQExtensionService {
    @Override
    public void register(ACAQRegistryService registryService) {

        // Register traits
        registerTraits(registryService);

        // Register data types
        registerDataTypes(registryService);

        // Register algorithms
        registerAlgorithms(registryService);

        // Register data sources
        registryService.getAlgorithmRegistry().register(ACAQGreyscaleImageDataFromFile.class);
        registryService.getAlgorithmRegistry().register(ACAQMaskImageDataFromFile.class);
        registryService.getAlgorithmRegistry().register(ACAQROIDataFromFile.class);
        registryService.getAlgorithmRegistry().register(ACAQMultichannelImageDataFromFile.class);

        // Register parameter editor UIs
        registryService.getUIParametertypeRegistry().registerParameterEditor(Path.class, FilePathParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(int.class, IntegerParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(double.class, DoubleParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(float.class, FloatParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(boolean.class, BooleanParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(AutoThresholder.Method.class, EnumParameterEditorUI.class);

        // Register result data slot UIs
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQMultichannelImageDataSlot.class, ImageDataSlotResultUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQGreyscaleImageDataSlot.class, ImageDataSlotResultUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQMaskDataSlot.class, ImageDataSlotResultUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQROIDataSlot.class, ROIDataSlotResultUI.class);
    }

    private void registerTraits(ACAQRegistryService registryService) {
        registryService.getTraitRegistry().register(LowBrightnessQuality.class);
        registryService.getTraitRegistry().register(NonUniformBrightnessQuality.class);
        registryService.getTraitRegistry().register(BioObjects.class);
        registryService.getTraitRegistry().register(ClusterBioObjects.class);
        registryService.getTraitRegistry().register(FilamentousBioObjects.class);
        registryService.getTraitRegistry().register(LabeledBioObjects.class);
        registryService.getTraitRegistry().register(MembraneLabeledBioObjects.class);
        registryService.getTraitRegistry().register(RoundBioObjects.class);
        registryService.getTraitRegistry().register(SingleBioObject.class);
        registryService.getTraitRegistry().register(UniformlyLabeledBioObjects.class);
        registryService.getTraitRegistry().register(UnlabeledBioObjects.class);
    }

    private void registerAlgorithms(ACAQRegistryService registryService) {
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
    }

    private void registerDataTypes(ACAQRegistryService registryService) {
        registryService.getDatatypeRegistry().register(ACAQGreyscaleImageData.class, ACAQGreyscaleImageDataSlot.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQGreyscaleImageData.class,
                ResourceUtils.getPluginResource("icons/data-types/greyscale.png"));
        registryService.getDatatypeRegistry().register(ACAQMaskData.class, ACAQMaskDataSlot.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQMaskData.class,
                ResourceUtils.getPluginResource("icons/data-types/binary.png"));
        registryService.getDatatypeRegistry().register(ACAQROIData.class, ACAQROIDataSlot.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQROIData.class,
                ResourceUtils.getPluginResource("icons/data-types/roi.png"));
        registryService.getDatatypeRegistry().register(ACAQMultichannelImageData.class, ACAQMultichannelImageDataSlot.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQMultichannelImageData.class,
                ResourceUtils.getPluginResource("icons/data-types/multichannel.png"));
    }
}
