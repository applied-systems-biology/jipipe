package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.api.JIPipeProgressInfo;

public class JIPipeDataAnnotation {
    private final String name;
    private final JIPipeVirtualData virtualData;

    public JIPipeDataAnnotation(String name, JIPipeVirtualData virtualData) {
        this.name = name;
        this.virtualData = virtualData;
    }

    public JIPipeDataAnnotation(String name, JIPipeData data) {
        this.name = name;
        this.virtualData = new JIPipeVirtualData(data);
    }

    public Class<? extends JIPipeData> getDataClass() {
        return virtualData.getDataClass();
    }

    public String getName() {
        return name;
    }

    public JIPipeVirtualData getVirtualData() {
        return virtualData;
    }

    public <T extends JIPipeData> T getData(Class<T> klass, JIPipeProgressInfo progressInfo) {
        return (T)virtualData.getData(progressInfo);
    }

    @Override
    public String toString() {
        return "$" + name + "=" + virtualData.getStringRepresentation() + " [" + JIPipeDataInfo.getInstance(getDataClass()).getId() + "]";
    }
}
