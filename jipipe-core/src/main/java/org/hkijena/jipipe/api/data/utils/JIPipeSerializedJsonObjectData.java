package org.hkijena.jipipe.api.data.utils;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Path;

/**
 * A base class for {@link JIPipeData} that is serialized from/to JSON for convenience
 * Ensure that this data type has a copy constructor for the duplicate() function.
 * You also still need to add the proper {@link SetJIPipeDocumentation} annotation and
 * the JIPipeData importData(Path) static function.
 */
@JIPipeDataStorageDocumentation(humanReadableDescription = "A JSON file that contains the serialized data",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public abstract class JIPipeSerializedJsonObjectData implements JIPipeData {

    public JIPipeSerializedJsonObjectData() {

    }

    public JIPipeSerializedJsonObjectData(JIPipeSerializedJsonObjectData other) {

    }

    public static <T extends JIPipeData> T importData(JIPipeReadDataStorage storage, Class<T> klass) {
        Path targetFile = PathUtils.findFileByExtensionIn(storage.getFileSystemPath(), ".json");
        return JsonUtils.readFromFile(targetFile, klass);
    }

    @Override
    public void exportData(JIPipeWriteDataStorage storage, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        JsonUtils.saveToFile(this, storage.getFileSystemPath().resolve(StringUtils.orElse(name, "data") + ".json"));
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return (JIPipeData) ReflectionUtils.newInstance(getClass(), this);
    }
}
