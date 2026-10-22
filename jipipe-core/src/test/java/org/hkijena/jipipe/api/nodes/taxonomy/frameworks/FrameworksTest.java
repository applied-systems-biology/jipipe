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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the 20-framework core ship set of {@link JIPipeNodeTaxonomyFramework}s:
 * the exact stable literal ids, display names, abbreviations (at most 4
 * characters, unique) and the node pattern style ids rendered on canvas
 * nodes, a clean {@link FrameworkRegistry#validate()} for the full set, and
 * the stable default color mapping per id. This is the single-source set of
 * core frameworks, so the exact counts are pinned (sanctioned exception per
 * the AGENTS.md plugin-aware test ruling).
 */
class FrameworksTest {

    /**
     * The 20 core ship-set framework class literals.
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
            JIPipeDLFramework.class,
            JFreeChartFramework.class,
            OpenCVFramework.class,
            OmniposeFramework.class,
            IlastikFramework.class,
            ClijFramework.class,
            ImageJ1Framework.class,
            ImageJ2Framework.class,
            WekaFramework.class,
            FilamentsFramework.class,
            IJ3DFramework.class,
            OmeroFramework.class,
            Scene3DFramework.class
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
        assertEquals(20, ids.size(), "Expected 20 unique framework ids, got " + ids.size() + " for: " + ids);
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
        assertEquals("jipipe:framework:jipipedl", new JIPipeDLFramework().getId());
        assertEquals("jipipe:framework:jfreechart", new JFreeChartFramework().getId());
        assertEquals("jipipe:framework:opencv", new OpenCVFramework().getId());
        assertEquals("jipipe:framework:clij", new ClijFramework().getId());
        assertEquals("jipipe:framework:imagej1", new ImageJ1Framework().getId());
        assertEquals("jipipe:framework:imagej2", new ImageJ2Framework().getId());
        assertEquals("jipipe:framework:weka", new WekaFramework().getId());
        assertEquals("jipipe:framework:filaments", new FilamentsFramework().getId());
        assertEquals("jipipe:framework:ij3d", new IJ3DFramework().getId());
        assertEquals("jipipe:framework:omero", new OmeroFramework().getId());
        assertEquals("jipipe:framework:scene-3d", new Scene3DFramework().getId());
    }

    @Test
    void namesAndPatternStyleIdsMatchTheSeedTable() {
        assertEquals("Core", new CoreFramework().getName());
        assertEquals("patchwork", new CoreFramework().getPatternStyleId());
        assertEquals("Cellpose 2", new Cellpose2Framework().getName());
        assertEquals("blobs", new Cellpose2Framework().getPatternStyleId());
        assertEquals("Cellpose 3", new Cellpose3Framework().getName());
        assertEquals("blobs", new Cellpose3Framework().getPatternStyleId());
        assertEquals("Cellpose 4", new Cellpose4Framework().getName());
        assertEquals("blobs", new Cellpose4Framework().getPatternStyleId());
        assertEquals("TrackMate", new TrackMateFramework().getName());
        assertEquals("net", new TrackMateFramework().getPatternStyleId());
        assertEquals("JIPipeDL", new JIPipeDLFramework().getName());
        assertEquals("carpet", new JIPipeDLFramework().getPatternStyleId());
        assertEquals("JFreeChart", new JFreeChartFramework().getName());
        assertEquals("waves", new JFreeChartFramework().getPatternStyleId());
        assertEquals("OpenCV", new OpenCVFramework().getName());
        assertEquals("weave", new OpenCVFramework().getPatternStyleId());
        assertEquals("CLIJ", new ClijFramework().getName());
        assertEquals("honeycomb", new ClijFramework().getPatternStyleId());
        assertEquals("ImageJ 1", new ImageJ1Framework().getName());
        assertEquals("triangles", new ImageJ1Framework().getPatternStyleId());
        assertEquals("ImageJ 2", new ImageJ2Framework().getName());
        assertEquals("triangles", new ImageJ2Framework().getPatternStyleId());
        assertEquals("ilastik", new IlastikFramework().getName());
        assertEquals("dots", new IlastikFramework().getPatternStyleId());
        assertEquals("Omnipose", new OmniposeFramework().getName());
        assertEquals("cellular", new OmniposeFramework().getPatternStyleId());
        assertEquals("Python", new PythonFramework().getName());
        assertEquals("stripes", new PythonFramework().getPatternStyleId());
        assertEquals("R", new RFramework().getName());
        assertEquals("landscape", new RFramework().getPatternStyleId());
        assertEquals("Weka", new WekaFramework().getName());
        assertEquals("cellular", new WekaFramework().getPatternStyleId());
        assertEquals("Filaments", new FilamentsFramework().getName());
        assertEquals("flow", new FilamentsFramework().getPatternStyleId());
        assertEquals("IJ3D", new IJ3DFramework().getName());
        assertEquals("hilbert", new IJ3DFramework().getPatternStyleId());
        assertEquals("OMERO", new OmeroFramework().getName());
        assertEquals("stripes", new OmeroFramework().getPatternStyleId());
        assertEquals("Scene 3D", new Scene3DFramework().getName());
        assertEquals("landscape", new Scene3DFramework().getPatternStyleId());
    }

    @Test
    void noFrameworkHasNoPatternStyle() {
        assertNull(new NoFramework().getPatternStyleId(), "NoFramework must carry no pattern");
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
        assertEquals(20, abbreviations.size(),
            "Expected 20 unique abbreviations, got " + abbreviations.size() + " for: " + abbreviations);
    }

    @Test
    void abbreviationsMatchTheSeedTable() {
        assertEquals("CORE", new CoreFramework().getAbbreviation());
        assertEquals("CP2", new Cellpose2Framework().getAbbreviation());
        assertEquals("CP3", new Cellpose3Framework().getAbbreviation());
        assertEquals("CP4", new Cellpose4Framework().getAbbreviation());
        assertEquals("TM", new TrackMateFramework().getAbbreviation());
        assertEquals("JDL", new JIPipeDLFramework().getAbbreviation());
        assertEquals("JFC", new JFreeChartFramework().getAbbreviation());
        assertEquals("OCV", new OpenCVFramework().getAbbreviation());
        assertEquals("CLIJ", new ClijFramework().getAbbreviation());
        assertEquals("IJ1", new ImageJ1Framework().getAbbreviation());
        assertEquals("IJ2", new ImageJ2Framework().getAbbreviation());
        assertEquals("WEKA", new WekaFramework().getAbbreviation());
        assertEquals("FILA", new FilamentsFramework().getAbbreviation());
        assertEquals("3D", new IJ3DFramework().getAbbreviation());
        assertEquals("OMRO", new OmeroFramework().getAbbreviation());
        assertEquals("S3D", new Scene3DFramework().getAbbreviation());
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
        assertEquals(20, registry.all().size());
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
            "Expected the 20 ship-set frameworks to span more than one palette color, got: " + colors);
    }
}
