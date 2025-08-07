package edu.kit.datamanager.ro_crate.writer;

import edu.kit.datamanager.ro_crate.Crate;

import java.io.IOException;

/**
 * Generic interface for the strategy of the writer class.
 * This allows for flexible output types when implementing different writing strategies.
 *
 * @param <DESTINATION_TYPE> the type of the destination parameter
 */
public interface GenericWriterStrategy<DESTINATION_TYPE> {
    /**
     * Saves the given crate to the specified destination.
     *
     * @param crate       The crate to save
     * @param destination The destination where the crate should be saved
     */
    void save(Crate crate, DESTINATION_TYPE destination) throws IOException;
}
