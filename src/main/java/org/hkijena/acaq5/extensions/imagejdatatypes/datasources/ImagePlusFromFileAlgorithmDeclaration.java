package org.hkijena.acaq5.extensions.imagejdatatypes.datasources;

import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.traits.ACAQDataSlotTraitConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.extensions.filesystem.api.dataypes.ACAQFileData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ImagePlusFromFileAlgorithmDeclaration implements ACAQAlgorithmDeclaration {

    private String dataClassId;
    private Class<? extends ACAQData> dataClass;
    private ACAQDataSlotTraitConfiguration slotTraitConfiguration = new ACAQDataSlotTraitConfiguration();
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private List<AlgorithmOutputSlot> outputSlots = new ArrayList<>();

    public ImagePlusFromFileAlgorithmDeclaration(String dataClassId, Class<? extends ACAQData> dataClass) {
        this.dataClassId = dataClassId;
        this.dataClass = dataClass;
        inputSlots.add(new DefaultAlgorithmInputSlot(ACAQFileData.class, "Input", false));
        outputSlots.add(new DefaultAlgorithmOutputSlot(dataClass, "Image", false));
    }

    @Override
    public String getId() {
        return dataClassId + "-from-file";
    }

    @Override
    public Class<? extends ACAQAlgorithm> getAlgorithmClass() {
        return ImagePlusFromFile.class;
    }

    @Override
    public ACAQAlgorithm newInstance() {
        return new ImagePlusFromFile(this, dataClass);
    }

    @Override
    public ACAQAlgorithm clone(ACAQAlgorithm algorithm) {
        return new ImagePlusFromFile((ImagePlusFromFile) algorithm);
    }

    @Override
    public String getName() {
        return "Import " + ACAQData.getNameOf(dataClass);
    }

    @Override
    public String getDescription() {
        return "Imports an image via native ImageJ functions";
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public ACAQAlgorithmCategory getCategory() {
        return ACAQAlgorithmCategory.DataSource;
    }

    @Override
    public Set<ACAQTraitDeclaration> getPreferredTraits() {
        return Collections.emptySet();
    }

    @Override
    public Set<ACAQTraitDeclaration> getUnwantedTraits() {
        return Collections.emptySet();
    }

    @Override
    public ACAQDataSlotTraitConfiguration getSlotTraitConfiguration() {
        return slotTraitConfiguration;
    }

    @Override
    public List<AlgorithmInputSlot> getInputSlots() {
        return Collections.unmodifiableList(inputSlots);
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return Collections.unmodifiableList(outputSlots);
    }
}
