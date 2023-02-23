package org.hkijena.jipipe.api.annotation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;

public class JIPipeDataAnnotation implements JIPipeAnnotation {
    private final String name;
    private final JIPipeDataItemStore virtualData;

    public JIPipeDataAnnotation(String name, JIPipeDataItemStore virtualData) {
        this.name = name;
        this.virtualData = virtualData;
    }

    public JIPipeDataAnnotation(String name, JIPipeData data) {
        this.name = name;
        this.virtualData = new JIPipeDataItemStore(data);
    }

    /**
     * Fully duplicates the data
     *
     * @return the copy
     */
    public JIPipeDataAnnotation duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeDataAnnotation(getName(), getVirtualData().duplicate(progressInfo));
    }

    public Class<? extends JIPipeData> getDataClass() {
        return virtualData.getDataClass();
    }

    @Override
    public String getName() {
        return name;
    }

    public JIPipeDataItemStore getVirtualData() {
        return virtualData;
    }

    public <T extends JIPipeData> T getData(Class<T> klass, JIPipeProgressInfo progressInfo) {
        return (T) JIPipe.getDataTypes().convert(virtualData.getData(progressInfo), klass);
    }

    @Override
    public String toString() {
        return "$" + name + "=" + virtualData.getStringRepresentation() + " [" + JIPipeDataInfo.getInstance(getDataClass()).getId() + "]";
    }
}
