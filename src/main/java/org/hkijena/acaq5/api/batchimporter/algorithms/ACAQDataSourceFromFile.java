package org.hkijena.acaq5.api.batchimporter.algorithms;

import org.hkijena.acaq5.api.ACAQProject;
import org.hkijena.acaq5.api.ACAQProjectSample;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.batchimporter.ACAQDataSourceFromFileAlgorithmDeclaration;
import org.hkijena.acaq5.api.batchimporter.dataslots.ACAQFilesDataSlot;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFileData;
import org.hkijena.acaq5.api.batchimporter.dataypes.ACAQFilesystemData;
import org.hkijena.acaq5.api.batchimporter.traits.ProjectSampleTrait;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQDataSource;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.traits.ACAQMutableTraitModifier;

import java.util.Map;

/**
 * An algorithm that belongs to the batch importer and is annotated to generate a specified data type
 * It is non-functional in all algorithm-graphs except BatchImporter
 */
public class ACAQDataSourceFromFile extends ACAQAlgorithm {

    private ACAQProject project;

    public ACAQDataSourceFromFile(ACAQAlgorithmDeclaration declaration, ACAQMutableSlotConfiguration configuration) {
        super(declaration, configuration, new ACAQMutableTraitModifier(configuration));
    }

    public ACAQDataSourceFromFile(ACAQDataSourceFromFile other) {
        super(other);
    }

    @Override
    public void run() {
        if(project == null)
            return;

        for(ACAQDataSlot<?> inputSlot : getInputSlots()) {
            if(inputSlot instanceof ACAQFilesDataSlot) {
                processInputFilesSlot((ACAQFilesDataSlot)inputSlot);
            }
        }
    }

    private void processInputFilesSlot(ACAQFilesDataSlot inputSlot) {
        for(ACAQFileData file : inputSlot.getData().getFiles()) {
            String sampleName = (String) file.findAnnotation(ProjectSampleTrait.FILESYSTEM_ANNOTATION_SAMPLE);
            ACAQProjectSample sample = project.addSample(sampleName);

            ACAQAlgorithmGraph graph = sample.getPreprocessingGraph();
            ACAQAlgorithm dataSource = ((ACAQDataSourceFromFileAlgorithmDeclaration)getDeclaration()).getWrappedAlgorithmDeclaration().newInstance();
            Map<String, ACAQParameterAccess> parameters = ACAQParameterAccess.getParameters(dataSource);
            ACAQParameterAccess parameterAccess = parameters.get(inputSlot.getName());
            parameterAccess.set(file.getFilePath());

            graph.insertNode(dataSource);
        }
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }

    public ACAQProject getProject() {
        return project;
    }

    public void setProject(ACAQProject project) {
        this.project = project;
    }
}
