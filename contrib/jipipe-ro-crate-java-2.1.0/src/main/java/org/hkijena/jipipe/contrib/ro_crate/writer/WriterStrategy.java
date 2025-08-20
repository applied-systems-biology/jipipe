package org.hkijena.jipipe.contrib.ro_crate.writer;

/**
 * Strategy for writing of crates.
 *
 * @author Nikola Tzotchev on 9.2.2022 г.
 * @version 1
 * @deprecated Use {@link GenericWriterStrategy} instead.
 */
@Deprecated(since = "2.1.0", forRemoval = true)
public interface WriterStrategy extends GenericWriterStrategy<String> {
}
