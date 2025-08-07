package edu.kit.datamanager.ro_crate.writer;

/**
 * The class used for writing (exporting) crates. The class uses a strategy
 * pattern for writing crates as different formats. (zip, folders, etc.)
 *
 * @deprecated Use {@link CrateWriter} instead.
 */
@Deprecated(since = "2.1.0", forRemoval = true)
public class RoCrateWriter extends CrateWriter<String> {

    public RoCrateWriter(GenericWriterStrategy<String> writer) {
        super(writer);
    }
}
