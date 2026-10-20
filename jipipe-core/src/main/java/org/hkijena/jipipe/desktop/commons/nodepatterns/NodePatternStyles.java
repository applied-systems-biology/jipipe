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

package org.hkijena.jipipe.desktop.commons.nodepatterns;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry of bundled node pattern styles. Ids are stable string literals. */
public final class NodePatternStyles {
    private static final Map<String, NodePatternFamily> STYLES = new LinkedHashMap<>();

    static {
        // Task 1 (algorithmic representatives)
        STYLES.put("stripes", new StripesFamily());
        STYLES.put("net", new NetFamily());
        STYLES.put("cellular", new CellularFamily());
        STYLES.put("carpet", new CarpetFamily());
        STYLES.put("hilbert", new HilbertFamily());
        STYLES.put("landscape", new LandscapeFamily());
        // Task 2 (complete the 17)
        STYLES.put("waves", new WavesFamily());
        STYLES.put("weave", new WeaveFamily());
        STYLES.put("patchwork", new PatchworkFamily());
        STYLES.put("loops", new LoopsFamily());
        STYLES.put("disco", new DiscoFamily());
        STYLES.put("triangles", new TrianglesFamily());
        STYLES.put("blobs", new BlobsFamily());
        STYLES.put("dots", new DotsFamily());
        STYLES.put("honeycomb", new HoneycombFamily());
        STYLES.put("ripple", new RippleFamily());
        STYLES.put("flow", new FlowFamily());
    }

    private NodePatternStyles() {}

    /** @return the family for {@code id}, or {@code null} for an unknown id */
    public static NodePatternFamily family(String id) {
        return id == null ? null : STYLES.get(id);
    }

    public static Collection<String> ids() {
        return Collections.unmodifiableCollection(STYLES.keySet());
    }
}
