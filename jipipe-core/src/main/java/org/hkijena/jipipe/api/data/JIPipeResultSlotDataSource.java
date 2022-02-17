/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.data;

import java.nio.file.Path;

/**
 * Data from a slot inside a result folder
 */
public class JIPipeResultSlotDataSource implements JIPipeDataSource {
    private final JIPipeDataSlot slot;
    private final JIPipeDataTableMetadataRow row;
    private final Path rowStoragePath;

    public JIPipeResultSlotDataSource(JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, Path rowStoragePath) {
        this.slot = slot;
        this.row = row;
        this.rowStoragePath = rowStoragePath;
    }

    public JIPipeDataSlot getSlot() {
        return slot;
    }

    public JIPipeDataTableMetadataRow getRow() {
        return row;
    }

    public Path getRowStoragePath() {
        return rowStoragePath;
    }
}
