package org.hkijena.acaq5.extensions.biooobjects;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.Sample;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.Subject;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.Treatment;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.BioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.count.BioObjectsCount;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.count.ClusterBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.count.SingleBioObject;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology.BioObjectsMorphology;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology.FilamentousBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology.IrregularBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.morphology.RoundBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.BioObjectsPreparations;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.BioObjectsLabeling;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.MembraneLabeledBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.UniformlyLabeledBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.bioobject.preparations.labeling.UnlabeledBioObjects;
import org.hkijena.acaq5.extensions.biooobjects.api.traits.quality.*;
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
    public void register() {
        // Project management annotations
        registerTrait("project-sample", Sample.class, ResourceUtils.getPluginResource("icons/traits/project-sample.png"));
        registerTrait("project-subject", Subject.class, ResourceUtils.getPluginResource("icons/traits/project-subject.png"));
        registerTrait("project-treatment", Treatment.class, ResourceUtils.getPluginResource("icons/traits/project-label.png"));

        // Image quality annotations
        registerTrait("image-quality", ImageQuality.class, null);
        registerTrait("image-quality-brightness", BrightnessImageQuality.class, null);
        registerTrait("image-quality-brightness-low", LowBrightnessQuality.class, ResourceUtils.getPluginResource("icons/traits/low-brightness.png"));
        registerTrait("image-quality-brightness-uniform", UniformBrightnessQuality.class, ResourceUtils.getPluginResource("icons/traits/uniform-brightness.png"));
        registerTrait("image-quality-brightness-nonuniform", NonUniformBrightnessQuality.class, ResourceUtils.getPluginResource("icons/traits/non-uniform-brightness.png"));

        // Biological object annotations
        registerTrait("bioobject", BioObjects.class, ResourceUtils.getPluginResource("icons/traits/bioobject.png"));
        registerTrait("bioobject-count", BioObjectsCount.class, ResourceUtils.getPluginResource("icons/traits/bioobject.png"));
        registerTrait("bioobject-count-single", SingleBioObject.class, ResourceUtils.getPluginResource("icons/traits/bioobject.png"));
        registerTrait("bioobject-count-cluster", ClusterBioObjects.class, ResourceUtils.getPluginResource("icons/traits/bioobject-cluster.png"));
        registerTrait("bioobject-morphology", BioObjectsMorphology.class, ResourceUtils.getPluginResource("icons/traits/bioobject.png"));
        registerTrait("bioobject-morphology-irregular", IrregularBioObjects.class, ResourceUtils.getPluginResource("icons/traits/bioobject.png"));
        registerTrait("bioobject-morphology-filamentous", FilamentousBioObjects.class, ResourceUtils.getPluginResource("icons/traits/bioobject-filamentous.png"));
        registerTrait("bioobject-morphology-round", RoundBioObjects.class, ResourceUtils.getPluginResource("icons/traits/bioobject-round.png"));
        registerTrait("bioobject-preparations", BioObjectsPreparations.class, ResourceUtils.getPluginResource("icons/traits/bioobject.png"));
        registerTrait("bioobject-preparations-labeling", BioObjectsLabeling.class, ResourceUtils.getPluginResource("icons/traits/bioobject-labeled.png"));
        registerTrait("bioobject-preparations-labeling-unlabeled", UnlabeledBioObjects.class, ResourceUtils.getPluginResource("icons/traits/bioobject-unlabeled.png"));
        registerTrait("bioobject-preparations-labeling-uniform", UniformlyLabeledBioObjects.class, ResourceUtils.getPluginResource("icons/traits/bioobject-labeled-uniform.png"));
        registerTrait("bioobject-preparations-labeling-membrane", MembraneLabeledBioObjects.class, ResourceUtils.getPluginResource("icons/traits/bioobject-labeled-membrane.png"));
    }
}
