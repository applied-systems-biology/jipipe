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

package org.hkijena.jipipe.plugins.cellpose.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wrapper around Cellpose models
 */
@SetJIPipeDocumentation(name = "Cellpose model", description = "A Cellpose model")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A single file without extension that contains the Cellpose model",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/cellpose-model-data.schema.json")
public class CellposeModelData implements JIPipeData {

    private byte[] data;
    private Metadata metadata = new Metadata();

    public CellposeModelData(String pretrainedModelName) {
        metadata.setName(pretrainedModelName);
        metadata.setPretrained(true);
    }

    public CellposeModelData(Path file) {
        this.metadata.name = file.getFileName().toString();
        this.metadata.pretrained = false;
        try {
            data = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public CellposeModelData(byte[] data, String name) {
        this.data = data;
        this.metadata.name = name;
        this.metadata.pretrained = false;
    }

    public CellposeModelData(CellposeModelData other) {
        this.data = other.data;
        this.metadata = new Metadata(other.metadata);
    }

    public static CellposeModelData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        Path metadataFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        Metadata metadata = JsonUtils.readFromFile(metadataFile, Metadata.class);

        if (metadata.isPretrained()) {
            return new CellposeModelData(metadata.getName());
        } else {
            return new CellposeModelData(storage.getFileSystemPath().resolve(metadata.getName()));
        }
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public boolean isPretrained() {
        return metadata.isPretrained();
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        try {
            String modelName = forceName ? StringUtils.orElse(name, metadata.name) : metadata.name;
            String metadataName = forceName ? StringUtils.orElse(name, "metadata") : "metadata";
            if (data != null) {
                Files.write(storage.getFileSystemPath().resolve(modelName), data);
            }
            JsonUtils.saveToFile(metadata, PathUtils.ensureExtension(storage.getFileSystemPath().resolve(metadataName), ".json"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new CellposeModelData(this);
    }

    @Override
    public String toString() {
        if (metadata.isPretrained()) {
            if (getPretrainedModelName() != null) {
                return "Pretrained Cellpose model '" + metadata.getName() + "'";
            } else {
                return "No model";
            }
        } else {
            return "Cellpose model '" + metadata.getName() + "' (" + (data.length / 1024 / 1024) + " MB)";
        }

    }

    public String getPretrainedModelName() {
        return metadata.getName();
    }

    public static class Metadata {
        private String name;
        private boolean pretrained;

        public Metadata() {
        }

        public Metadata(Metadata other) {
            this.name = other.name;
            this.pretrained = other.pretrained;
        }

        @JsonGetter("name")
        public String getName() {
            return name;
        }

        @JsonSetter("name")
        public void setName(String name) {
            this.name = name;
        }

        @JsonGetter("is-pretrained")
        public boolean isPretrained() {
            return pretrained;
        }

        @JsonSetter("is-pretrained")
        public void setPretrained(boolean pretrained) {
            this.pretrained = pretrained;
        }
    }
}
