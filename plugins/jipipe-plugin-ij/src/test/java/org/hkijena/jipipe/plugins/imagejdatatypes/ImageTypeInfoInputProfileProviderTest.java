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

package org.hkijena.jipipe.plugins.imagejdatatypes;

import org.hkijena.jipipe.api.nodes.taxonomy.JIPipeNodeTaxonomyInputProfile;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImageTypeInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure unit test for {@link ImageTypeInfoInputProfileProvider}: the provider is
 * constructed directly (no {@code JIPipe.ensureInstance()}) and exercised over
 * {@link ImageTypeInfo}-annotated stub classes. It verifies the mapping of
 * {@code numDimensions()}: 2 → {@code PLANE_2D}, 3 → {@code VOLUME_3D},
 * −1 → {@code ANY}, and unannotated types → {@code NA}.
 * <p>
 * The stubs are plain classes (no ImageJ / {@code ij.*} hierarchy), so loading
 * them must not trip the ImageJ legacy patcher ordering.
 */
class ImageTypeInfoInputProfileProviderTest {

    /** 2D image type. */
    @ImageTypeInfo(numDimensions = 2)
    private static final class TwoDimStub {
    }

    /** 3D volume type. */
    @ImageTypeInfo(numDimensions = 3)
    private static final class ThreeDimStub {
    }

    /** Image type with no dimension constraint (default −1). */
    @ImageTypeInfo
    private static final class AnyDimStub {
    }

    /** Non-image type: no {@link ImageTypeInfo} annotation. */
    private static final class NonImageStub {
    }

    @Test
    void providerMapsNumDimensions() {
        ImageTypeInfoInputProfileProvider provider = new ImageTypeInfoInputProfileProvider();

        assertEquals(JIPipeNodeTaxonomyInputProfile.PLANE_2D, provider.getProfile(TwoDimStub.class));
        assertEquals(JIPipeNodeTaxonomyInputProfile.VOLUME_3D, provider.getProfile(ThreeDimStub.class));
        assertEquals(JIPipeNodeTaxonomyInputProfile.ANY, provider.getProfile(AnyDimStub.class));
        assertEquals(JIPipeNodeTaxonomyInputProfile.NA, provider.getProfile(NonImageStub.class));
    }
}
