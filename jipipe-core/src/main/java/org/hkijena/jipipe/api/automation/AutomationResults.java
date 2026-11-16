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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;

/**
 * Result envelope helpers for operations that return images or file paths.
 * <p>
 * The field names are part of the automation contract: the MCP broker maps results with
 * non-null {@code image-base64} and {@code mime} fields to MCP image content. The
 * {@link #choose(BufferedImage, String)} gate enforces the size contract: a save-to result
 * carries only the path, never the inline image.
 */
public final class AutomationResults {

    private static final ObjectMapper mapper = new ObjectMapper();

    private AutomationResults() {
    }

    /**
     * Returns the inline image envelope for an image at its final size: PNG-encoded and
     * base64-encoded, with its mime type. Keys: {@code image-base64}, {@code mime}.
     */
    public static ObjectNode image(BufferedImage image) {
        Objects.requireNonNull(image, "Image must not be null");
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", stream);
        } catch (IOException e) {
            // Writing to a byte array cannot fail in practice; PNG support is mandatory in ImageIO
            throw new UncheckedIOException("Could not encode PNG image", e);
        }
        ObjectNode node = mapper.createObjectNode();
        node.put("image-base64", Base64.getEncoder().encodeToString(stream.toByteArray()));
        node.put("mime", "image/png");
        return node;
    }

    /**
     * Returns the inline image envelope for an image scaled up by the integer factor first.
     */
    public static ObjectNode image(BufferedImage image, int scale) {
        return image(scaled(image, scale));
    }

    /**
     * Returns the file path envelope. Key: {@code saved-to} (absolute, normalized).
     */
    public static ObjectNode savedTo(Path path) {
        Objects.requireNonNull(path, "Path must not be null");
        ObjectNode node = mapper.createObjectNode();
        node.put("saved-to", path.toAbsolutePath().normalize().toString());
        return node;
    }

    /**
     * The single gate for operations that render an image with an optional save-to target:
     * a non-null, non-blank {@code saveTo} returns the {@link #savedTo(Path)} envelope only
     * (no inline image - the caller is responsible for having written the file), otherwise
     * the image is inlined at its own size. This keeps large images out of the result JSON
     * when they are already persisted.
     */
    public static ObjectNode choose(BufferedImage image, String saveTo) {
        if (saveTo != null && !saveTo.isBlank()) {
            return savedTo(Path.of(saveTo.trim()));
        }
        return image(image);
    }

    /**
     * Returns the image scaled up by the integer factor (drawn with antialiasing and bilinear
     * interpolation). Factors below 1 return the image unchanged.
     */
    public static BufferedImage scaled(BufferedImage image, int factor) {
        Objects.requireNonNull(image, "Image must not be null");
        int effectiveFactor = Math.max(1, factor);
        if (effectiveFactor == 1) {
            return image;
        }
        BufferedImage scaled = new BufferedImage(image.getWidth() * effectiveFactor,
                image.getHeight() * effectiveFactor, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }
}
