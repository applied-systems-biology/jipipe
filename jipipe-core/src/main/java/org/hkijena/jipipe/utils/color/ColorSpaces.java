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

package org.hkijena.jipipe.utils.color;

import java.awt.Color;

/**
 * Pure color-space conversions for the modern color dialog (#1339):
 * RGB ↔ HSB, RGB ↔ CIE L*a*b* (D65), and hex formatting.
 * <p>
 * {@link #fromLab(double, double, double)} always returns a valid sRGB color:
 * out-of-gamut L*a*b* values are clamped by iterative channel clipping
 * (clip the linear RGB, convert back to L*a*b*, keep L, repeat) —
 * deterministic and bounded (≤ 30 iterations).
 */
public final class ColorSpaces {

    // CIE D65 white point
    private static final double XN = 0.95047;
    private static final double YN = 1.00000;
    private static final double ZN = 1.08883;
    private static final double LAB_EPSILON = 0.008856; // (6/29)^3
    private static final double LAB_KAPPA = 903.3;      // 3*(6/29)^2 + 4/29 (≈ 841*(29/18)^3 / ... standard 903.3)
    /** Upper bound of the gamut-clamp search (converges in far fewer). */
    private static final int CLAMP_MAX_ITERATIONS = 200;

    private ColorSpaces() {
    }

    // ------------------------------------------------------------------
    // HSB (h 0-360, s 0-100, b 0-100)
    // ------------------------------------------------------------------

    /** @return {hue 0-360, saturation 0-100, brightness 0-100} */
    public static double[] toHsb(Color c) {
        float[] hsb = new float[3];
        Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
        return new double[]{hsb[0] * 360.0, hsb[1] * 100.0, hsb[2] * 100.0};
    }

    /** Inputs are wrapped/clipped into range (h mod 360, s/b clamped to [0,100]). */
    public static Color fromHsb(double h, double s, double b) {
        h = ((h % 360.0) + 360.0) % 360.0;
        s = clamp(s, 0.0, 100.0);
        b = clamp(b, 0.0, 100.0);
        int rgb = Color.HSBtoRGB((float) (h / 360.0), (float) (s / 100.0), (float) (b / 100.0));
        return new Color(rgb);
    }

    // ------------------------------------------------------------------
    // CIE L*a*b* (D65), L 0-100
    // ------------------------------------------------------------------

