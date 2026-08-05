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

package org.hkijena.jipipe.desktop.commons.components.layouts;

import java.util.HashMap;
import java.util.Map;

public class JIPipeDesktopColumnConstraints {
    private final int defaultSpan;
    private final int defaultOffset;
    private final Map<JIPipeDesktopBreakpoint, Integer> spanOverrides;
    private final Map<JIPipeDesktopBreakpoint, Integer> offsetOverrides;

    public JIPipeDesktopColumnConstraints(int defaultSpan) {
        this(defaultSpan, 0);
    }

    private JIPipeDesktopColumnConstraints(int defaultSpan, int defaultOffset,
                                           Map<JIPipeDesktopBreakpoint, Integer> spanOverrides,
                                           Map<JIPipeDesktopBreakpoint, Integer> offsetOverrides) {
        this.defaultSpan = defaultSpan;
        this.defaultOffset = defaultOffset;
        this.spanOverrides = spanOverrides;
        this.offsetOverrides = offsetOverrides;
    }

    private JIPipeDesktopColumnConstraints(int defaultSpan, int defaultOffset) {
        this(defaultSpan, defaultOffset, new HashMap<>(), new HashMap<>());
    }

    public JIPipeDesktopColumnConstraints withSpan(JIPipeDesktopBreakpoint breakpoint, int span) {
        Map<JIPipeDesktopBreakpoint, Integer> copy = new HashMap<>(spanOverrides);
        copy.put(breakpoint, span);
        return new JIPipeDesktopColumnConstraints(defaultSpan, defaultOffset, copy, offsetOverrides);
    }

    public JIPipeDesktopColumnConstraints withOffset(int offset) {
        return new JIPipeDesktopColumnConstraints(defaultSpan, offset, spanOverrides, offsetOverrides);
    }

    public JIPipeDesktopColumnConstraints withOffset(JIPipeDesktopBreakpoint breakpoint, int offset) {
        Map<JIPipeDesktopBreakpoint, Integer> copy = new HashMap<>(offsetOverrides);
        copy.put(breakpoint, offset);
        return new JIPipeDesktopColumnConstraints(defaultSpan, defaultOffset, spanOverrides, copy);
    }

    public int getEffectiveSpan(JIPipeDesktopBreakpoint breakpoint) {
        JIPipeDesktopBreakpoint[] order = JIPipeDesktopBreakpoint.values();
        for (int i = breakpoint.ordinal(); i >= 0; i--) {
            Integer override = spanOverrides.get(order[i]);
            if (override != null) {
                return override;
            }
        }
        return defaultSpan;
    }

    public int getEffectiveOffset(JIPipeDesktopBreakpoint breakpoint) {
        JIPipeDesktopBreakpoint[] order = JIPipeDesktopBreakpoint.values();
        for (int i = breakpoint.ordinal(); i >= 0; i--) {
            Integer override = offsetOverrides.get(order[i]);
            if (override != null) {
                return override;
            }
        }
        return defaultOffset;
    }
}
