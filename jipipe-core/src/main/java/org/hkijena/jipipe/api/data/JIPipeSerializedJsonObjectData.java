package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.nio.file.Path;

/**
 * A base class for {@link JIPipeData} that is serialized from/to JSON for convenience
 * Ensure that this data type has a copy constructor for the duplicate() function.
 * You also still need to add the proper {@link org.hkijena.jipipe.api.JIPipeDocumentation} annotation and
 * the JIPipeData importFrom(Path) static function.
 */
@JIPipeDataStorageDocumentation(humanReadableDescription = "A JSON file that contains the serialized data",
jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-json-data.schema.json")
public abstract class JIPipeSerializedJsonObjectData implements JIPipeData {

    public JIPipeSerializedJsonObjectData() {

    }

    public JIPipeSerializedJsonObjectData(JIPipeSerializedJsonObjectData other) {

    }

    public static <T extends JIPipeData> T importFrom(Path storagePath, Class<T> klass) {
        Path targetFile = PathUtils.findFileByExtensionIn(storagePath, ".json");
        return JsonUtils.readFromFile(targetFile, klass);
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {
        JsonUtils.saveToFile(this, storageFilePath.resolve(StringUtils.orElse(name, "data") + ".json"));
    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return (JIPipeData) ReflectionUtils.newInstance(getClass(), this);
    }
}
