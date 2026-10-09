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

package org.hkijena.jipipe.api;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFunction;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyFramework;
import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomySpatialDimension;
import org.hkijena.jipipe.api.nodes.taxonomy.NoFramework;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies the additive taxonomy fields added to {@link ConfigureJIPipeNode},
 * the {@link JIPipeNodeTaxonomyTags} repeatable annotation, and that the legacy
 * fields ({@code menuPath}, {@code nodeTypeCategory}) still exist and compile
 * with their original defaults (this task must not break the tree).
 */
class ConfigureJIPipeNodeAnnotationTest {

    /** Minimal stub framework used in the annotated test node. */
    private static class StubFramework implements JIPipeNodeTaxonomyFramework {
        @Override
        public String getId() {
            return "stub:framework";
        }

        @Override
        public String getName() {
            return "Stub Framework";
        }

        @Override
        public String getAbbreviation() {
            return "SF";
        }

        @Override
        public String getPatternStyleId() {
            return "initials";
        }
    }

    /** Minimal stub taxonomy function used in the annotated test node. */
    private static class StubFunction implements JIPipeNodeTaxonomyFunction {
        @Override
        public String getId() {
            return "stub:function";
        }

        @Override
        public String getName() {
            return "Stub Function";
        }

        @Override
        public JIPipeNodeTaxonomyFunction getParent() {
            return null;
        }

        @Override
        public Float getColorHue() {
            return null;
        }

        @Override
        public String getDescription() {
            return "stub function description";
        }

        @Override
        public Icon getIcon() {
            return null;
        }

        @Override
        public Set<String> getTags() {
            return Set.of();
        }

        @Override
        public boolean isVisibleInPipeline() {
            return true;
        }

        @Override
        public boolean isVisibleInCompartments() {
            return true;
        }
    }

    /**
     * A node stub annotated with the new taxonomy fields plus two repeated
     * {@link JIPipeNodeTaxonomyTags} annotations and the legacy fields.
     */
    @ConfigureJIPipeNode(
        function = {StubFunction.class},
        spatialDimensions = {JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D, JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D},
        framework = StubFramework.class
    )
    @JIPipeNodeTaxonomyTags({"a", "b"})
    @JIPipeNodeTaxonomyTags({"c"})
    private static class AnnotatedNode {
    }

    /** A node stub that uses only the legacy {@code menuPath} field. */
    @ConfigureJIPipeNode(menuPath = "Legacy / Menu")
    private static class LegacyNode {
    }

    @Test
    void functionFirstEntryIsPrimary() {
        ConfigureJIPipeNode annotation = AnnotatedNode.class.getAnnotation(ConfigureJIPipeNode.class);
        assertNotNull(annotation);
        assertEquals(StubFunction.class, annotation.function()[0]);
    }

    @Test
    void spatialDimensionsAreRead() {
        ConfigureJIPipeNode annotation = AnnotatedNode.class.getAnnotation(ConfigureJIPipeNode.class);
        assertNotNull(annotation);
        assertArrayEquals(
            new JIPipeNodeTaxonomySpatialDimension[]{
                JIPipeNodeTaxonomySpatialDimension.SPATIAL_2D,
                JIPipeNodeTaxonomySpatialDimension.SPATIAL_3D
            },
            annotation.spatialDimensions()
        );
    }

    @Test
    void frameworkIsRead() {
        ConfigureJIPipeNode annotation = AnnotatedNode.class.getAnnotation(ConfigureJIPipeNode.class);
        assertNotNull(annotation);
        assertEquals(StubFramework.class, annotation.framework());
    }

    @Test
    void frameworkDefaultsToNoFramework() {
        // A node with no explicit framework must default to NoFramework.class.
        ConfigureJIPipeNode annotation = NoFrameworkOnly.class.getAnnotation(ConfigureJIPipeNode.class);
        assertNotNull(annotation);
        assertEquals(NoFramework.class, annotation.framework());
    }

    /** A node stub that does not set any taxonomy field. */
    @ConfigureJIPipeNode
    private static class NoFrameworkOnly {
    }

    @Test
    void repeatedTagsAreConcatenated() {
        List<JIPipeNodeTaxonomyTags> tags =
            Arrays.asList(AnnotatedNode.class.getAnnotationsByType(JIPipeNodeTaxonomyTags.class));
        assertEquals(2, tags.size());
        assertArrayEquals(new String[]{"a", "b"}, tags.get(0).value());
        assertArrayEquals(new String[]{"c"}, tags.get(1).value());

        // All values, concatenated, in application order.
        List<String> all = tags.stream()
            .flatMap(t -> Arrays.stream(t.value()))
            .toList();
        assertEquals(List.of("a", "b", "c"), all);
    }

    @Test
    void functionAndSpatialDefaultToEmpty() {
        ConfigureJIPipeNode annotation = NoFrameworkOnly.class.getAnnotation(ConfigureJIPipeNode.class);
        assertNotNull(annotation);
        assertEquals(0, annotation.function().length);
        assertEquals(0, annotation.spatialDimensions().length);
    }

    @SuppressWarnings("deprecation")
    @Test
    void legacyFieldsStillExistAndKeepDefaults() {
        // These calls only compile if the legacy fields are still present.
        ConfigureJIPipeNode annotation = AnnotatedNode.class.getAnnotation(ConfigureJIPipeNode.class);
        assertNotNull(annotation);
        // menuPath defaults to "" when not set.
        assertEquals("", annotation.menuPath());
        // nodeTypeCategory defaults to InternalNodeTypeCategory when not set.
        assertNotNull(annotation.nodeTypeCategory());

        // A node that sets menuPath still reads it back.
        ConfigureJIPipeNode legacy = LegacyNode.class.getAnnotation(ConfigureJIPipeNode.class);
        assertNotNull(legacy);
        assertEquals("Legacy / Menu", legacy.menuPath());
    }

    @Test
    void legacyFieldsAreDeprecated() {
        // The deprecation markers are present on both legacy fields.
        try {
            assertNotNull(ConfigureJIPipeNode.class.getDeclaredMethod("menuPath")
                .getAnnotation(Deprecated.class));
            assertNotNull(ConfigureJIPipeNode.class.getDeclaredMethod("nodeTypeCategory")
                .getAnnotation(Deprecated.class));
        } catch (NoSuchMethodException e) {
            throw new AssertionError("legacy method not found: " + e.getMessage(), e);
        }
    }

}
