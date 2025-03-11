/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.io;

import ij.ImagePlus;
import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNodeRunContext;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeSimpleIteratingAlgorithm;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.dataenvironment.JIPipeDataDirectoryEnvironment;
import org.hkijena.jipipe.plugins.dataenvironment.OptionalJIPipeDataDirectoryEnvironment;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.settings.ImageSamplesApplicationSettings;
import org.hkijena.jipipe.plugins.imagejdatatypes.settings.ImageSamplesProjectSettings;
import org.hkijena.jipipe.plugins.parameters.api.enums.EnumParameterSettings;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SetJIPipeDocumentation(name = "Import ImageJ sample image", description = "Imports a sample image from the standard set of sample images provided by ImageJ")
@ConfigureJIPipeNode(nodeTypeCategory = DataSourceNodeTypeCategory.class)
@AddJIPipeOutputSlot(value = ImagePlusData.class, name = "Image", create = true)
public class ImportImageJSampleAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Sample sample = Sample.Blobs;
    private OptionalJIPipeDataDirectoryEnvironment dataDirectoryEnvironment = new OptionalJIPipeDataDirectoryEnvironment();

    public ImportImageJSampleAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public ImportImageJSampleAlgorithm(ImportImageJSampleAlgorithm other) {
        super(other);
        this.sample = other.sample;
    }

    @SetJIPipeDocumentation(name = "Sample", description = "The sample to import")
    @JIPipeParameter("sample")
    @EnumParameterSettings(searchable = true)
    public Sample getSample() {
        return sample;
    }

    @JIPipeParameter("sample")
    public void setSample(Sample sample) {
        this.sample = sample;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        JIPipeDataDirectoryEnvironment environment = getDataDirectoryEnvironment();
        Path fileName = environment.getDirectory().resolve(sample.getFileName());
        if(!Files.isRegularFile(fileName)) {
            throw new RuntimeException(new FileNotFoundException(fileName.toString()));
        }

        ImagePlus imagePlus = ImportImagePlusAlgorithm.readImageFrom(fileName, false, runContext, progressInfo);
        iterationStep.addOutputData(getFirstOutputSlot(), new ImagePlusData(imagePlus), progressInfo);
    }

    @Override
    public void getEnvironmentDependencies(List<JIPipeEnvironment> target) {
        super.getEnvironmentDependencies(target);
        JIPipeDataDirectoryEnvironment dataDirectory = getDataDirectoryEnvironment();
        if(dataDirectory != null) {
            target.add(dataDirectory);
        }
    }

    public JIPipeDataDirectoryEnvironment getDataDirectoryEnvironment() {
        if(dataDirectoryEnvironment.isEnabled()) {
            return dataDirectoryEnvironment.getContent();
        }
        ImageSamplesProjectSettings settingsSheet = getProject().getSettingsSheet(ImageSamplesProjectSettings.class);
        if(settingsSheet.getProjectDefaultEnvironment().isEnabled()) {
            return settingsSheet.getProjectDefaultEnvironment().getContent();
        }
        ImageSamplesApplicationSettings applicationSettings = ImageSamplesApplicationSettings.getInstance();
        if(applicationSettings.getDefaultEnvironment().isEnabled()) {
            return applicationSettings.getDefaultEnvironment().getContent();
        }
        return applicationSettings.getReadOnlyDefaultEnvironment();
    }

    public enum Sample {
        AuPbSn40("AuPbSn40.jpg"),
        BatCochleaRenderings("bat-cochlea-renderings.tif"),
        BatCochleaVolume("bat-cochlea-volume.tif"),
        Blobs("blobs.gif"),
        Boats("boats.gif"),
        Cardio("Cardio.dcm"),
        CellColony("Cell_Colony.jpg"),
        CentipedeDrawing("centipede-drawing.jpg"),
        CentipedeMivart("centipede-mivart.png"),
        Clown("clown.jpg"),
        ConfocalSeries("confocal-series.tif"),
        CtDcm("ct.dcm.tif"),
        DotBlot("Dot_Blot.jpg"),
        Embryos("embryos.jpg"),
        FakeTracks("FakeTracks.tif"),
        FirstInstarBrain("first-instar-brain.tif"),
        FluorescentCells("FluorescentCells.tif"),
        Flybrain("flybrain.tif"),
        Gel("gel.gif"),
        HelaCells("hela-cells.tif"),
        ImageWithOverlay("ImageWithOverlay.tif"),
        Leaf("leaf.jpg"),
        LineGraph("LineGraph.jpg"),
        M51("m51.tif"),
        MalariaParasites("_malaria_parasites.tif"),
        Mitosis("mitosis.tif"),
        MriStack("mri-stack.tif"),
        NileBend("NileBend.jpg"),
        OrganOfCorti("organ-of-corti.tif"),
        Particles("particles.gif"),
        RatHippocampalNeuron("Rat_Hippocampal_Neuron.tif"),
        T1Head("t1-head.tif"),
        T1Rendering("t1-rendering.tif"),
        TemFilterSample("TEM_filter_sample.jpg"),
        TopoJSampleSinteredAlumina("TopoJSample_Sintered_Alumina.tif"),
        TreeRings("Tree_Rings.jpg");

        private final String fileName;

        Sample(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }


        @Override
        public String toString() {
            return fileName;
        }
    }
}