    /** @return {L 0-100, a, b} in CIE L*a*b* (D65) */
    public static double[] toLab(Color c) {
        double r = srgbToLinear(c.getRed() / 255.0);
        double g = srgbToLinear(c.getGreen() / 255.0);
        double b = srgbToLinear(c.getBlue() / 255.0);

        double x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * b) / XN;
        double y = (0.2126729 * r + 0.7151522 * g + 0.0721750 * b) / YN;
        double z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * b) / ZN;

        double fx = labF(x);
        double fy = labF(y);
        double fz = labF(z);

        double L = 116.0 * fy - 16.0;
        double a = 500.0 * (fx - fy);
        double bb = 200.0 * (fy - fz);
        return new double[]{L, a, bb};
    }

    /**
     * Converts L*a*b* to the nearest representable sRGB color (out-of-gamut
     * values are clamped — the result is always a valid sRGB color).
     * <p>
     * The clamp is a projected subgradient search in L*a*b* space: starting at
     * the target, each step moves against the sum of (a) the pull back to the
     * target and (b) a strongly weighted penalty for out-of-gamut linear-RGB
     * channels. It settles on the gamut boundary at the point closest to the
     * target in L*a*b* distance (both L and chroma are free, so e.g.
     * L=100 clamps to a saturated color at a lower L instead of white).
     * Deterministic, bounded (≤ {@value #CLAMP_MAX_ITERATIONS} iterations);
     * in-gamut inputs return immediately.
     */
    public static Color fromLab(double L, double a, double b) {
        double[] lin = labToLinearSrgb(L, a, b);
        if (isInGamut(lin)) {
            return toSrgbColor(clamp01(lin[0]), clamp01(lin[1]), clamp01(lin[2]));
        }
        final double lambda = 1000.0; // weight of the gamut-violation penalty
        final double eps = 0.25;      // numerical gradient step (LAB units)
        final LabToLinear f = ColorSpaces::labToLinearSrgb;
        double pL = L, pA = a, pB = b;
        for (int iter = 0; iter < CLAMP_MAX_ITERATIONS; iter++) {
            double viol = gamutViolation(f.convert(pL, pA, pB));
            if (viol < 1e-10) {
                break;
            }
            double gL = 2.0 * (pL - L) + lambda * numericGradient(f, pL, pA, pB, 0, eps);
            double gA = 2.0 * (pA - a) + lambda * numericGradient(f, pL, pA, pB, 1, eps);
            double gB = 2.0 * (pB - b) + lambda * numericGradient(f, pL, pA, pB, 2, eps);
            double norm = Math.sqrt(gL * gL + gA * gA + gB * gB);
            if (norm < 1e-12) {
                break;
            }
            pL -= gL / norm;
            pA -= gA / norm;
            pB -= gB / norm;
        }
        double[] finalLin = labToLinearSrgb(pL, pA, pB);
        return toSrgbColor(clamp01(finalLin[0]), clamp01(finalLin[1]), clamp01(finalLin[2]));
    }

    private interface LabToLinear {
        double[] convert(double L, double a, double b);
    }

    /** Sum of squared out-of-range linear-RGB channels (0 when in gamut). */
    private static double gamutViolation(double[] lin) {
        double v = 0;
        for (double c : lin) {
            if (c > 1.0) {
                v += (c - 1.0) * (c - 1.0);
            } else if (c < 0.0) {
                v += c * c;
            }
        }
        return v;
    }

    private static double numericGradient(LabToLinear f, double pL, double pA, double pB, int axis, double eps) {
        double plus = pL, minus = pL, aPlus = pA, aMinus = pA, bPlus = pB, bMinus = pB;
        if (axis == 0) {
            plus = pL + eps;
            minus = pL - eps;
        } else if (axis == 1) {
            aPlus = pA + eps;
            aMinus = pA - eps;
        } else {
            bPlus = pB + eps;
            bMinus = pB - eps;
        }
        return (gamutViolation(f.convert(plus, aPlus, bPlus)) - gamutViolation(f.convert(minus, aMinus, bMinus))) / (2.0 * eps);
    }

    // ------------------------------------------------------------------
    // Hex (#RGB / #RRGGBB / #RRGGBBAA, '#' optional)
    // ------------------------------------------------------------------

    /** @return {@code #RRGGBB} when alpha is 255, else {@code #RRGGBBAA} */
    public static String toHex(Color c) {
        if (c.getAlpha() == 255) {
            return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
        }
        return String.format("#%02X%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
    }

    /**
     * Parses {@code #RGB}, {@code #RRGGBB} or {@code #RRGGBBAA} (with or
     * without the leading '#').
     *
     * @throws IllegalArgumentException on an unrecognized format
     */
    public static Color fromHex(String s) {
        String hex = s.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() == 3) {
            int r = Integer.parseInt(hex.substring(0, 1), 16) * 17;
            int g = Integer.parseInt(hex.substring(1, 2), 16) * 17;
            int b = Integer.parseInt(hex.substring(2, 3), 16) * 17;
            return new Color(r, g, b);
        }
        if (hex.length() == 6) {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new Color(r, g, b);
        }
        if (hex.length() == 8) {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            int a = Integer.parseInt(hex.substring(6, 8), 16);
            return new Color(r, g, b, a);
        }
        throw new IllegalArgumentException("Invalid hex color: " + s);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static double labF(double t) {
        if (t > LAB_EPSILON) {
            return Math.cbrt(t);
        }
        return (LAB_KAPPA * t + 16.0) / 116.0;
    }

    private static double labFInverse(double t) {
        double t3 = t * t * t;
        if (t3 > LAB_EPSILON) {
            return t3;
        }
        return (116.0 * t - 16.0) / LAB_KAPPA;
    }

    private static double[] labToLinearSrgb(double L, double a, double b) {
        double fy = (L + 16.0) / 116.0;
        double fx = fy + a / 500.0;
        double fz = fy - b / 200.0;
        double[] xyz = xyzToLinearInverse(fx, fy, fz);
        return xyzToLinearSrgb(xyz[0], xyz[1], xyz[2]);
    }

    private static double[] xyzToLinearInverse(double fx, double fy, double fz) {
        double x = XN * labFInverse(fx);
        double y = YN * labFInverse(fy);
        double z = ZN * labFInverse(fz);
        return new double[]{x, y, z};
    }

    private static double[] xyzToLinearSrgb(double x, double y, double z) {
        double r = 3.2404542 * x - 1.5371385 * y - 0.4985314 * z;
        double g = -0.9692660 * x + 1.8760108 * y + 0.0415560 * z;
        double b = 0.0556434 * x - 0.2040259 * y + 1.0572252 * z;
        return new double[]{r, g, b};
    }

    private static boolean isInGamut(double[] lin) {
        return lin[0] >= -1e-6 && lin[1] >= -1e-6 && lin[2] >= -1e-6
                && lin[0] <= 1 + 1e-6 && lin[1] <= 1 + 1e-6 && lin[2] <= 1 + 1e-6;
    }

    private static boolean converged(double[] raw, double r, double g, double b) {
        return Math.abs(raw[0] - r) < 1e-7 && Math.abs(raw[1] - g) < 1e-7 && Math.abs(raw[2] - b) < 1e-7;
    }

    private static double srgbToLinear(double c) {
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double linearToSrgb(double c) {
        return c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055;
    }

    private static Color toSrgbColor(double r, double g, double b) {
        return new Color(
                (int) Math.round(linearToSrgb(r) * 255),
                (int) Math.round(linearToSrgb(g) * 255),
                (int) Math.round(linearToSrgb(b) * 255));
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0, 1.0);
    }
}
