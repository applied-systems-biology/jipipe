package org.hkijena.acaq5.extension.datasources;

import org.hkijena.acaq5.api.ACAQSimpleDataSource;
import org.hkijena.acaq5.extension.dataslots.ACAQGreyscaleImageDataSlot;

import java.nio.file.Path;

public class ACAQGreyscaleImageDataFromFile extends ACAQSimpleDataSource {

    private Path fileName;

    public ACAQGreyscaleImageDataFromFile() {
        super("output", ACAQGreyscaleImageDataSlot.class);
    }

    @Override
    public void run() {

    }
}
