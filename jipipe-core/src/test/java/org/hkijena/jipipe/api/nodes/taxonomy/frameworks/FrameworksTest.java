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

package org.hkijena.jipipe.api.nodes.taxonomy.frameworks;

import org.hkijena.jipipe.api.nodes.taxonomy.FrameworkRegistry;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the 14 seed {@link JIPipeNodeTaxonomyFramework}s (the exact
 * table in the Phase 1a plan for Task 11, plus the Python and R frameworks
 * of the scripting re-clustering audit): the exact stable literal ids,
 * display names, abbreviations (at most 4 characters, unique) and
 * DiceBear pattern style ids, a clean {@link FrameworkRegistry#validate()}
 * for the full set, and the stable default color mapping per id.
 */
class FrameworksTest {

    /**
     * The 14 seed framework class literals, in the order of the plan's table,
     * plus the Python and R frameworks of the scripting re-clustering audit.
     */
    private static List<Class<? extends JIPipeNodeTaxonomyFramework>> allFrameworks() {
        return List.of(
            CoreFramework.class,
            PythonFramework.class,
            RFramework.class,
            Cellpose2Framework.class,
            Cellpose3Framework.class,
            Cellpose4Framework.class,
            TrackMateFramework.class,
            CometFramework.class,
            JIPipeDLFramework.class,
            JFreeChartFramework.class,
            OpenCVFramework.class,
            ClijFramework.class,
            ImageJ1Framework.class,
            ImageJ2Framework.class
        );
    }

    private static JIPipeNodeTaxonomyFramework instantiate(Class<? extends JIPipeNodeTaxonomyFramework> framework) {
        try {
            return framework.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not instantiate " + framework.getName(), e);
        }
    }

    @Test
    void idsAreUniqueStableLiterals() {
        Set<String> ids = new HashSet<>();
        for (Class<? extends JIPipeNodeTaxonomyFramework> framework : allFrameworks()) {
            String id = instantiate(framework).getId();
            assertTrue(id != null && id.startsWith("jipipe:framework:"),
                "Id of " + framework.getName() + " must be a non-null literal starting with 'jipipe:framework:', got: " + id);
            ids.add(id);
        }
        assertEquals(14, ids.size(), "Expected 14 unique framework ids, got " + ids.size() + " for: " + ids);
        assertFalse(ids.contains(new NoFramework().getId()),
            "A seed framework must not collide with the NoFramework sentinel id");
    }

    @Test
    void idsMatchTheExactSeedLiterals() {
        assertEquals("jipipe:framework:core", new CoreFramework().getId());
        assertEquals("jipipe:framework:cellpose2", new Cellpose2Framework().getId());
        assertEquals("jipipe:framework:cellpose3", new Cellpose3Framework().getId());
        assertEquals("jipipe:framework:cellpose4", new Cellpose4Framework().getId());
        assertEquals("jipipe:framework:trackmate", new TrackMateFramework().getId());
        assertEquals("jipipe:framework:comet", new CometFramework().getId());
        assertEquals("jipipe:framework:jipipedl", new JIPipeDLFramework().getId());
        assertEquals("jipipe:framework:jfreechart", new JFreeChartFramework().getId());
        assertEquals("jipipe:framework:opencv", new OpenCVFramework().getId());
        assertEquals("jipipe:framework:clij", new ClijFramework().getId());
        assertEquals("jipipe:framework:imagej1", new ImageJ1Framework().getId());
        assertEquals("jipipe:framework:imagej2", new ImageJ2Framework().getId());
    }

    @Test
    void namesAndPatternStyleIdsMatchTheSeedTable() {
        assertEquals("Core", new CoreFramework().getName());
        assertEquals("initials", new CoreFramework().getPatternStyleId());
        assertEquals("Cellpose 2", new Cellpose2Framework().getName());
        assertEquals("blobs", new Cellpose2Framework().getPatternStyleId());
        assertEquals("Cellpose 3", new Cellpose3Framework().getName());
        assertEquals("blobs", new Cellpose3Framework().getPatternStyleId());
        assertEquals("Cellpose 4", new Cellpose4Framework().getName());
        assertEquals("blobs", new Cellpose4Framework().getPatternStyleId());
        assertEquals("TrackMate", new TrackMateFramework().getName());
        assertEquals("constellation", new TrackMateFramework().getPatternStyleId());
        assertEquals("Comet", new CometFramework().getName());
        assertEquals("constellation", new CometFramework().getPatternStyleId());
        assertEquals("JIPipeDL", new JIPipeDLFramework().getName());
        assertEquals("shape-grid", new JIPipeDLFramework().getPatternStyleId());
        assertEquals("JFreeChart", new JFreeChartFramework().getName());
        assertEquals("shape-grid", new JFreeChartFramework().getPatternStyleId());
        assertEquals("OpenCV", new OpenCVFramework().getName());
        assertEquals("weave", new OpenCVFramework().getPatternStyleId());
        assertEquals("CLIJ", new ClijFramework().getName());
        assertEquals("weave", new ClijFramework().getPatternStyleId());
        assertEquals("ImageJ 1", new ImageJ1Framework().getName());
        assertEquals("triangles", new ImageJ1Framework().getPatternStyleId());
        assertEquals("ImageJ 2", new ImageJ2Framework().getName());
        assertEquals("triangles", new ImageJ2Framework().getPatternStyleId());
    }

    @Test
    void abbreviationsAreShortAndUnique() {
        Set<String> abbreviations = new HashSet<>();
        for (Class<? extends JIPipeNodeTaxonomyFramework> framework : allFrameworks()) {
            String abbreviation = instantiate(framework).getAbbreviation();
            assertTrue(abbreviation != null && !abbreviation.isEmpty(),
                "Expected a non-empty abbreviation for " + framework.getName());
            assertTrue(abbreviation.length() <= 4,
                "Abbreviation of " + framework.getName() + " must be at most 4 characters: " + abbreviation);
            abbreviations.add(abbreviation);
        }
        assertEquals(14, abbreviations.size(),
            "Expected 14 unique abbreviations, got " + abbreviations.size() + " for: " + abbreviations);
    }

    @Test
    void abbreviationsMatchTheSeedTable() {
        assertEquals("CORE", new CoreFramework().getAbbreviation());
        assertEquals("CP2", new Cellpose2Framework().getAbbreviation());
        assertEquals("CP3", new Cellpose3Framework().getAbbreviation());
        assertEquals("CP4", new Cellpose4Framework().getAbbreviation());
        assertEquals("TM", new TrackMateFramework().getAbbreviation());
        assertEquals("CMET", new CometFramework().getAbbreviation());
        assertEquals("JDL", new JIPipeDLFramework().getAbbreviation());
        assertEquals("JFC", new JFreeChartFramework().getAbbreviation());
        assertEquals("OCV", new OpenCVFramework().getAbbreviation());
        assertEquals("CLIJ", new ClijFramework().getAbbreviation());
        assertEquals("IJ1", new ImageJ1Framework().getAbbreviation());
        assertEquals("IJ2", new ImageJ2Framework().getAbbreviation());
    }

    @Test
    void patternStyleIdsAreNonEmpty() {
        for (Class<? extends JIPipeNodeTaxonomyFramework> framework : allFrameworks()) {
            String styleId = instantiate(framework).getPatternStyleId();
            assertTrue(styleId != null && !styleId.isEmpty(),
                "Expected a non-empty pattern style id for " + framework.getName() + ", got: " + styleId);
        }
    }

    @Test
    void registryValidateReturnsEmptyForFullSet() {
        FrameworkRegistry registry = new FrameworkRegistry();
        for (Class<? extends JIPipeNodeTaxonomyFramework> framework : allFrameworks()) {
            registry.register(instantiate(framework));
        }
        assertEquals(14, registry.all().size());
        Set<String> errors = registry.validate();
        assertTrue(errors.isEmpty(), "Expected no validation errors but got: " + errors);
    }

    @Test
    void colorsAreStablePerId() {
        for (Class<? extends JIPipeNodeTaxonomyFramework> framework : allFrameworks()) {
            Color first = instantiate(framework).getColor();
            Color second = instantiate(framework).getColor();
            assertEquals(first, second,
                "getColor() of " + framework.getName() + " must be stable across instances");
        }
    }

    @Test
    void differentFrameworksMapToDifferentColors() {
        Set<Color> colors = allFrameworks().stream()
            .map(f -> instantiate(f).getColor())
            .collect(Collectors.toSet());
        assertTrue(colors.size() > 1,
            "Expected the 14 seed frameworks to span more than one palette color, got: " + colors);
    }
}
