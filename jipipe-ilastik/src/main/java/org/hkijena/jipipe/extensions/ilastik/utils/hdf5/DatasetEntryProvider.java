package org.hkijena.jipipe.extensions.ilastik.utils.hdf5;

import hdf.hdf5lib.exceptions.HDF5Exception;

import java.util.List;

/**
 * From ilastik4ij
 * <p>
 * MIT License
 * <p>
 * Copyright (c) 2017 ilastik
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
