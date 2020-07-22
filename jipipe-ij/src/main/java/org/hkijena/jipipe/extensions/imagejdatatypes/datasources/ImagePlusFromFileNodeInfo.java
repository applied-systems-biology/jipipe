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

package org.hkijena.jipipe.extensions.imagejdatatypes.datasources;

import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.nodes.categories.DataSourceNodeTypeCategory;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Info for {@link ImagePlusFromFile}
 */
public class ImagePlusFromFileNodeInfo implements JIPipeNodeInfo {

    private String dataClassId;
    private Class<? extends JIPipeData> dataClass;
    private List<JIPipeInputSlot> inputSlots = new ArrayList<>();
    private List<JIPipeOutputSlot> outputSlots = new ArrayList<>();

    /**
     * @param dataClassId the data class ID
     * @param dataClass   the data class generated by the algorithm
     */
    public ImagePlusFromFileNodeInfo(String dataClassId, Class<? extends JIPipeData> dataClass) {
        this.dataClassId = dataClassId;
        this.dataClass = dataClass;
        inputSlots.add(new DefaultJIPipeInputSlot(FileData.class, "Input", false));
        outputSlots.add(new DefaultJIPipeOutputSlot(dataClass, "Image", "", false));
    }

    @Override
    public String getId() {
        return dataClassId + "-from-file";
    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return ImagePlusFromFile.class;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new ImagePlusFromFile(this, dataClass);
    }

    @Override
    public JIPipeGraphNode clone(JIPipeGraphNode algorithm) {
        return new ImagePlusFromFile((ImagePlusFromFile) algorithm);
    }

    @Override
    public String getName() {
        return "Import " + JIPipeData.getNameOf(dataClass);
    }

    @Override
    public String getDescription() {
        return "Loads an image via the native ImageJ functions. Please note that you might run into issues " +
                "if you open a file that is imported via Bio-Formats (for example .czi files). In such cases, please use the Bio-Formats importer algorithm.";
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public JIPipeNodeTypeCategory getCategory() {
        return new DataSourceNodeTypeCategory();
    }

    @Override
    public List<JIPipeInputSlot> getInputSlots() {
        return Collections.unmodifiableList(inputSlots);
    }

    @Override
    public List<JIPipeOutputSlot> getOutputSlots() {
        return Collections.unmodifiableList(outputSlots);
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public boolean isHidden() {
        return false;
    }
}
