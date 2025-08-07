package edu.kit.datamanager.ro_crate.reader;

/**
 * Interface for the strategy for the reader class.
 * This should be implemented if additional strategies are to be build.
 * (e.g., reading from a gzip)
 *
 * @author Nikola Tzotchev on 9.2.2022 г.
 * @version 1
 *
 * @deprecated Use {@link GenericReaderStrategy} instead.
 */
@Deprecated(since = "2.1.0", forRemoval = true)
public interface ReaderStrategy extends GenericReaderStrategy<String> {}
