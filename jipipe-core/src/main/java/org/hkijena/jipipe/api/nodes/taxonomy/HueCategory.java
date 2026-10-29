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

package org.hkijena.jipipe.api.nodes.taxonomy;

/**
 * The six research-anchored hue families used by the deterministic hue
 * auto-picker ({@link TaxonomyHue#autoHue(HueCategory, String)}).
 * <p>
 * The anchors come from human color categorization research (basic color
 * terms, Sturges &amp; Whitfield 1995): humans reliably distinguish six
 * hue-only families at red 0°, orange 30°, yellow 60°, green 120°,
 * blue 240° and purple 300°. Hue acuity is non-uniform, so each family's
 * Voronoi cell is intentionally uneven: wide cells where humans are weak
 * (green, blue), narrow cells where they are acute (red, orange, yellow).
 * A cell spans from the midpoint to the previous anchor to the midpoint to
 * the next, wrapping around the hue circle.
 */
public enum HueCategory {

    /**
     * Red anchor (0°) — regions, shapes, ROIs, filaments, skeletons, surfaces. Cell [330°, 15°).
     */
    GEOMETRY("Geometry", 0f),
    /**
     * Orange anchor (30°) — statistics, measurement, quantification, tracking/motion analysis. Cell [15°, 45°).
     */
    ANALYSIS("Analysis", 30f),
    /**
     * Yellow anchor (60°) — plotting, charts, figures. Cell [45°, 90°).
     */
    VISUALIZATION("Visualization", 60f),
    /**
     * Green anchor (120°) — input, sources, export, data platforms, metadata transport. Cell [90°, 180°).
     */
    EXCHANGE("Exchange", 120f),
    /**
     * Blue anchor (240°) — pixel data and tabular data. Cell [180°, 270°).
     */
    DATA("Data", 240f),
    /**
     * Purple anchor (300°) — machine learning, annotations, scripting, expressions, automation. Cell [270°, 330°).
     */
    INTELLIGENCE("Intelligence", 300f);

    private final String label;

    /**
     * The focal hue anchor of this family in degrees ({@code 0..360}).
     */
    private final float anchorDeg;

    HueCategory(String label, float anchorDeg) {
        this.label = label;
        this.anchorDeg = anchorDeg;
    }

    /**
     * @return the display label of this category
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the focal hue anchor of this category in degrees ({@code 0..360})
     */
    public float getAnchorDeg() {
        return anchorDeg;
    }
}
