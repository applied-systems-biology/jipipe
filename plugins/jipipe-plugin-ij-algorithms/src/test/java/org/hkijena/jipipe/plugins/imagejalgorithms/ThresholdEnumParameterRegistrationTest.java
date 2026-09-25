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

package org.hkijena.jipipe.plugins.imagejalgorithms;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.AutoThreshold2DAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.local.LocalAutoThreshold2D16UAlgorithm;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.local.LocalAutoThreshold2DAlgorithm;
import org.hkijena.jipipe.utils.threshold.AutoThresholdMethod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Guards the threshold-related enum parameter registrations: every enum used as a node
 * parameter must be registered, and its ID must be the stable string literal that saved
 * projects contain (never a code-derived canonical name).
 */
class ThresholdEnumParameterRegistrationTest {

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @Test
    void autoThresholdMethodIsRegisteredWithLegacyId() {
        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(AutoThresholdMethod.class);
        assertNotNull(info, "AutoThresholdMethod must be registered as parameter type");
        // The ID predates the enum (it was ij.process.AutoThresholder$Method) and is
        // persisted in saved projects; it must never change.
        assertEquals("ij.process.AutoThresholder$Method", info.getId());
    }

    @Test
    void localAutoThresholdMethodEnumsAreRegistered() {
        JIPipeParameterTypeInfo info8 = JIPipe.getParameterTypes().getInfoByFieldClass(LocalAutoThreshold2DAlgorithm.Method.class);
        assertNotNull(info8, "LocalAutoThreshold2DAlgorithm.Method must be registered as parameter type");
        assertEquals("org.hkijena.jipipe.plugins.imagejalgorithms.nodes.threshold.local.LocalAutoThreshold2DAlgorithm$Method",
                info8.getId(), "ID must be the frozen legacy value persisted in saved projects");

        JIPipeParameterTypeInfo info16 = JIPipe.getParameterTypes().getInfoByFieldClass(LocalAutoThreshold2D16UAlgorithm.Method.class);
        assertNotNull(info16, "LocalAutoThreshold2D16UAlgorithm.Method must be registered as parameter type");
        assertEquals("ij1-threshold-local-auto2d-16u:method", info16.getId());
    }

    @Test
    void sliceThresholdModeIdIsStable() {
        JIPipeParameterTypeInfo info = JIPipe.getParameterTypes().getInfoByFieldClass(AutoThreshold2DAlgorithm.SliceThresholdMode.class);
        assertNotNull(info, "SliceThresholdMode must be registered as parameter type");
        assertEquals("slice-threshold-mode", info.getId());
    }
}
