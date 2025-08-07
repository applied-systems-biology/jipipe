package edu.kit.datamanager.ro_crate.reader;

import edu.kit.datamanager.ro_crate.Crate;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Base Interface for methods required to test all reader strategies.
 *
 * @param <SOURCE_T> the source type the strategy reads from.
 * @param <READER_STRATEGY> the type of the reader strategy.
 */
interface TestableReaderStrategy<SOURCE_T, READER_STRATEGY extends GenericReaderStrategy<SOURCE_T>> {
    /**
     * Saves the crate with the writer fitting to the reader of {@link #readCrate(Path)}.
     *
     * @param crate the crate to save
     * @param target the target path to the save location
     * @throws IOException if an error occurs while saving the crate
     */
    void saveCrate(Crate crate, Path target) throws IOException;

    /**
     * Reads the crate with the reader fitting to the writer of {@link #saveCrate(Crate, Path)}.
     * @param source the source path to the crate
     * @return the read crate
     * @throws IOException if an error occurs while reading the crate
     */
    Crate readCrate(Path source) throws IOException;

    /**
     * Creates a new reader strategy with a non-default temporary directory (if supported, default otherwise).
     *
     * @param tmpDirectory the temporary directory to use
     * @param useUuidSubfolder whether to create a UUID subfolder under the temporary directory
     * @return a new reader strategy
     */
    READER_STRATEGY newReaderStrategyWithTmp(Path tmpDirectory, boolean useUuidSubfolder);

    /**
     * Reads the crate using the provided reader strategy.
     *
     * @param strategy the reader strategy to use
     * @param source the source path to the crate
     * @return the read crate
     * @throws IOException if an error occurs while reading the crate
     */
    Crate readCrate(READER_STRATEGY strategy, Path source) throws IOException;
}
