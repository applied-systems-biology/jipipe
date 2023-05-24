package org.hkijena.jipipe.extensions.scene3d.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeHeavyData;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeSerializedJsonObjectData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.data.storage.JIPipeWriteDataStorage;
import org.hkijena.jipipe.extensions.strings.JsonData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

@JIPipeDocumentation(name = "3D scene", description = "3D objects arranged in a scene")
@JIPipeHeavyData
public class Scene3DData extends JIPipeSerializedJsonObjectData {
    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }

    public static Scene3DData importData(JIPipeReadDataStorage storage, JIPipeProgressInfo progressInfo) {
        return JIPipeSerializedJsonObjectData.importData(storage, Scene3DData.class);
    }
}
