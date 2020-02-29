package org.hkijena.acaq5.extension;

import ij.process.AutoThresholder;
import org.hkijena.acaq5.ACAQExtensionService;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.extension.api.datasources.ACAQGreyscaleImageDataFromFile;
import org.hkijena.acaq5.extension.api.datasources.ACAQMaskImageDataFromFile;
import org.hkijena.acaq5.extension.api.datasources.ACAQMultichannelImageDataFromFile;
import org.hkijena.acaq5.extension.api.datasources.ACAQROIDataFromFile;
import org.hkijena.acaq5.extension.api.datasources.ACAQResultsTableFromFile;
import org.hkijena.acaq5.extension.api.datatypes.*;
import org.hkijena.acaq5.extension.api.traits.Sample;
import org.hkijena.acaq5.extension.api.traits.Subject;
import org.hkijena.acaq5.extension.api.traits.Treatment;
import org.hkijena.acaq5.extension.api.traits.bioobject.count.ClusterBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.count.SingleBioObject;
import org.hkijena.acaq5.extension.api.traits.bioobject.morphology.FilamentousBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.morphology.IrregularBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.morphology.RoundBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.BioObjectsLabeling;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.MembraneLabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.UniformlyLabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.bioobject.preparations.labeling.UnlabeledBioObjects;
import org.hkijena.acaq5.extension.api.traits.quality.LowBrightnessQuality;
import org.hkijena.acaq5.extension.api.traits.quality.NonUniformBrightnessQuality;
import org.hkijena.acaq5.extension.api.traits.quality.UniformBrightnessQuality;
import org.hkijena.acaq5.extension.ui.parametereditors.*;
import org.hkijena.acaq5.extension.ui.resultanalysis.ImageDataSlotResultDataSlotRowUI;
import org.hkijena.acaq5.extension.ui.resultanalysis.ROIDataSlotResultDataSlotRowUI;
import org.hkijena.acaq5.extension.ui.resultanalysis.ResultsTableDataSlotResultDataSlotRowUI;
import org.hkijena.acaq5.utils.PathFilter;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Plugin(type = ACAQExtensionService.class)
public class StandardACAQExtensionService extends AbstractService implements ACAQExtensionService {
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
        registryService.getAlgorithmRegistry().register(ACAQResultsTableFromFile.class);

