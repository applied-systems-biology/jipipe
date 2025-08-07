package edu.kit.datamanager.ro_crate.writer;

import java.io.OutputStream;

/**
 * Utility class for creating instances of different crate writers.
 * This class is not meant to be instantiated.
 */
public class Writers {

    /**
     * Prevents instantiation of this utility class.
     */
    private Writers() {}

    /**
     * Creates a new instance of a crate writer that writes to a folder.
     *
     * @return a new instance of {@link CrateWriter} for writing to a folder
     */
    public static CrateWriter<String> newFolderWriter() {
        return new CrateWriter<>(new WriteFolderStrategy());
    }

    /**
     * Creates a new instance of a crate writer that writes to a zip stream.
     *
     * @return a new instance of {@link CrateWriter} for writing to a zip stream
     */
    public static CrateWriter<OutputStream> newZipStreamWriter() {
        return new CrateWriter<>(new WriteZipStreamStrategy());
    }

    /**
     * Creates a new instance of a crate writer that writes to a zip file.
     *
     * @return a new instance of {@link CrateWriter} for writing to a zip file
     */
    public static CrateWriter<String> newZipPathWriter() {
        return new CrateWriter<>(new WriteZipStrategy());
    }
}
