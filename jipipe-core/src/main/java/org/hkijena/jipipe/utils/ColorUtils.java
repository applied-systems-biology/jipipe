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

package org.hkijena.jipipe.utils;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.awt.*;
import java.util.*;
import java.util.List;

public class ColorUtils {
    public static final Map<String, Color> COLOR_MAP;
    public static final String PARSE_COLOR_DESCRIPTION = "The color can be a common color name (e.g. 'red'), a hex color (e.g. '#ff00ff'), or an RGB color (e.g. '0,128,255').";

    public static Color WHITE_TRANSPARENT = new Color(255, 255, 255, 0);
    public static Color BLACK_TRANSPARENT = new Color(0, 0, 0, 0);

    static {
        COLOR_MAP = new HashMap<>();
        COLOR_MAP.put("lightsalmon", new Color(0xFFA07A));
        COLOR_MAP.put("salmon", new Color(0xFA8072));
        COLOR_MAP.put("darksalmon", new Color(0xE9967A));
        COLOR_MAP.put("lightcoral", new Color(0xF08080));
        COLOR_MAP.put("indianred", new Color(0xCD5C5C));
        COLOR_MAP.put("crimson", new Color(0xDC143C));
        COLOR_MAP.put("firebrick", new Color(0xB22222));
        COLOR_MAP.put("red", new Color(0xFF0000));
        COLOR_MAP.put("darkred", new Color(0x8B0000));
        COLOR_MAP.put("coral", new Color(0xFF7F50));
        COLOR_MAP.put("tomato", new Color(0xFF6347));
        COLOR_MAP.put("orangered", new Color(0xFF4500));
        COLOR_MAP.put("gold", new Color(0xFFD700));
        COLOR_MAP.put("orange", new Color(0xFFA500));
        COLOR_MAP.put("darkorange", new Color(0xFF8C00));
        COLOR_MAP.put("lightyellow", new Color(0xFFFFE0));
        COLOR_MAP.put("lemonchiffon", new Color(0xFFFACD));
        COLOR_MAP.put("lightgoldenrodyellow", new Color(0xFAFAD2));
        COLOR_MAP.put("papayawhip", new Color(0xFFEFD5));
        COLOR_MAP.put("moccasin", new Color(0xFFE4B5));
        COLOR_MAP.put("peachpuff", new Color(0xFFDAB9));
        COLOR_MAP.put("palegoldenrod", new Color(0xEEE8AA));
        COLOR_MAP.put("khaki", new Color(0xF0E68C));
        COLOR_MAP.put("darkkhaki", new Color(0xBDB76B));
        COLOR_MAP.put("yellow", new Color(0xFFFF00));
        COLOR_MAP.put("lawngreen", new Color(0x7CFC00));
        COLOR_MAP.put("chartreuse", new Color(0x7FFF00));
        COLOR_MAP.put("limegreen", new Color(0x32CD32));
        COLOR_MAP.put("lime", new Color(0x00FF00));
        COLOR_MAP.put("forestgreen", new Color(0x228B22));
        COLOR_MAP.put("green", new Color(0x008000));
        COLOR_MAP.put("darkgreen", new Color(0x006400));
        COLOR_MAP.put("greenyellow", new Color(0xADFF2F));
        COLOR_MAP.put("yellowgreen", new Color(0x9ACD32));
        COLOR_MAP.put("springgreen", new Color(0x00FF7F));
        COLOR_MAP.put("mediumspringgreen", new Color(0x00FA9A));
        COLOR_MAP.put("lightgreen", new Color(0x90EE90));
        COLOR_MAP.put("palegreen", new Color(0x98FB98));
        COLOR_MAP.put("darkseagreen", new Color(0x8FBC8F));
        COLOR_MAP.put("mediumseagreen", new Color(0x3CB371));
        COLOR_MAP.put("seagreen", new Color(0x2E8B57));
        COLOR_MAP.put("olive", new Color(0x808000));
        COLOR_MAP.put("darkolivegreen", new Color(0x556B2F));
        COLOR_MAP.put("olivedrab", new Color(0x6B8E23));
        COLOR_MAP.put("lightcyan", new Color(0xE0FFFF));
        COLOR_MAP.put("cyan", new Color(0x00FFFF));
        COLOR_MAP.put("aqua", new Color(0x00FFFF));
        COLOR_MAP.put("aquamarine", new Color(0x7FFFD4));
        COLOR_MAP.put("mediumaquamarine", new Color(0x66CDAA));
        COLOR_MAP.put("paleturquoise", new Color(0xAFEEEE));
        COLOR_MAP.put("turquoise", new Color(0x40E0D0));
        COLOR_MAP.put("mediumturquoise", new Color(0x48D1CC));
        COLOR_MAP.put("darkturquoise", new Color(0x00CED1));
        COLOR_MAP.put("lightseagreen", new Color(0x20B2AA));
        COLOR_MAP.put("cadetblue", new Color(0x5F9EA0));
        COLOR_MAP.put("darkcyan", new Color(0x008B8B));
        COLOR_MAP.put("teal", new Color(0x008080));
        COLOR_MAP.put("powderblue", new Color(0xB0E0E6));
        COLOR_MAP.put("lightblue", new Color(0xADD8E6));
        COLOR_MAP.put("lightskyblue", new Color(0x87CEFA));
        COLOR_MAP.put("skyblue", new Color(0x87CEEB));
        COLOR_MAP.put("deepskyblue", new Color(0x00BFFF));
        COLOR_MAP.put("lightsteelblue", new Color(0xB0C4DE));
        COLOR_MAP.put("dodgerblue", new Color(0x1E90FF));
        COLOR_MAP.put("cornflowerblue", new Color(0x6495ED));
        COLOR_MAP.put("steelblue", new Color(0x4682B4));
        COLOR_MAP.put("royalblue", new Color(0x4169E1));
        COLOR_MAP.put("blue", new Color(0x0000FF));
        COLOR_MAP.put("mediumblue", new Color(0x0000CD));
        COLOR_MAP.put("darkblue", new Color(0x00008B));
        COLOR_MAP.put("navy", new Color(0x000080));
        COLOR_MAP.put("midnightblue", new Color(0x191970));
        COLOR_MAP.put("mediumslateblue", new Color(0x7B68EE));
        COLOR_MAP.put("slateblue", new Color(0x6A5ACD));
        COLOR_MAP.put("darkslateblue", new Color(0x483D8B));
        COLOR_MAP.put("lavender", new Color(0xE6E6FA));
        COLOR_MAP.put("thistle", new Color(0xD8BFD8));
        COLOR_MAP.put("plum", new Color(0xDDA0DD));
        COLOR_MAP.put("violet", new Color(0xEE82EE));
        COLOR_MAP.put("orchid", new Color(0xDA70D6));
        COLOR_MAP.put("fuchsia", new Color(0xFF00FF));
        COLOR_MAP.put("magenta", new Color(0xFF00FF));
        COLOR_MAP.put("mediumorchid", new Color(0xBA55D3));
        COLOR_MAP.put("mediumpurple", new Color(0x9370DB));
        COLOR_MAP.put("blueviolet", new Color(0x8A2BE2));
        COLOR_MAP.put("darkviolet", new Color(0x9400D3));
        COLOR_MAP.put("darkorchid", new Color(0x9932CC));
        COLOR_MAP.put("darkmagenta", new Color(0x8B008B));
        COLOR_MAP.put("purple", new Color(0x800080));
        COLOR_MAP.put("indigo", new Color(0x4B0082));
        COLOR_MAP.put("pink", new Color(0xFFC0CB));
        COLOR_MAP.put("lightpink", new Color(0xFFB6C1));
        COLOR_MAP.put("hotpink", new Color(0xFF69B4));
        COLOR_MAP.put("deeppink", new Color(0xFF1493));
        COLOR_MAP.put("palevioletred", new Color(0xDB7093));
        COLOR_MAP.put("mediumvioletred", new Color(0xC71585));
        COLOR_MAP.put("white", new Color(0xFFFFFF));
        COLOR_MAP.put("snow", new Color(0xFFFAFA));
        COLOR_MAP.put("honeydew", new Color(0xF0FFF0));
        COLOR_MAP.put("mintcream", new Color(0xF5FFFA));
        COLOR_MAP.put("azure", new Color(0xF0FFFF));
        COLOR_MAP.put("aliceblue", new Color(0xF0F8FF));
        COLOR_MAP.put("ghostwhite", new Color(0xF8F8FF));
        COLOR_MAP.put("whitesmoke", new Color(0xF5F5F5));
        COLOR_MAP.put("seashell", new Color(0xFFF5EE));
        COLOR_MAP.put("beige", new Color(0xF5F5DC));
        COLOR_MAP.put("oldlace", new Color(0xFDF5E6));
        COLOR_MAP.put("floralwhite", new Color(0xFFFAF0));
        COLOR_MAP.put("ivory", new Color(0xFFFFF0));
        COLOR_MAP.put("antiquewhite", new Color(0xFAEBD7));
        COLOR_MAP.put("linen", new Color(0xFAF0E6));
        COLOR_MAP.put("lavenderblush", new Color(0xFFF0F5));
        COLOR_MAP.put("mistyrose", new Color(0xFFE4E1));
        COLOR_MAP.put("gainsboro", new Color(0xDCDCDC));
        COLOR_MAP.put("lightgray", new Color(0xD3D3D3));
        COLOR_MAP.put("silver", new Color(0xC0C0C0));
        COLOR_MAP.put("darkgray", new Color(0xA9A9A9));
        COLOR_MAP.put("gray", new Color(0x808080));
        COLOR_MAP.put("dimgray", new Color(0x696969));
        COLOR_MAP.put("lightslategray", new Color(0x778899));
        COLOR_MAP.put("slategray", new Color(0x708090));
        COLOR_MAP.put("darkslategray", new Color(0x2F4F4F));
        COLOR_MAP.put("black", new Color(0x000000));
        COLOR_MAP.put("cornsilk", new Color(0xFFF8DC));
        COLOR_MAP.put("blanchedalmond", new Color(0xFFEBCD));
        COLOR_MAP.put("bisque", new Color(0xFFE4C4));
        COLOR_MAP.put("navajowhite", new Color(0xFFDEAD));
        COLOR_MAP.put("wheat", new Color(0xF5DEB3));
        COLOR_MAP.put("burlywood", new Color(0xDEB887));
        COLOR_MAP.put("tan", new Color(0xD2B48C));
        COLOR_MAP.put("rosybrown", new Color(0xBC8F8F));
        COLOR_MAP.put("sandybrown", new Color(0xF4A460));
        COLOR_MAP.put("goldenrod", new Color(0xDAA520));
        COLOR_MAP.put("peru", new Color(0xCD853F));
        COLOR_MAP.put("chocolate", new Color(0xD2691E));
        COLOR_MAP.put("saddlebrown", new Color(0x8B4513));
        COLOR_MAP.put("sienna", new Color(0xA0522D));
        COLOR_MAP.put("brown", new Color(0xA52A2A));
        COLOR_MAP.put("maroon", new Color(0x800000));
    }

