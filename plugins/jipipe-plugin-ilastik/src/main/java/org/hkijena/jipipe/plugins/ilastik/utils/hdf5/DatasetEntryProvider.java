/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.ilastik.utils.hdf5;

import hdf.hdf5lib.exceptions.HDF5Exception;

import java.util.List;

public interface DatasetEntryProvider {
    List<DatasetEntry> findAvailableDatasets(String path);

    class ReadException extends RuntimeException {
        public ReadException(String message, HDF5Exception e) {
            super(message, e);
        }
    }

    class DatasetEntry {
        public final String path;
        public final String axisTags;
        public final String verboseName;
        public final int rank;

        public DatasetEntry(String path, int rank, String axisTags, String verboseName) {
            this.path = path;
            this.rank = rank;
            this.axisTags = axisTags;
            this.verboseName = verboseName;
        }

    }
}
