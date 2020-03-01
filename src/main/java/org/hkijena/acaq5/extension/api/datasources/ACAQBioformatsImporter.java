package org.hkijena.acaq5.extension.api.datasources;

import ij.ImagePlus;
import loci.formats.FormatException;
import loci.plugins.in.*;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQDefaultMutableTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.AutoTransferTraits;
import org.hkijena.acaq5.extension.api.datatypes.ACAQGreyscaleImageData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMaskData;
import org.hkijena.acaq5.extension.api.datatypes.ACAQMultichannelImageData;
import org.hkijena.acaq5.filesystem.api.dataypes.ACAQFileData;

import java.io.IOException;

@ACAQDocumentation(name = "Bioformats importer")
@AlgorithmInputSlot(value = ACAQFileData.class, slotName = "Files", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQMultichannelImageData.class, slotName = "Image", autoCreate = true)
@AlgorithmOutputSlot(value = ACAQGreyscaleImageData.class)
@AlgorithmOutputSlot(value = ACAQMaskData.class)
@AlgorithmMetadata(category = ACAQAlgorithmCategory.DataSource)
@AutoTransferTraits
public class ACAQBioformatsImporter extends ACAQIteratingAlgorithm {

    public ACAQBioformatsImporter(ACAQAlgorithmDeclaration declaration) {
        super(declaration);
        ((ACAQMutableSlotConfiguration) getSlotConfiguration()).setOutputSealed(false);
    }

    public ACAQBioformatsImporter(ACAQBioformatsImporter other) {
        super(other);
    }

    @Override
    protected void initializeTraits() {
        super.initializeTraits();
        ((ACAQDefaultMutableTraitConfiguration) getTraitConfiguration()).setTraitModificationsSealed(false);
    }

    @Override
    protected void runIteration(ACAQDataInterface dataInterface) {
        ACAQFileData inputFile = dataInterface.getInputData(getFirstInputSlot());
        ImporterOptions options;
        try {
            options = new ImporterOptions();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        options.setId(inputFile.getFilePath().toString());
        options.setWindowless(true);
        options.setQuiet(true);
        options.setShowMetadata(false);
        options.setShowOMEXML(false);
        options.setShowROIs(false);

        ImportProcess process = new ImportProcess(options);

        ImagePlus[] images;

        try {
            new ImporterPrompter(process);
            process.execute();

            DisplayHandler displayHandler = new DisplayHandler(process);
            displayHandler.displayOriginalMetadata();
            displayHandler.displayOMEXML();

            ImagePlusReader reader = new ImagePlusReader(process);
            images = reader.openImagePlus();

            if (!process.getOptions().isVirtual())
                process.getReader().close();
        } catch (FormatException | IOException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < Math.min(getOutputSlots().size(), images.length); ++i) {
            ACAQDataSlot slot = getOutputSlots().get(i);
            dataInterface.addOutputData(slot, ACAQData.createInstance(slot.getAcceptedDataType(), images[i]));
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