    /**
     * Applies scaling in HSV space
     *
     * @param color   the color
     * @param hFactor factor for H
     * @param sFactor factor for S
     * @param vFactor factor for V
     * @return scaled color
     */
    public static Color scaleHSV(Color color, float hFactor, float sFactor, float vFactor) {
        float[] hsv = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hsv[0] *= hFactor;
        hsv[1] *= sFactor;
        hsv[2] *= vFactor;
        return Color.getHSBColor(hsv[0], hsv[1], hsv[2]);
    }

    /**
     * Parses a color
     *
     * @param colorString the color string
     * @return the color or null if none was found
     */
    public static Color parseColor(String colorString) {
        if (colorString.startsWith("#")) {
            return hexStringToColor(colorString);
        } else if (colorString.contains(",")) {
            String[] components = colorString.replace(" ", "").split(",");
            return new Color(Integer.parseInt(components[0]), Integer.parseInt(components[1]), Integer.parseInt(components[2]));
        } else {
            return COLOR_MAP.get(colorString.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", ""));
        }
    }

    /**
     * Converts a color to a Hex string
     *
     * @param color the color
     * @return A hex string #RRGGBB or #RRGGBBAA (only if alpha is not 255)
     */
    public static String colorToHexString(Color color) {
        if (color.getAlpha() == 255) {
            return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        } else {
            return String.format("#%02X%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        }
    }

    /**
     * Converts a hex color string #RRGGBB or #RRGGBBAA to a color
     *
     * @param s the string
     * @return the color
     */
    public static Color hexStringToColor(String s) {
        if (s.length() == 9) {
            // This is #RRGGBBAA
            Color rgb = Color.decode(s.substring(0, 7));
            int alpha = Integer.parseInt(s.substring(7));
            return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha);
        } else {
            return Color.decode(s);
        }
    }

    public static Color setAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Color[] renderGradient(List<GradientStop> stops, int numColors) {
        stops.sort(Comparator.naturalOrder());
        if (stops.get(0).position > 0)
            stops.add(0, new GradientStop(0, stops.get(0).getColor()));
        if (stops.get(stops.size() - 1).position < 1)
            stops.add(new GradientStop(1, stops.get(stops.size() - 1).getColor()));
        int[] reds = new int[numColors + 1];
        int[] greens = new int[numColors + 1];
        int[] blues = new int[numColors + 1];
        int currentFirstStop = 0;
        int currentLastStop = 1;
        int startIndex = 0;
        int endIndex = (int) (numColors * stops.get(currentLastStop).position);
        for (int i = 0; i < numColors + 1; i++) {
            if (i != numColors && i >= endIndex) {
                startIndex = i;
                ++currentFirstStop;
                ++currentLastStop;
                endIndex = (int) (numColors * stops.get(currentLastStop).position);
            }
            Color currentStart = stops.get(currentFirstStop).getColor();
            Color currentEnd = stops.get(currentLastStop).getColor();
            int r0 = currentStart.getRed();
            int g0 = currentStart.getGreen();
            int b0 = currentStart.getBlue();
            int r1 = currentEnd.getRed();
            int g1 = currentEnd.getGreen();
            int b1 = currentEnd.getBlue();
            int r = (int) (r0 + (r1 - r0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            int g = (int) (g0 + (g1 - g0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            int b = (int) (b0 + (b1 - b0) * (1.0 * (i - startIndex) / (endIndex - startIndex)));
            reds[i] = r;
            greens[i] = g;
            blues[i] = b;
        }
        Color[] result = new Color[numColors];
        for (int i = 0; i < numColors; i++) {
            result[i] = new Color(reds[i], greens[i], blues[i]);
        }
        return result;
    }

    public static Color multiplyHSB(Color color, float factorH, float factorS, float factorB) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(Math.max(0, Math.min(1, hsb[0] * factorH)), Math.max(0, Math.min(1, hsb[1] * factorS)), Math.max(0, Math.min(1, hsb[2] * factorB)));
    }

    public static Color mix(Color first, Color second, double percentage) {
        int newR = (int) (first.getRed() + percentage * (second.getRed() - first.getRed()));
        int newG = (int) (first.getGreen() + percentage * (second.getGreen() - first.getGreen()));
        int newB = (int) (first.getBlue() + percentage * (second.getBlue() - first.getBlue()));
        return new Color(newR, newG, newB);
    }

    public static Color toGreyscale(Color color) {
        int v = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
        return new Color(v, v, v);
    }

    public static boolean isGreyscale(Color color) {
        return color.getRed() == color.getGreen() && color.getGreen() == color.getBlue();
    }

    public static Color invertRGBA(Color color) {
        return new Color(~color.getRGB());
    }

    public static Color invertRGB(Color color) {
        return new Color(255 - color.getRed(),
                255 - color.getGreen(),
                255 - color.getBlue(),
                color.getAlpha());
    }

    public static class GradientStop implements Comparable<GradientStop> {
        private Color color;
        private float position;

        public GradientStop() {
        }

        public GradientStop(float position, Color color) {
            this.position = position;
            this.color = color;
        }

        public GradientStop(GradientStop other) {
            this.position = other.position;
            this.color = other.color;
        }

        @JsonGetter("position")
        public float getPosition() {
            return position;
        }

        @JsonSetter("position")
        public void setPosition(float position) {
            this.position = position;
        }

        @JsonGetter("color")
        public Color getColor() {
            return color;
        }

        @JsonSetter("color")
        public void setColor(Color color) {
            this.color = color;
        }

        @Override
        public int compareTo(GradientStop o) {
            return Float.compare(position, o.position);
        }
    }
}
