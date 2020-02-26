package org.hkijena.acaq5.api.batchimporter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.batchimporter.algorithms.ACAQDataSourceFromFile;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFoldersDataSlot;
import org.hkijena.acaq5.api.batchimporter.traits.ProjectSampleTrait;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.AddsTrait;
import org.hkijena.acaq5.api.traits.RemovesTrait;
import org.hkijena.acaq5.extension.ui.parametereditors.FilePathParameterSettings;
import org.hkijena.acaq5.ui.components.FileSelection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@JsonSerialize(using = ACAQDataSourceFromFileAlgorithmDeclaration.Serializer.class)
public class ACAQDataSourceFromFileAlgorithmDeclaration implements ACAQAlgorithmDeclaration {

    private ACAQAlgorithmDeclaration wrappedAlgorithmDeclaration;
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private ACAQMutableSlotConfiguration slotConfiguration;

    public ACAQDataSourceFromFileAlgorithmDeclaration(ACAQAlgorithmDeclaration wrappedAlgorithmDeclaration) {
        this.wrappedAlgorithmDeclaration = wrappedAlgorithmDeclaration;
        this.slotConfiguration = new ACAQMutableSlotConfiguration();
        initializeSlotDeclarations();
    }

    private void initializeSlotDeclarations() {
        ACAQAlgorithm wrappedAlgorithm = wrappedAlgorithmDeclaration.newInstance();
        for (Map.Entry<String, ACAQParameterAccess> entry : ACAQParameterAccess.getParameters(wrappedAlgorithm).entrySet()) {
            if(entry.getValue().getFieldClass().equals(Path.class) && !entry.getValue().isHidden()) {
                FilePathParameterSettings settings = entry.getValue().getAnnotationOfType(FilePathParameterSettings.class);
                FileSelection.IOMode ioMode = settings != null ? settings.ioMode() : FileSelection.IOMode.Open;
                FileSelection.PathMode pathMode = settings != null ? settings.pathMode() : FileSelection.PathMode.FilesOnly;
                if(ioMode == FileSelection.IOMode.Save)
                    continue;

                if(pathMode == FileSelection.PathMode.FilesOnly) {
                    DefaultAlgorithmInputSlot slot = new DefaultAlgorithmInputSlot(ACAQFilesDataSlot.class, entry.getKey(), false);
                    slotConfiguration.addInputSlot(entry.getKey(), ACAQFilesDataSlot.class);
                    inputSlots.add(slot);
                }
                else if(pathMode == FileSelection.PathMode.DirectoriesOnly) {
                    DefaultAlgorithmInputSlot slot = new DefaultAlgorithmInputSlot(ACAQFoldersDataSlot.class, entry.getKey(), false);
                    slotConfiguration.addInputSlot(entry.getKey(), ACAQFoldersDataSlot.class);
                    inputSlots.add(slot);
                }
            }
        }

        for (AlgorithmOutputSlot outputSlot : getOutputSlots()) {
            slotConfiguration.addOutputSlot(outputSlot.slotName(), outputSlot.value());
        }


        slotConfiguration.setInputSealed(true);
        slotConfiguration.setOutputSealed(true);
    }

    @Override
    public Class<? extends ACAQAlgorithm> getAlgorithmClass() {
        return ACAQDataSourceFromFile.class;
    }

    @Override
    public ACAQAlgorithm newInstance() {
        return new ACAQDataSourceFromFile(this, slotConfiguration);
    }

    @Override
    public ACAQAlgorithm clone(ACAQAlgorithm algorithm) {
        return null;
    }

    @Override
    public String getName() {
        return "Generate: " + wrappedAlgorithmDeclaration.getName();
    }

    @Override
    public String getDescription() {
        return "Generates the data source from input parameters provided via input slots." +
                "This algorithm is only functional within the batch importer.";
    }

    @Override
    public ACAQAlgorithmCategory getCategory() {
        return ACAQAlgorithmCategory.DataSource;
    }

    @Override
    public ACAQAlgorithmVisibility getVisibility() {
        return ACAQAlgorithmVisibility.BatchImporterOnly;
    }

    @Override
    public Set<Class<? extends ACAQTrait>> getPreferredTraits() {
        return Collections.singleton(ProjectSampleTrait.class);
    }

    @Override
    public Set<Class<? extends ACAQTrait>> getUnwantedTraits() {
        return Collections.emptySet();
    }

    @Override
    public List<AddsTrait> getAddedTraits() {
        return Collections.emptyList();
    }

    @Override
    public List<RemovesTrait> getRemovedTraits() {
        return Collections.emptyList();
    }

    @Override
    public List<AlgorithmInputSlot> getInputSlots() {
        return Collections.unmodifiableList(inputSlots);
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return wrappedAlgorithmDeclaration.getOutputSlots();
    }

    @Override
    public boolean matches(JsonNode node) {
        JsonNode classNode = node.path("acaq:algorithm-class");
        JsonNode wrappedNode = node.path("acaq:wrapped-algorithm");
        if(!classNode.isMissingNode() && !wrappedNode.isMissingNode()) {
            String className = classNode.asText();
            if(getAlgorithmClass().getCanonicalName().equals(className)) {
                return wrappedAlgorithmDeclaration.matches(wrappedNode);
            }
        }
        return false;
    }

    public static class Serializer extends JsonSerializer<ACAQDataSourceFromFileAlgorithmDeclaration> {
        @Override
        public void serialize(ACAQDataSourceFromFileAlgorithmDeclaration declaration, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("acaq:algorithm-class", declaration.getAlgorithmClass().getCanonicalName());
            jsonGenerator.writeObjectField("acaq:wrapped-algorithm", declaration.wrappedAlgorithmDeclaration);
            jsonGenerator.writeEndObject();
        }
    }
}
