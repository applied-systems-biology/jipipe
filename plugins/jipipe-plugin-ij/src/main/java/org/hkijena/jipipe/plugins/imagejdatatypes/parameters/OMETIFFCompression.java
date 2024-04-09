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

package org.hkijena.jipipe.plugins.imagejdatatypes.parameters;

import loci.formats.out.TiffWriter;

/**
 * Available compression methods
 */
public enum OMETIFFCompression {
    Uncompressed(TiffWriter.COMPRESSION_UNCOMPRESSED),
    LZW(TiffWriter.COMPRESSION_LZW),
    JPEG2000(TiffWriter.COMPRESSION_J2K),
    JPEG2000Lossy(TiffWriter.COMPRESSION_J2K_LOSSY),
    JPEG(TiffWriter.COMPRESSION_JPEG),
    ZLIB(TiffWriter.COMPRESSION_ZLIB);

    private final String compression;

    OMETIFFCompression(String compression) {

        this.compression = compression;
    }

    public String getCompression() {
        return compression;
    }
}
