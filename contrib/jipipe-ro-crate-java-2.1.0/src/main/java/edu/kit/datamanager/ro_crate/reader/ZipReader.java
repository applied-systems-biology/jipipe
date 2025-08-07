package edu.kit.datamanager.ro_crate.reader;

import java.nio.file.Path;

/**
 * A ReaderStrategy implementation which reads from ZipFiles.
 * <p>
 * May be used as a dependency for RoCrateReader. It will unzip
 * the ZipFile in a path relative to the directory this application runs in.
 * By default, it will be `./.tmp/ro-crate-java/zipReader/$UUID/`.
 * <p>
 * NOTE: The resulting crate may refer to these temporary files. Therefore,
 * these files are only being deleted before the JVM exits. If you need to free
 * space because your application is long-running or creates a lot of
 * crates, you may use the getters to retrieve information which will help
 * you to clean up manually. Keep in mind that crates may refer to this
 * folder after extraction. Use RoCrateWriter to export it so some
 * persistent location and possibly read it from there, if required. Or use
 * the ZipWriter to write it back to its source.
 *
 * @deprecated Use {@link ReadZipStrategy} instead.
 */
@Deprecated(since = "2.1.0", forRemoval = true)
public class ZipReader extends ReadZipStrategy {

  /**
   * Crates a ZipReader with the default configuration as described in the class documentation.
   */
  public ZipReader() {
    super();
  }

  /**
   * Creates a ZipReader which will extract the contents temporary
   * to the given location instead of the default location.
   *
   * @param folderPath            the custom directory to extract
   *                              content to for temporary access.
   * @param shallAddUuidSubfolder if true, the reader will extract
   *                              into subdirectories of the given
   *                              directory. These subdirectories
   *                              will have UUIDs as their names.
   */
  public ZipReader(Path folderPath, boolean shallAddUuidSubfolder) {
    super(folderPath, shallAddUuidSubfolder);
  }
}
