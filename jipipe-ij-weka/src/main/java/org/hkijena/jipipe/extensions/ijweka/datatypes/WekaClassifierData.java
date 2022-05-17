package org.hkijena.jipipe.extensions.ijweka.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.JIPipeSerializedParameterCollectionData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import weka.classifiers.AbstractClassifier;

import java.nio.file.Path;

@JIPipeDocumentation(name = "Weka classifier", description = "A Weka classifier")
@JIPipeDataStorageDocumentation(humanReadableDescription = "A JSON file that contains the serialized data",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public abstract class WekaClassifierData extends JIPipeSerializedParameterCollectionData {

    public WekaClassifierData() {
    }

    public WekaClassifierData(WekaClassifierData other) {
        super(other);
    }

    public static JIPipeData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
       return JIPipeSerializedParameterCollectionData.importData(storage, progressInfo);
    }

    /**
     * Creates a new instance of the classifier described by this data
     * @return the instance
     */
    public abstract AbstractClassifier newClassifierInstance();
}
