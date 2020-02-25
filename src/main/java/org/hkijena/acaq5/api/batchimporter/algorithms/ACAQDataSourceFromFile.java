package org.hkijena.acaq5.api.batchimporter.algorithms;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSource;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.traits.ACAQMutableTraitModifier;

/**
 * An algorithm that belongs to the batch importer and is annotated to generate a specified data type
 * It is non-functional in all algorithm-graphs except BatchImporter
 */
public class ACAQDataSourceFromFile extends ACAQAlgorithm {

    public ACAQDataSourceFromFile(ACAQAlgorithmDeclaration declaration, ACAQMutableSlotConfiguration configuration) {
        super(declaration, configuration, new ACAQMutableTraitModifier(configuration));
    }

    public ACAQDataSourceFromFile(ACAQDataSourceFromFile other) {
        super(other);
    }

    @Override
    public void run() {

    }

    @Override
    public void reportValidity(ACAQValidityReport report) {

    }
}
