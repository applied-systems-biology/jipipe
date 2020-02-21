package org.hkijena.acaq5.api;

import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmCategory;
import org.hkijena.acaq5.api.algorithm.AlgorithmMetadata;
import org.hkijena.acaq5.api.data.ACAQInputAsOutputSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotConfiguration;
import org.hkijena.acaq5.api.traits.ACAQTraitConfiguration;

/**
 * An internal node that interfaces sample-specific processing with the analysis
 * The preprocessing output exists both in sample graphs (with a {@link ACAQMutableSlotConfiguration} slot configuration)
 * and in the analysis project graph (with a {@link ACAQInputAsOutputSlotConfiguration}).
 * The preprocessing output does not pass traits like other algorithms, but instead generates traits from an
 * {@link ACAQTraitConfiguration} that is global for each project.
 *
 * Note: This node is not designed to be carried into an {@link ACAQRun} and should not be registered. It should be
 * only added by the project logic.
 */
@ACAQDocumentation(name="Preprocessing output")
@AlgorithmMetadata(category = ACAQAlgorithmCategory.Internal)
public class ACAQPreprocessingOutput extends ACAQAlgorithm {

    public ACAQPreprocessingOutput(ACAQSlotConfiguration slotConfiguration, ACAQTraitConfiguration traitConfiguration) {
        super(slotConfiguration, traitConfiguration);
    }

    @Override
    protected ACAQSlotConfiguration copySlotConfiguration(ACAQAlgorithm other) {
        // The slot configuration is global
        return other.getSlotConfiguration();
    }

    @Override
    public void run() {

    }
}
