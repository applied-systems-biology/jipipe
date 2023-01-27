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
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotation;
import org.hkijena.jipipe.api.annotation.JIPipeTextAnnotationMergeMode;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeInputDataSlot;
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

    @BeforeAll
    public static void setupJIPipe() {
        ImageJ imageJ = new ImageJ();
        JIPipe jiPipe = JIPipe.createInstance(imageJ.context());
        ExtensionSettings settings = new ExtensionSettings();
        JIPipeRegistryIssues issues = new JIPipeRegistryIssues();
        jiPipe.initialize(settings, issues);
    }

    /**
     * A simple test where one slot should be split into three batches (one for A, B, and C)
     */
    @Test
    public void simpleBatchGenerationTest() {
        JIPipeInputDataSlot slot1 = new JIPipeInputDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot1", ""), null);
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        slot1.addData(new StringData("A"), Collections.singletonList(new JIPipeTextAnnotation("C1", "A")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot1.addData(new StringData("B"), Collections.singletonList(new JIPipeTextAnnotation("C1", "B")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot1.addData(new StringData("C"), Collections.singletonList(new JIPipeTextAnnotation("C1", "C")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode.Merge);
        builder.setReferenceColumns(new HashSet<>(Collections.singletonList("C1")));
        builder.setSlots(Collections.singletonList(slot1));
        List<JIPipeMergingDataBatch> batches = builder.build(new JIPipeProgressInfo());
        assertEquals(3, batches.size());
    }

    /**
     * A simple test where two slots should be split into three batches. The second slot does not have any reference columns and contains only one data item.
     * It should be assigned to A, B and C
     */
    @Test
    public void simpleAssignmentBatchGenerationTest() {
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        JIPipeInputDataSlot slot1 = new JIPipeInputDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot1", ""), null);
        JIPipeInputDataSlot slot2 = new JIPipeInputDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot2", ""), null);
        slot1.addData(new StringData("A"), Collections.singletonList(new JIPipeTextAnnotation("C1", "A")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot1.addData(new StringData("B"), Collections.singletonList(new JIPipeTextAnnotation("C1", "B")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot1.addData(new StringData("C"), Collections.singletonList(new JIPipeTextAnnotation("C1", "C")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot2.addData(new StringData("N"), Collections.singletonList(new JIPipeTextAnnotation("C2", "N")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);

        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode.Merge);
        builder.setReferenceColumns(new HashSet<>(Collections.singletonList("C1")));
        builder.setSlots(Arrays.asList(slot1, slot2));
        List<JIPipeMergingDataBatch> batches = builder.build(new JIPipeProgressInfo());
        assertEquals(3, batches.size());
    }

    /**
     * A test where each row consists of one data set, but has two columns that are referenced.
     * Should still create three batches.
     */
    @Test
    public void twoColumnBatchGenerationTest() {
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        JIPipeInputDataSlot slot1 = new JIPipeInputDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot1", ""), null);
        slot1.addData(new StringData("A"), Arrays.asList(new JIPipeTextAnnotation("C1", "A"), new JIPipeTextAnnotation("C2", "X")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot1.addData(new StringData("B"), Arrays.asList(new JIPipeTextAnnotation("C1", "B"), new JIPipeTextAnnotation("C2", "Y")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot1.addData(new StringData("C"), Arrays.asList(new JIPipeTextAnnotation("C1", "C"), new JIPipeTextAnnotation("C3", "Z")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode.Merge);
        builder.setReferenceColumns(new HashSet<>(Arrays.asList("C1", "C2")));
        builder.setSlots(Collections.singletonList(slot1));
        List<JIPipeMergingDataBatch> batches = builder.build(new JIPipeProgressInfo());
        assertEquals(3, batches.size());
    }

    /**
     * A test where each row consists of one data set, but has two columns that are referenced.
     * Should still create three batches.
     */
    @Test
    public void equalTwoSlotTest() {
        JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
        JIPipeInputDataSlot slot1 = new JIPipeInputDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot1", ""), null);
        slot1.addData(new StringData("A"), Arrays.asList(new JIPipeTextAnnotation("C1", "A"), new JIPipeTextAnnotation("C2", "X")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot1.addData(new StringData("B"), Arrays.asList(new JIPipeTextAnnotation("C1", "B"), new JIPipeTextAnnotation("C2", "Y")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot1.addData(new StringData("C"), Arrays.asList(new JIPipeTextAnnotation("C1", "C"), new JIPipeTextAnnotation("C3", "Z")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);

        JIPipeInputDataSlot slot2 = new JIPipeInputDataSlot(new JIPipeDataSlotInfo(StringData.class, JIPipeSlotType.Input, "slot2", ""), null);
        slot2.addData(new StringData("A"), Arrays.asList(new JIPipeTextAnnotation("C1", "A"), new JIPipeTextAnnotation("C2", "X")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot2.addData(new StringData("B"), Arrays.asList(new JIPipeTextAnnotation("C1", "B"), new JIPipeTextAnnotation("C2", "Y")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);
        slot2.addData(new StringData("C"), Arrays.asList(new JIPipeTextAnnotation("C1", "C"), new JIPipeTextAnnotation("C3", "Z")), JIPipeTextAnnotationMergeMode.Merge, progressInfo);

        JIPipeMergingDataBatchBuilder builder = new JIPipeMergingDataBatchBuilder();
        builder.setAnnotationMergeStrategy(JIPipeTextAnnotationMergeMode.Merge);
        builder.setReferenceColumns(new HashSet<>(Arrays.asList("C1", "C2")));
        builder.setSlots(Arrays.asList(slot1, slot2));
        List<JIPipeMergingDataBatch> batches = builder.build(new JIPipeProgressInfo());
        assertEquals(3, batches.size());
    }
}
