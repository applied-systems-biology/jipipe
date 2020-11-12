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

package org.hkijena.jipipe;

import net.imagej.ImageJ;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatch;
import org.hkijena.jipipe.api.nodes.JIPipeMergingDataBatchBuilder;
import org.hkijena.jipipe.extensions.settings.ExtensionSettings;
import org.hkijena.jipipe.extensions.strings.StringData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BatchGenerationTests {

    /**
     * A simple test where one slot should be split into three batches (one for A, B, and C)
     */
    @Test
    public void simpleBatchGenerationTest() {
        JIPipeDataSlot slot1 = new JIPipeDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot1", null), null);
        slot1.addData(new StringData("A"), Collections.singletonList(new JIPipeAnnotation("C1", "A")));
        slot1.addData(new StringData("B"), Collections.singletonList(new JIPipeAnnotation("C1", "B")));
        slot1.addData(new StringData("C"), Collections.singletonList(new JIPipeAnnotation("C1", "C")));
        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy.Merge);
        builder.setReferenceColumns(new HashSet<>(Collections.singletonList("C1")));
        builder.setSlots(Collections.singletonList(slot1));
        List<JIPipeMergingDataBatch> batches = builder.build();
        assertEquals(3, batches.size());
    }

    /**
     * A simple test where two slots should be split into three batches. The second slot does not have any reference columns and contains only one data item.
     * It should be assigned to A, B and C
     */
    @Test
    public void simpleAssignmentBatchGenerationTest() {
        JIPipeDataSlot slot1 = new JIPipeDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot1", null), null);
        JIPipeDataSlot slot2 = new JIPipeDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot2", null), null);
        slot1.addData(new StringData("A"), Collections.singletonList(new JIPipeAnnotation("C1", "A")));
        slot1.addData(new StringData("B"), Collections.singletonList(new JIPipeAnnotation("C1", "B")));
        slot1.addData(new StringData("C"), Collections.singletonList(new JIPipeAnnotation("C1", "C")));
        slot2.addData(new StringData("N"), Collections.singletonList(new JIPipeAnnotation("C2", "N")));

        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy.Merge);
        builder.setReferenceColumns(new HashSet<>(Collections.singletonList("C1")));
        builder.setSlots(Arrays.asList(slot1, slot2));
        List<JIPipeMergingDataBatch> batches = builder.build();
        assertEquals(3, batches.size());
    }

    /**
     * A test where each row consists of one data set, but has two columns that are referenced.
     * Should still create three batches.
     */
    @Test
    public void twoColumnBatchGenerationTest() {
        JIPipeDataSlot slot1 = new JIPipeDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot1", null), null);
        slot1.addData(new StringData("A"), Arrays.asList(new JIPipeAnnotation("C1", "A"), new JIPipeAnnotation("C2", "X")));
        slot1.addData(new StringData("B"), Arrays.asList(new JIPipeAnnotation("C1", "B"), new JIPipeAnnotation("C2", "Y")));
        slot1.addData(new StringData("C"), Arrays.asList(new JIPipeAnnotation("C1", "C"), new JIPipeAnnotation("C3", "Z")));
        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy.Merge);
        builder.setReferenceColumns(new HashSet<>(Arrays.asList("C1", "C2")));
        builder.setSlots(Collections.singletonList(slot1));
        List<JIPipeMergingDataBatch> batches = builder.build();
        assertEquals(3, batches.size());
    }

    /**
     * A test where each row consists of one data set, but has two columns that are referenced.
     * Should still create three batches.
     */
    @Test
    public void equalTwoSlotTest() {
        JIPipeDataSlot slot1 = new JIPipeDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot1", null), null);
        slot1.addData(new StringData("A"), Arrays.asList(new JIPipeAnnotation("C1", "A"), new JIPipeAnnotation("C2", "X")));
        slot1.addData(new StringData("B"), Arrays.asList(new JIPipeAnnotation("C1", "B"), new JIPipeAnnotation("C2", "Y")));
        slot1.addData(new StringData("C"), Arrays.asList(new JIPipeAnnotation("C1", "C"), new JIPipeAnnotation("C3", "Z")));

        JIPipeDataSlot slot2 = new JIPipeDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot2", null), null);
        slot2.addData(new StringData("A"), Arrays.asList(new JIPipeAnnotation("C1", "A"), new JIPipeAnnotation("C2", "X")));
        slot2.addData(new StringData("B"), Arrays.asList(new JIPipeAnnotation("C1", "B"), new JIPipeAnnotation("C2", "Y")));
        slot2.addData(new StringData("C"), Arrays.asList(new JIPipeAnnotation("C1", "C"), new JIPipeAnnotation("C3", "Z")));

        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setAnnotationMergeStrategy(JIPipeAnnotationMergeStrategy.Merge);
        builder.setReferenceColumns(new HashSet<>(Arrays.asList("C1", "C2")));
        builder.setSlots(Arrays.asList(slot1, slot2));
        List<JIPipeMergingDataBatch> batches = builder.build();
        assertEquals(3, batches.size());
    }

    @BeforeAll
    public static void setupJIPipe() {
        ImageJ imageJ = new ImageJ();
        JIPipe jiPipe = JIPipe.createInstance(imageJ.context());
        ExtensionSettings settings = new ExtensionSettings();
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        jiPipe.initialize(settings, issues);
    }
}
