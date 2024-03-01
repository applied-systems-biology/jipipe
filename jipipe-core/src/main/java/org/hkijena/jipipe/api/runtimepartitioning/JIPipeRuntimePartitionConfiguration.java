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

package org.hkijena.jipipe.api.runtimepartitioning;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.extensions.expressions.functions.math.RandomFunction;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.ui.components.icons.SolidColorIcon;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class JIPipeRuntimePartitionConfiguration {
    private JIPipeRuntimePartition defaultRuntimePartition = new JIPipeRuntimePartition();
    private List<JIPipeRuntimePartition> extraRuntimePartitions = new ArrayList<>();

    public JIPipeRuntimePartitionConfiguration() {
        defaultRuntimePartition.setName("Default");
        defaultRuntimePartition.getColor().setEnabled(false);
    }

    /**
     * Returns a runtime partition with the specific index
     *
     * @param index the index. if negative, the default partition is returned.
     * @return the partition. will not be null (fallback to the default partition of needed)
     */
    public JIPipeRuntimePartition get(int index) {
        if(index <= 0 || index > extraRuntimePartitions.size()) {
            return defaultRuntimePartition;
        }
        return extraRuntimePartitions.get(index - 1);
    }

    @JsonGetter("default")
    public JIPipeRuntimePartition getDefaultRuntimePartition() {
        return defaultRuntimePartition;
    }

    @JsonGetter("extra")
    public List<JIPipeRuntimePartition> getExtraRuntimePartitions() {
        return Collections.unmodifiableList(extraRuntimePartitions);
    }

    @JsonSetter("default")
    public void setDefaultRuntimePartition(JIPipeRuntimePartition defaultRuntimePartition) {
        this.defaultRuntimePartition = defaultRuntimePartition;
    }

    @JsonSetter("extra")
    public void setExtraRuntimePartitions(List<JIPipeRuntimePartition> extraRuntimePartitions) {
        this.extraRuntimePartitions = extraRuntimePartitions;
    }

    /**
     * Adds a new runtime partition. The color is automatically generated.
     * @return the new partition
     */
    public JIPipeRuntimePartition add() {
        JIPipeRuntimePartition partition = new JIPipeRuntimePartition();
        partition.setName("Partition " + (extraRuntimePartitions.size() + 1));

        final float baseSaturation = 1.2f / 100f;
        final float baseValue = 98.04f / 100f;

        // Find a proper hue
        Set<Integer> usedHues = new HashSet<>();
        if(defaultRuntimePartition.getColor().isEnabled()) {
            float[] hsb = Color.RGBtoHSB(defaultRuntimePartition.getColor().getContent().getRed(),
                    defaultRuntimePartition.getColor().getContent().getGreen(),
                    defaultRuntimePartition.getColor().getContent().getBlue(),
                    null);
            usedHues.add(Math.round(hsb[0] * 10));
        }
        for (JIPipeRuntimePartition runtimePartition : extraRuntimePartitions) {
            float[] hsb = Color.RGBtoHSB(runtimePartition.getColor().getContent().getRed(),
                    runtimePartition.getColor().getContent().getGreen(),
                    runtimePartition.getColor().getContent().getBlue(),
                    null);
            usedHues.add(Math.round(hsb[0] * 10));
        }
        if(usedHues.size() >= 9) {
            for (int i = 0; i < 10; i++) {
                if(!usedHues.contains(i)) {
                    partition.setColor(new OptionalColorParameter(Color.getHSBColor(i / 10.0f, baseSaturation, baseValue), true));
                }
            }
        }
        else {
            partition.setColor(new OptionalColorParameter(Color.getHSBColor(RandomFunction.RANDOM.nextFloat(), baseSaturation, baseValue), true));
        }

        extraRuntimePartitions.add(partition);
        return partition;
    }

    public void add(JIPipeRuntimePartition partition) {
        extraRuntimePartitions.add(partition);
    }

    public void remove(int index) {
        if(index <= 0) {
            throw new IndexOutOfBoundsException("Cannot remove default partition");
        }
        extraRuntimePartitions.remove(index - 1);
    }

    public int indexOf(JIPipeRuntimePartition partition) {
        if(partition == defaultRuntimePartition) {
            return 0;
        }
        else {
            int index = extraRuntimePartitions.indexOf(partition);
            if(index != -1) {
                return index + 1;
            }
            else {
                return -1;
            }
        }
    }

    public int size() {
        return extraRuntimePartitions.size() + 1;
    }

    public List<JIPipeRuntimePartition> toList() {
        List<JIPipeRuntimePartition> result = new ArrayList<>();
        result.add(defaultRuntimePartition);
        result.addAll(extraRuntimePartitions);
        return result;
    }

    public String getFullName(JIPipeRuntimePartition value) {
        int idx = indexOf(value);
        String nameText;
        if(idx >= 0 ) {
            nameText = idx == 0 ? StringUtils.orElse(value.getName(), "Default") : StringUtils.orElse(value.getName(), "Unnamed");
        }
        else {
            nameText = StringUtils.orElse(value.getName(), "Unnamed");
        }
        return nameText + " (Partition " + idx + ")";
    }

    public Icon getIcon(JIPipeRuntimePartition runtimePartition) {
        if(runtimePartition.getColor().isEnabled()) {
            return new SolidColorIcon(16, 16, runtimePartition.getColor().getContent());
        }
        else {
            return UIUtils.getIconFromResources("actions/runtime-partition.png");
        }
    }
}
