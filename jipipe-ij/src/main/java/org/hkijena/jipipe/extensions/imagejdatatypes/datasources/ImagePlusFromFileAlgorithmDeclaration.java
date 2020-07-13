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
import org.hkijena.jipipe.api.algorithm.*;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.filesystem.dataypes.FileData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Declaration for {@link ImagePlusFromFile}
 */
public class ImagePlusFromFileAlgorithmDeclaration implements JIPipeAlgorithmDeclaration {

    private String dataClassId;
    private Class<? extends JIPipeData> dataClass;
    private List<JIPipeInputSlot> inputSlots = new ArrayList<>();
    private List<JIPipeOutputSlot> outputSlots = new ArrayList<>();

    /**
     * @param dataClassId the data class ID
     * @param dataClass   the data class generated by the algorithm
     */
    public ImagePlusFromFileAlgorithmDeclaration(String dataClassId, Class<? extends JIPipeData> dataClass) {
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
    public Class<? extends JIPipeGraphNode> getAlgorithmClass() {
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
        return "Imports an image via native ImageJ functions";
    }

    @Override
    public String getMenuPath() {
        return "";
    }

    @Override
    public JIPipeAlgorithmCategory getCategory() {
        return JIPipeAlgorithmCategory.DataSource;
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
