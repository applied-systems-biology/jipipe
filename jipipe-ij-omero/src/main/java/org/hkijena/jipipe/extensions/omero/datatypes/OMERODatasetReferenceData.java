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

package org.hkijena.jipipe.extensions.omero.datatypes;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;

import java.io.IOException;
import java.nio.file.Path;

@JIPipeDocumentation(name = "OMERO Dataset", description = "An OMERO dataset ID")
@JIPipeDataStorageDocumentation("Contains a single *.json file that stores the <pre>dataset-id</pre> in a JSON object.")
public class OMERODatasetReferenceData implements JIPipeData {
    private long datasetId;

    public OMERODatasetReferenceData(long datasetId) {
        this.datasetId = datasetId;
    }

    @JsonGetter("dataset-id")
    public long getDatasetId() {
        return datasetId;
    }

    @JsonSetter("dataset-id")
    public void setDatasetId(long datasetId) {
        this.datasetId = datasetId;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        Path jsonFile = storageFilePath.resolve(name + ".json");
        try {
            JsonUtils.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JIPipeData duplicate() {
        return new OMERODatasetReferenceData(datasetId);
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    @Override
    public String toString() {
        return "OMERO dataset ID=" + datasetId;
    }

    public static OMERODatasetReferenceData importFrom(Path storageFilePath) {
        Path targetFile = PathUtils.findFileByExtensionIn(storageFilePath, ".json");
        try {
            return JsonUtils.getObjectMapper().readerFor(OMERODatasetReferenceData.class).readValue(targetFile.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
