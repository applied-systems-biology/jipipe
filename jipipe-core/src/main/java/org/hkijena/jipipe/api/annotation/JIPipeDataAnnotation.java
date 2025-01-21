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

package org.hkijena.jipipe.api.annotation;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;

public class JIPipeDataAnnotation implements JIPipeAnnotation {
    private final String name;
    private final JIPipeDataItemStore dataItemStore;

    public JIPipeDataAnnotation(String name, JIPipeDataItemStore dataItemStore) {
        this.name = name;
        this.dataItemStore = dataItemStore;
    }

    public JIPipeDataAnnotation(String name, JIPipeData data) {
        this.name = name;
        this.dataItemStore = new JIPipeDataItemStore(data);
    }

    /**
     * Fully duplicates the data
     *
     * @return the copy
     */
    public JIPipeDataAnnotation duplicate(JIPipeProgressInfo progressInfo) {
        return new JIPipeDataAnnotation(getName(), getDataItemStore().duplicate(progressInfo));
    }

    public Class<? extends JIPipeData> getDataClass() {
        return dataItemStore.getDataClass();
    }

    @Override
    public String getName() {
        return name;
    }

    public JIPipeDataItemStore getDataItemStore() {
        return dataItemStore;
    }

    public <T extends JIPipeData> T getData(Class<T> klass, JIPipeProgressInfo progressInfo) {
        return (T) JIPipe.getDataTypes().convert(dataItemStore.getData(progressInfo), klass, progressInfo);
    }

    @Override
    public String toString() {
        return "$" + name + "=" + dataItemStore.getStringRepresentation() + " [" + JIPipeDataInfo.getInstance(getDataClass()).getId() + "]";
    }

    public JIPipeDataInfo getDataInfo() {
        return JIPipeDataInfo.getInstance(getDataClass());
    }
}
