package edu.kit.datamanager.ro_crate.writer;

import edu.kit.datamanager.ro_crate.Crate;
import edu.kit.datamanager.ro_crate.validation.JsonSchemaValidation;
import edu.kit.datamanager.ro_crate.validation.Validator;

import java.io.IOException;

/**
 * The class used for writing (exporting) crates. The class uses a strategy
 * pattern for writing crates as different formats. (zip, folders, etc.)
 *
 * @param <DESTINATION_TYPE> the type which determines the destination of the result
 */
public class CrateWriter<DESTINATION_TYPE> {

    private final GenericWriterStrategy<DESTINATION_TYPE> strategy;
    protected ProvenanceManager provenanceManager = new ProvenanceManager();

    /**
     * Constructs a CrateWriter with a specified strategy for writing crates.
     *
     * @param strategy the strategy to use for writing crates.
     */
    public CrateWriter(GenericWriterStrategy<DESTINATION_TYPE> strategy) {
        this.strategy = strategy;
    }

    /**
     * Sets the ProvenanceManager to be used for automatic provenance information
     * generation when saving the crate.
     *
     * @param provenanceManager the ProvenanceManager to use. Using null will disable provenance management.
     * @return this CrateWriter instance for method chaining.
     */
    public CrateWriter<DESTINATION_TYPE> withAutomaticProvenance(ProvenanceManager provenanceManager) {
        this.provenanceManager = provenanceManager;
        return this;
    }

    /**
     * This method saves the crate to a destination provided.
     *
     * @param crate the crate to write.
     * @param destination the location where the crate should be written.
     */
    public void save(Crate crate, DESTINATION_TYPE destination) throws IOException {
        Validator defaultValidation = new Validator(new JsonSchemaValidation());
        defaultValidation.validate(crate);
        if (this.provenanceManager != null) {
            this.provenanceManager.addProvenanceInformation(crate);
        }
        this.strategy.save(crate, destination);
    }
}
