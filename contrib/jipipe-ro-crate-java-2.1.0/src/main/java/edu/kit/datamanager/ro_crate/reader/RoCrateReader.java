package edu.kit.datamanager.ro_crate.reader;

/**
 * This class allows reading crates from the outside into the library in order
 * to inspect or modify it.
 * <p>
 * The constructor takes a strategy to support different ways of importing the
 * crates. (from zip, folder, etc.).
 * <p>
 * The reader consideres "hasPart" and "isPartOf" properties and considers all
 * entities (in-)directly connected to the root entity ("./") as DataEntities.
 *
 * @deprecated Use {@link CrateReader} instead.
 */
@Deprecated(since = "2.1.0", forRemoval = true)
public class RoCrateReader extends CrateReader<String> {
    public RoCrateReader(GenericReaderStrategy<String> reader) {
        super(reader);
    }
}
