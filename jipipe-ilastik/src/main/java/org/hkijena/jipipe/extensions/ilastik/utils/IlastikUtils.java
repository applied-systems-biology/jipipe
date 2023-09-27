package org.hkijena.jipipe.extensions.ilastik.utils;

import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

import java.nio.file.Path;

public class IlastikUtils {
    public static boolean projectSupports(Path projectFile, String group) {
        try (IHDF5Reader reader = HDF5Factory.openForReading(projectFile.toFile())) {
            return reader.isGroup(group);
        }
    }
}
