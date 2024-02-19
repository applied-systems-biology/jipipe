package org.hkijena.jipipe.extensions.imagejdatatypes.util;

import ij.process.ColorProcessor;
import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;

@AddJIPipeDocumentationDescription(description = "See https://en.wikipedia.org/wiki/Blend_modes")
public enum ImageBlendMode {
    Normal("Normal"),
    Multiply("Multiply"),
    DivideBA("Divide (top / bottom)"),
    DivideAB("Divide (bottom / top)"),
    Add("Add"),
    Minimum("Minimum (darken)"),
    Maximum("Maximum (lighten)"),
    SubtractAB("Subtract (bottom - top)"),
    SubtractBA("Subtract (top - bottom)"),
    Difference("Difference"),
    Overlay("Overlay"),
    Screen("Screen");

    private final String label;

    ImageBlendMode(String label) {

        this.label = label;
    }

    /**
     * Blends the top image onto the bottom image
     *
     * @param bottom  the bottom (target) image. will be changed
     * @param top     the top image (will be unchanged)
     * @param opacity the opacity of the top layer
     */
    public void blend(ColorProcessor bottom, ColorProcessor top, double opacity) {
        opacity = Math.max(0, Math.min(1, opacity));
        int[] inputPixels = (int[]) top.getPixels();
        int[] outputPixels = (int[]) bottom.getPixels();

        for (int i = 0; i < outputPixels.length; i++) {
            outputPixels[i] = blend(outputPixels[i], inputPixels[i], opacity);
        }
    }

    /**
     * Applies blending on a single RGB pixel
     *
     * @param aRGB    the first pixel
     * @param bRGB    the second pixel
     * @param opacity the opacity
     * @return the blended value
     */
    public int blend(int aRGB, int bRGB, double opacity) {
        final int ar, ag, ab, br, bg, bb;
        ar = (aRGB & 0xff0000) >> 16;
        ag = (aRGB & 0xff00) >> 8;
        ab = aRGB & 0xff;
        br = (bRGB & 0xff0000) >> 16;
        bg = (bRGB & 0xff00) >> 8;
        bb = bRGB & 0xff;
        final int or, og, ob;

        // Mixing
        switch (this) {
            case Normal: {
                or = br;
                og = bg;
                ob = bb;
            }
            break;
            case Overlay: {
                if (ar < 127)
                    or = Math.min(255, 2 * ar * br);
                else
                    or = (int) Math.min(255, (255.0 * (1.0 - 2.0 * (1.0 - (ar / 255.0)) * (1.0 - (br / 255.0)))));
                if (ag < 127)
                    og = Math.min(255, 2 * ag * bg);
                else
                    og = (int) Math.min(255, (255.0 * (1.0 - 2.0 * (1.0 - (ag / 255.0)) * (1.0 - (bg / 255.0)))));
                if (ab < 127)
                    ob = Math.min(255, 2 * ab * bb);
                else
                    ob = (int) Math.min(255, (255.0 * (1.0 - 2.0 * (1.0 - (ab / 255.0)) * (1.0 - (bb / 255.0)))));
            }
            break;
            case Screen: {
                or = (int) (255.0 * (1.0 - (1.0 - ar / 255.0) * (1.0 - br / 255.0)));
                og = (int) (255.0 * (1.0 - (1.0 - ag / 255.0) * (1.0 - bg / 255.0)));
                ob = (int) (255.0 * (1.0 - (1.0 - ab / 255.0) * (1.0 - bb / 255.0)));
            }
            break;
            case Difference: {
                or = Math.abs(br - ar);
                og = Math.abs(bg - ag);
                ob = Math.abs(bb - ab);
            }
            break;
            case Minimum: {
                or = Math.min(br, ar);
                og = Math.min(bg, ag);
                ob = Math.min(bb, ab);
            }
            break;
            case Maximum: {
                or = Math.max(br, ar);
                og = Math.max(bg, ag);
                ob = Math.max(bb, ab);
            }
            break;
            case Multiply: {
                or = (int) (((br / 255.0) * (ar / 255.0)) * 255);
                og = (int) (((bg / 255.0) * (ag / 255.0)) * 255);
                ob = (int) (((bb / 255.0) * (ab / 255.0)) * 255);
            }
            break;
            case Add: {
                or = Math.min(255, ar + br);
                og = Math.min(255, ag + bg);
                ob = Math.min(255, ab + bb);
            }
            break;
            case SubtractAB: {
                or = Math.max(0, ar - br);
                og = Math.max(0, ag - bg);
                ob = Math.max(0, ab - bb);
            }
            break;
            case SubtractBA: {
                or = Math.max(0, br - ar);
                og = Math.max(0, bg - ag);
                ob = Math.max(0, bb - ab);
            }
            break;
            case DivideBA: {
                if (ar > 0)
                    or = (int) (((br / 255.0) / (ar / 255.0)) * 255);
                else
                    or = 255;
                if (ag > 0)
                    og = (int) (((bg / 255.0) / (ag / 255.0)) * 255);
                else
                    og = 255;
                if (ab > 0)
                    ob = (int) (((bb / 255.0) / (ab / 255.0)) * 255);
                else
                    ob = 255;
            }
            break;
            case DivideAB: {
                if (br > 0)
                    or = (int) (((ar / 255.0) / (br / 255.0)) * 255);
                else
                    or = 255;
                if (bg > 0)
                    og = (int) (((ag / 255.0) / (bg / 255.0)) * 255);
                else
                    og = 255;
                if (bb > 0)
                    ob = (int) (((ab / 255.0) / (bb / 255.0)) * 255);
                else
                    ob = 255;
            }
            break;
            default:
                throw new UnsupportedOperationException("Unsupported blend mode " + this);
        }

        // Alpha blending
        int r = Math.min(255, Math.max((int) (opacity * or + (1.0 - opacity) * ar), 0));
        int g = Math.min(255, Math.max((int) (opacity * og + (1.0 - opacity) * ag), 0));
        int b = Math.min(255, Math.max((int) (opacity * ob + (1.0 - opacity) * ab), 0));

        return b + (g << 8) + (r << 16);
    }


    @Override
    public String toString() {
        return label;
    }
}
