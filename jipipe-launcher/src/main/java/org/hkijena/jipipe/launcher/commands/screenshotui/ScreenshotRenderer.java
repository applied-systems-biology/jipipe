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

package org.hkijena.jipipe.launcher.commands.screenshotui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Offscreen Swing → PNG capture used by the {@code screenshot-ui} launcher
 * command (VLM-driven before/after verification for the #1339 parameter UX
 * overhaul).
 * <p>
 * A component that has never been shown does not lay out or paint reliably via
 * {@code printAll} alone (and a {@code JScrollPane} reports a clipped preferred
 * height, not its content's). So the component is realized in an off-screen
 * {@link JFrame} (placed far off the visible desktop so it never flashes),
 * laid out at a fixed width, and then captured at its full realized size.
 * Must be called on the EDT.
 */
public final class ScreenshotRenderer {

    /** Far-off position so the realization frame is never visible on screen. */
    private static final int OFFSCREEN_X = -4000;
    private static final int OFFSCREEN_Y = -4000;

    private ScreenshotRenderer() {
    }

    /**
     * Lays out the component at the given width inside an off-screen frame,
     * then captures it at its full realized size into a PNG.
     *
     * @param c       the component to capture (already constructed)
     * @param width   target width in pixels
     * @param outPng  destination file (parent directory must exist)
     */
    public static void renderComponent(Component c, int width, Path outPng) throws IOException {
        renderFixed(c, width, -1, outPng);
    }
    /**
     * Captures the component at a fixed width, and (if {@code height > 0}) a
     * fixed height; otherwise at the component's full realized height.
     * Used for both panels (auto height) and dialogs (fixed height).
     */
    public static void renderFixed(Component c, int width, int height, Path outPng) throws IOException {
        JFrame frame = new JFrame("screenshot");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        ((JComponent) frame.getContentPane()).setBorder(BorderFactory.createEmptyBorder());
        frame.getContentPane().add(c, BorderLayout.CENTER);
        c.setPreferredSize(new Dimension(width, height > 0 ? height : Math.max(c.getPreferredSize().height, 200)));
        if (height > 0) {
            frame.setSize(width, height);
        } else {
            frame.pack();
        }
        frame.setLocation(OFFSCREEN_X, OFFSCREEN_Y);
        frame.setVisible(true);
        frame.validate();

        try {
            int capW = c.getWidth() > 0 ? c.getWidth() : width;
            int capH = c.getHeight() > 0 ? c.getHeight() : 300;

            BufferedImage img = new BufferedImage(capW, capH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g.setColor(new Color(0, 0, 0, 0));
            g.fillRect(0, 0, capW, capH);
            c.printAll(g);
            g.dispose();
            ImageIO.write(img, "png", outPng.toFile());
        } finally {
            frame.setVisible(false);
            frame.dispose();
        }
    }
}
