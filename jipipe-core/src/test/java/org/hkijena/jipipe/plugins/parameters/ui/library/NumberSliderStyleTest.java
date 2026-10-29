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

package org.hkijena.jipipe.plugins.parameters.ui.library;

import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.NumberParameterSettings;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the editor's pure {@code shouldUseSlider} decision and the annotation
 * default (#1339 pass 2, S6). Lives in the editor package (next to
 * {@code shouldUseSlider}); the slider's static math is pinned separately by
 * {@code NumberSliderMathTest} in the slider package.
 */
class NumberSliderStyleTest {
    @Test
    void annotationDefaultIsField() throws Exception {
        // A getter annotated with @NumberParameterSettings and no style() -> FIELD.
        Method m = SliderProbe.class.getMethod("getA");
        NumberParameterSettings s = m.getAnnotation(NumberParameterSettings.class);
        assertEquals(NumberParameterSettings.Style.FIELD, s.style());
    }

    @Test
    void shouldUseSliderBoundedOnly() {
        NumberParameterSettings slider = new NumberParameterSettings() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return NumberParameterSettings.class;
            }

            @Override
            public double min() {
                return 0;
            }

            @Override
            public double max() {
                return 1;
            }

            @Override
            public double step() {
                return 0.01;
            }

            @Override
            public NumberParameterSettings.Style style() {
                return NumberParameterSettings.Style.SLIDER;
            }
        };
        NumberParameterSettings field = new NumberParameterSettings() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return NumberParameterSettings.class;
            }

            @Override
            public double min() {
                return Double.NEGATIVE_INFINITY;
            }

            @Override
            public double max() {
                return Double.POSITIVE_INFINITY;
            }

            @Override
            public double step() {
                return 1;
            }

            @Override
            public NumberParameterSettings.Style style() {
                return NumberParameterSettings.Style.FIELD;
            }
        };
        // bounded + SLIDER -> slider
        assertTrue(JIPipeDesktopNumberParameterEditorUI.shouldUseSlider(slider, 0, 1));
        // unbounded (defaults) + SLIDER style -> FIELD fallback (Review Focus #2)
        assertFalse(JIPipeDesktopNumberParameterEditorUI.shouldUseSlider(slider, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        // default settings (FIELD) -> never slider
        assertFalse(JIPipeDesktopNumberParameterEditorUI.shouldUseSlider(field, 0, 1));
        // no settings at all -> never slider
        assertFalse(JIPipeDesktopNumberParameterEditorUI.shouldUseSlider(null, 0, 1));
    }

    static class SliderProbe {
        @JIPipeParameter("a")
        @NumberParameterSettings(min = 0, max = 1, step = 0.01)
        public double getA() {
            return 0;
        }
    }
}
