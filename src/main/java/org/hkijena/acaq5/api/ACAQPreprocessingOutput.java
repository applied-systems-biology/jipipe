package org.hkijena.acaq5.api;

@ACAQDocumentation(name="Preprocessing output")
@ACAQAlgorithmMetadata(category = ACAQAlgorithmCategory.Internal)
public class ACAQPreprocessingOutput extends ACAQAlgorithm {

    public ACAQPreprocessingOutput(ACAQSlotConfiguration slotConfiguration) {
        super(slotConfiguration);
    }

    public ACAQPreprocessingOutput(ACAQPreprocessingOutput other) {
        super(other.getSlotConfiguration());
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
