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

package org.hkijena.jipipe.api.automation;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the result envelope helpers: the inline image envelope and the save-to size contract
 * (a save-to result must never inline the image).
 */
public class AutomationResultsTest {

    private static BufferedImage sampleImage() {
        return new BufferedImage(8, 6, BufferedImage.TYPE_INT_ARGB);
    }

    @Test
    void chooseWithoutSaveToReturnsInlineImage() {
        ObjectNode result = AutomationResults.choose(sampleImage(), null);
        assertTrue(result.has("image-base64"), "inline result carries the image");
        assertTrue(result.has("mime"), "inline result carries the mime type");
        assertEquals("image/png", result.get("mime").asText());
        assertFalse(result.has("saved-to"), "inline result must not carry a path");
    }

    @Test
    void chooseWithBlankSaveToBehavesLikeNoSaveTo() {
        ObjectNode result = AutomationResults.choose(sampleImage(), "   ");
        assertTrue(result.has("image-base64"));
        assertFalse(result.has("saved-to"));
    }

    @Test
    void chooseWithSaveToReturnsOnlyThePath() {
        ObjectNode result = AutomationResults.choose(sampleImage(), "/x/y.png");
        assertTrue(result.has("saved-to"));
        assertEquals("/x/y.png", result.get("saved-to").asText());
        assertFalse(result.has("image-base64"), "save-to results never inline the image (size contract)");
        assertFalse(result.has("mime"), "save-to results never inline the image (size contract)");
    }

    @Test
    void imageDecodesToPngAtScaledDimensions() throws IOException {
        ObjectNode result = AutomationResults.image(sampleImage(), 2);
        byte[] decoded = Base64.getDecoder().decode(result.get("image-base64").asText());
        assertEquals(0x89, decoded[0] & 0xFF, "PNG magic byte");
        assertEquals('P', decoded[1], "PNG magic byte");
        assertEquals('N', decoded[2], "PNG magic byte");
        assertEquals('G', decoded[3], "PNG magic byte");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(decoded));
        assertEquals(16, image.getWidth(), "scale 2 doubles the width");
        assertEquals(12, image.getHeight(), "scale 2 doubles the height");
    }

    @Test
    void imageAtScaleOneKeepsDimensions() {
        ObjectNode result = assertDoesNotThrow(() -> AutomationResults.image(sampleImage(), 1));
        byte[] decoded = Base64.getDecoder().decode(result.get("image-base64").asText());
        BufferedImage image = assertDoesNotThrow(() -> ImageIO.read(new ByteArrayInputStream(decoded)));
        assertEquals(8, image.getWidth());
        assertEquals(6, image.getHeight());
    }
}