        // Register parameter editor UIs
        registryService.getUIParametertypeRegistry().registerParameterEditor(Path.class, FilePathParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(int.class, IntegerParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(double.class, DoubleParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(float.class, FloatParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(boolean.class, BooleanParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(String.class, StringParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(AutoThresholder.Method.class, EnumParameterEditorUI.class);
        registryService.getUIParametertypeRegistry().registerParameterEditor(PathFilter.class, PathFilterParameterEditorUI.class);

        // Register result data slot UIs
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQMultichannelImageData.class, ImageDataSlotResultDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQGreyscaleImageData.class, ImageDataSlotResultDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQMaskData.class, ImageDataSlotResultDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQROIData.class, ROIDataSlotResultDataSlotRowUI.class);
        registryService.getUIDatatypeRegistry().registerResultSlotUI(ACAQResultsTableData.class, ResultsTableDataSlotResultDataSlotRowUI.class);
    }

    private void registerTraits(ACAQRegistryService registryService) {

        registryService.getTraitRegistry().register(LowBrightnessQuality.class);
        registryService.getUITraitRegistry().registerIcon(LowBrightnessQuality.class,
                ResourceUtils.getPluginResource("icons/traits/low-brightness.png"));

        registryService.getTraitRegistry().register(NonUniformBrightnessQuality.class);
        registryService.getUITraitRegistry().registerIcon(NonUniformBrightnessQuality.class,
                ResourceUtils.getPluginResource("icons/traits/non-uniform-brightness.png"));

        registryService.getTraitRegistry().register(UniformBrightnessQuality.class);
        registryService.getUITraitRegistry().registerIcon(UniformBrightnessQuality.class,
                ResourceUtils.getPluginResource("icons/traits/uniform-brightness.png"));

        registryService.getTraitRegistry().register(ClusterBioObjects.class);
        registryService.getUITraitRegistry().registerIcon(ClusterBioObjects.class,
                ResourceUtils.getPluginResource("icons/traits/bioobject-cluster.png"));

        registryService.getTraitRegistry().register(FilamentousBioObjects.class);
        registryService.getUITraitRegistry().registerIcon(FilamentousBioObjects.class,
                ResourceUtils.getPluginResource("icons/traits/bioobject-filamentous.png"));

        registryService.getTraitRegistry().register(IrregularBioObjects.class);
        registryService.getUITraitRegistry().registerIcon(IrregularBioObjects.class,
                ResourceUtils.getPluginResource("icons/traits/bioobject.png"));

        registryService.getTraitRegistry().register(BioObjectsLabeling.class);
        registryService.getUITraitRegistry().registerIcon(BioObjectsLabeling.class,
                ResourceUtils.getPluginResource("icons/traits/bioobject-labeled.png"));

        registryService.getTraitRegistry().register(MembraneLabeledBioObjects.class);
        registryService.getUITraitRegistry().registerIcon(MembraneLabeledBioObjects.class,
                ResourceUtils.getPluginResource("icons/traits/bioobject-labeled-membrane.png"));

        registryService.getTraitRegistry().register(RoundBioObjects.class);
        registryService.getUITraitRegistry().registerIcon(RoundBioObjects.class,
                ResourceUtils.getPluginResource("icons/traits/bioobject-round.png"));

        registryService.getTraitRegistry().register(SingleBioObject.class);
        registryService.getUITraitRegistry().registerIcon(SingleBioObject.class,
                ResourceUtils.getPluginResource("icons/traits/bioobject.png"));

        registryService.getTraitRegistry().register(UniformlyLabeledBioObjects.class);
        registryService.getUITraitRegistry().registerIcon(UniformlyLabeledBioObjects.class,
                ResourceUtils.getPluginResource("icons/traits/bioobject-labeled-uniform.png"));

        registryService.getTraitRegistry().register(UnlabeledBioObjects.class);
        registryService.getUITraitRegistry().registerIcon(UnlabeledBioObjects.class,
                ResourceUtils.getPluginResource("icons/traits/bioobject-unlabeled.png"));

        registryService.getTraitRegistry().register(Sample.class);
        registryService.getUITraitRegistry().registerIcon(Sample.class,
                ResourceUtils.getPluginResource("icons/traits/project-sample.png"));

        registryService.getTraitRegistry().register(Treatment.class);
        registryService.getUITraitRegistry().registerIcon(Treatment.class,
                ResourceUtils.getPluginResource("icons/traits/project-label.png"));

        registryService.getTraitRegistry().register(Subject.class);
        registryService.getUITraitRegistry().registerIcon(Subject.class,
                ResourceUtils.getPluginResource("icons/traits/project-subject.png"));
    }

    private void registerAlgorithms(ACAQRegistryService registryService) {
//        registryService.getAlgorithmRegistry().register(MaskToParticleConverter.class);
//        registryService.getAlgorithmRegistry().register(CLAHEImageEnhancer.class);
//        registryService.getAlgorithmRegistry().register(IlluminationCorrectionEnhancer.class);
//        registryService.getAlgorithmRegistry().register(WatershedMaskEnhancer.class);
//        registryService.getAlgorithmRegistry().register(AutoThresholdSegmenter.class);
//        registryService.getAlgorithmRegistry().register(BrightSpotsSegmenter.class);
//        registryService.getAlgorithmRegistry().register(HoughSegmenter.class);
//        registryService.getAlgorithmRegistry().register(InternalGradientSegmenter.class);
//        registryService.getAlgorithmRegistry().register(MultiChannelSplitterConverter.class);
//        registryService.getAlgorithmRegistry().register(MergeROIEnhancer.class);
//        registryService.getAlgorithmRegistry().register(HessianSegmenter.class);
    }

    private void registerDataTypes(ACAQRegistryService registryService) {
        registryService.getDatatypeRegistry().register(ACAQGreyscaleImageData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQGreyscaleImageData.class,
                ResourceUtils.getPluginResource("icons/data-types/greyscale.png"));
        registryService.getDatatypeRegistry().register(ACAQMaskData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQMaskData.class,
                ResourceUtils.getPluginResource("icons/data-types/binary.png"));
        registryService.getDatatypeRegistry().register(ACAQROIData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQROIData.class,
                ResourceUtils.getPluginResource("icons/data-types/roi.png"));
        registryService.getDatatypeRegistry().register(ACAQMultichannelImageData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQMultichannelImageData.class,
                ResourceUtils.getPluginResource("icons/data-types/multichannel.png"));
        registryService.getDatatypeRegistry().register(ACAQResultsTableData.class);
        registryService.getUIDatatypeRegistry().registerIcon(ACAQResultsTableData.class,
                ResourceUtils.getPluginResource("icons/data-types/results-table.png"));
    }
}
