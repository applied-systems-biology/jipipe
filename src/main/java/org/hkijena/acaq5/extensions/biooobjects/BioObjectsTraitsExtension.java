package org.hkijena.acaq5.extensions.biooobjects;

import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.Sample;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.Subject;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.Treatment;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.count.ClusterBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.count.SingleBioObject;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology.FilamentousBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology.IrregularBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology.RoundBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.BioObjectsLabeling;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.MembraneLabeledBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.UniformlyLabeledBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.UnlabeledBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.quality.LowBrightnessQuality;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.quality.NonUniformBrightnessQuality;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.quality.UniformBrightnessQuality;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

@Plugin(type = ACAQJavaExtension.class)
public class BioObjectsTraitsExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Bioimage analysis annotations";
    }

    @Override
    public String getDescription() {
        return "Commonly used annotations for biological image analysis";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:bioobjects-traits";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register(ACAQDefaultRegistry registryService) {
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
}
