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

import com.google.common.html.HtmlEscapers;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.text.WordUtils;

import java.awt.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Utilities for strings
 */
public class StringUtils {

    public static final char[] INVALID_FILESYSTEM_CHARACTERS = new char[]{'<', '>', ':', '"', '/', '\\', '|', '?', '*', '{', '}'};
    public static final char[] INVALID_JSONIFY_CHARACTERS = new char[]{'<', '>', ':', '"', '/', '\\', '|', '?', '*', '{', '}', ' ', '_'};
    private static final Random RANDOM = new Random();

    private StringUtils() {

    }

    public static boolean isValidDouble(String text) {
        if (NumberUtils.isCreatable(text)) {
            return true;
        }
        if (StringUtils.isNullOrEmpty(text)) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).startsWith("-inf") || text.toLowerCase(Locale.ROOT).startsWith("inf") || text.equalsIgnoreCase("nan") || text.equalsIgnoreCase("na");
    }

    public static boolean isValidInteger(String text) {
        return NumberUtils.isCreatable(text);
    }

    public static String formatPluralS(int count, String labelSingular) {
        if (count == 1) {
            return count + " " + labelSingular;
        } else {
            return count + " " + labelSingular + "s";
        }
    }

    /**
     * If the string is longer than maxWidth, limit it to fit within maxWidth
     * with an ellipsis (...). Will always return at least the string "..."
     */
    public static String limitWithEllipsis(String str, int maxWidth, FontMetrics metrics) {
        int strWidth = metrics.stringWidth(str);
        if (strWidth <= maxWidth)
            return str;
        for (int len = str.length() - 1; len > 0; len--) {
            String subStr = str.substring(0, len) + "...";
            if (metrics.stringWidth(subStr) <= maxWidth)
                return subStr;
        }
        return "...";
    }

    /**
     * If the string is longer than maxWidth, limit it to fit within maxWidth
     * with an ellipsis (...). Will always return at least the string "..."
     */
    public static String limitWithoutEllipsis(String str, int maxWidth, FontMetrics metrics) {
        int strWidth = metrics.stringWidth(str);
        if (strWidth <= maxWidth)
            return str;
        for (int len = str.length() - 1; len > 0; len--) {
            String subStr = str.substring(0, len);
            if (metrics.stringWidth(subStr) <= maxWidth)
                return subStr;
        }
        return "";
    }

    public static String removeDuplicateDelimiters(String string, String delimiter) {
        while (string.contains(delimiter + delimiter)) {
            string = string.replace(delimiter + delimiter, delimiter);
        }
        return string;
    }

    /**
     * Formats a duration in milliseconds to something readable
     *
     * @param durationMilliseconds the duration
     * @return formatted string
     */
    public static String formatDuration(long durationMilliseconds) {
        return DurationFormatUtils.formatDuration(durationMilliseconds, "HH:mm:ss,SSS");
    }

    /**
     * A nice human-readable format
     *
     * @param dateTime the time point
     * @return formatted string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE) + " " +
                dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    /**
     * Creates an HTML table with an icon and text.
     * Contains root elements.
     *
     * @param text    the text. Is automatically escaped
     * @param iconURL the icon
     * @return the HTML
     */
    public static String createIconTextHTMLTable(String text, URL iconURL) {
        return "<html>" + createIconTextHTMLTableElement(text, iconURL) + " </html>";
    }

    /**
     * Creates an HTML table with an icon and text.
     * Has no HTML root
     *
     * @param text    the text. Is automatically escaped
     * @param iconURL the icon
     * @return the HTML element
     */
    public static String createIconTextHTMLTableElement(String text, URL iconURL) {
        return "<table><tr><td><img src=\"" + iconURL + "\" /></td><td>" + HtmlEscapers.htmlEscaper().escape(text) + "</td></tr></table>";
    }

    /**
     * Same as createIconTextHTMLTable but the icon is on the right side
     *
     * @param text    the text. Is automatically escaped
     * @param iconURL the icon
     * @return the HTML
     */
    public static String createRightIconTextHTMLTable(String text, URL iconURL) {
        return "<html>" + createRightIconTextHTMLTableElement(text, iconURL) + "</html>";
    }

    /**
     * Same as createIconTextHTMLTableElement but the icon is on the right side
     *
     * @param text    the text. Is automatically escaped
     * @param iconURL the icon
     * @return the HTML
     */
    public static String createRightIconTextHTMLTableElement(String text, URL iconURL) {
        return "<table><tr><td>" + HtmlEscapers.htmlEscaper().escape(text) + "</td><td><img src=\"" + iconURL + "\" /></td></tr></table>";
    }

    /**
     * Applies a \0 termination to a string.
     * Used to parse unclean input from C
     *
     * @param input the input string
     * @return string that ends at the first \0
     */
    public static String nullTerminate(String input) {
        int firstNull = -1;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '\0') {
                firstNull = i;
                break;
            }
        }
        if (firstNull == -1) {
            return input;
        } else {
            return input.substring(0, firstNull);
        }
    }

    /**
     * Removes the HTML root from HTML
     *
     * @param html the HTML
     * @return html without root
     */
    public static String removeHTMLRoot(String html) {
        return html.replace("<html>", "").replace("</html>", "");
    }

    /**
     * Create word wrapping in HTML.
     * Has HTML root element.
     *
     * @param text       the wrapped text. It is automatically escaped
     * @param wrapColumn after how many columns to break words
     * @return the wrapped text
     */
    public static String wordWrappedHTML(String text, int wrapColumn) {
        if (text == null)
            text = "";
        return "<html>" + wordWrappedHTMLElement(text, wrapColumn) + "</html>";
    }

    /**
     * Generates a random string
     *
     * @param characters the available characters
     * @param length     the length
     * @return the string
     */
    public static String generateRandomString(String characters, int length) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            stringBuilder.append(characters.charAt(RANDOM.nextInt(characters.length())));
        }
        return stringBuilder.toString();
    }

    /**
     * Create word wrapping in HTML.
     * Has HTML root element.
     *
     * @param text       the wrapped text. It is automatically escaped
     * @param wrapColumn after how many columns to break words
     * @return the wrapped text
     */
    public static String wordWrappedHTMLElement(String text, int wrapColumn) {
        if (text == null)
            text = "";
        return WordUtils.wrap(HtmlEscapers.htmlEscaper().escape(text), wrapColumn).replace("\n", "<br/>");
    }

    /**
     * Returns true if the string is null or empty
     *
     * @param string the string
     * @return if the string is null or empty
     */
    public static boolean isNullOrEmpty(Object string) {
        if (string instanceof String)
            return ((String) string).isEmpty();
        else if (string != null)
            return ("" + string).isEmpty();
        else
            return true;
    }

    /**
     * Returns the string if its not null or empty or the alternative
     *
     * @param string        the string
     * @param ifNullOrEmpty returned if string is null or empty
     * @return string or ifNullOrEmpty depending on if string is null or empty
     */
    public static String orElse(Object string, String ifNullOrEmpty) {
        return isNullOrEmpty(string) ? ifNullOrEmpty : "" + string;
    }

    /**
     * Removes all spaces, and filesystem-unsafe characters from the string and replaces them with dashes
     * Duplicate dashes are removed
     *
     * @param input the input string
     * @return jsonified string
     */
    public static String safeJsonify(String input) {
        if (input == null)
            return null;
        char[] arr = input.trim().toLowerCase(Locale.ROOT).toCharArray();
        for (int i = 0; i < arr.length; i++) {
            if (!Character.isLetterOrDigit(arr[i]) && arr[i] != '-') {
                arr[i] = '-';
            }
        }
        input = new String(arr);
        while (input.contains("--")) {
            input = input.replace("--", "-");
        }
        while (input.startsWith("-")) {
            input = input.substring(1);
        }
        while (input.endsWith("-")) {
            input = input.substring(0, input.length() - 1);
        }
        if (input.isEmpty()) {
            input = "empty";
        }
        return input;
    }

    /**
     * Removes all spaces, and filesystem-unsafe characters from the string and replaces them with dashes
     * Duplicate dashes are removed
     *
     * @param input the input string
     * @return jsonified string
     */
    public static String jsonify(String input) {
        if (input == null)
            return null;
        input = input.trim().toLowerCase(Locale.ROOT);
        for (char c : INVALID_JSONIFY_CHARACTERS) {
            input = input.replace(c, '-');
        }
        while (input.contains("--")) {
            input = input.replace("--", "-");
        }
        return input;
    }

    /**
     * Replaces all characters invalid for filesystems with spaces
     * Assumes that the string is a filename, so path operators are not allowed.
     * Applies the limits for file / path names
     *
     * @param input filename
     * @return string compatible with file systems
     */
    public static String makeFilesystemCompatible(String input) {
        if (input == null)
            return null;
        for (char c : INVALID_FILESYSTEM_CHARACTERS) {
            input = input.replace(c, ' ');
        }
        if (input.length() >= 255) {
            input = input.substring(0, 255);
        }
        return input;
    }

    /**
     * Makes an unique string by adding a counting variable to its end if it already exists
     *
     * @param input          initial string
     * @param spaceCharacter character to add between the string and counting variable
     * @param existing       collection of existing strings
     * @return unique string
     */
    public static String makeUniqueString(String input, String spaceCharacter, Collection<String> existing) {
        return makeUniqueString(input, spaceCharacter, existing::contains);
    }

    /**
     * Makes an unique string by adding a counting variable to its end if it already exists
     *
     * @param input          initial string
     * @param spaceCharacter character to add between the string and counting variable
     * @param existing       function that returns true if the string already exists
     * @return unique string
     */
    public static String makeUniqueString(String input, String spaceCharacter, Predicate<String> existing) {
        if (!existing.test(input))
            return input;
        int index = 1;
        while (existing.test(input + spaceCharacter + index)) {
            ++index;
        }
        return input + spaceCharacter + index;
    }

    /**
     * Cleans a menu path string
     *
     * @param menuPath a menu path
     * @return cleaned string
     */
    public static String getCleanedMenuPath(String menuPath) {
        if (menuPath == null)
            return "";
        while (menuPath.contains("  ") || menuPath.contains("\n\n") || menuPath.contains("\n ") || menuPath.contains(" \n"))
            menuPath = menuPath
                    .replace("  ", " ")
                    .replace("\n\n", "\n")
                    .replace("\n ", "\n")
                    .replace(" \n", "\n");
        return menuPath.trim();
    }

    /**
     * Returns if the string does not contain invalid characters.
     * Assumes that the string is a filename, so path operators are not allowed.
     *
     * @param string the filename
     * @return if the filename is valid
     */
    public static boolean isFilesystemCompatible(String string) {
        for (char c : INVALID_FILESYSTEM_CHARACTERS) {
            if (string.contains(c + "")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns an empty string if s is null otherwise returns s
     *
     * @param s the string
     * @return an empty string if s is null otherwise returns s
     */
    public static String nullToEmpty(Object s) {
        return s == null ? "" : "" + s;
    }

    /**
     * Converts a standard POSIX Shell globbing pattern into a regular expression
     * pattern. The result can be used with the standard {@link java.util.regex} API to
     * recognize strings which match the glob pattern.
     * <p>
     * See also, the POSIX Shell language:
     * <a href="http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01">...</a>
     * <p>
     * See <a href="https://stackoverflow.com/questions/1247772/is-there-an-equivalent-of-java-util-regex-for-glob-type-patterns">...</a>
     *
     * @param pattern A glob pattern.
     * @return A regex pattern to recognize the given glob pattern.
     */
    public static String convertGlobToRegex(String pattern) {
        StringBuilder sb = new StringBuilder(pattern.length());
        int inGroup = 0;
        int inClass = 0;
        int firstIndexInClass = -1;
        char[] arr = pattern.toCharArray();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            switch (ch) {
                case '\\':
                    if (++i >= arr.length) {
                        sb.append('\\');
                    } else {
                        char next = arr[i];
                        switch (next) {
                            case ',':
                                // escape not needed
                                break;
                            case 'Q':
                            case 'E':
                                // extra escape needed
                                sb.append('\\');
                            default:
                                sb.append('\\');
                        }
                        sb.append(next);
                    }
                    break;
                case '*':
                    if (inClass == 0)
                        sb.append(".*");
                    else
                        sb.append('*');
                    break;
                case '?':
                    if (inClass == 0)
                        sb.append('.');
                    else
                        sb.append('?');
                    break;
                case '[':
                    inClass++;
                    firstIndexInClass = i + 1;
                    sb.append('[');
                    break;
                case ']':
                    inClass--;
                    sb.append(']');
                    break;
                case '.':
                case '(':
                case ')':
                case '+':
                case '|':
                case '^':
                case '$':
                case '@':
                case '%':
                    if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                        sb.append('\\');
                    sb.append(ch);
                    break;
                case '!':
                    if (firstIndexInClass == i)
                        sb.append('^');
                    else
                        sb.append('!');
                    break;
                case '{':
                    inGroup++;
                    sb.append('(');
                    break;
                case '}':
                    inGroup--;
                    sb.append(')');
                    break;
                case ',':
                    if (inGroup > 0)
                        sb.append('|');
                    else
                        sb.append(',');
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Compares two version strings. Only numeric versions are supported
     *
     * @param version1 the first version
     * @param version2 the second version
     * @return -1 if version1 is less than version2. 1 if version2 is less than version1. 0 if equal
     */
    public static int compareVersions(String version1, String version2) {
        int comparisonResult = 0;

        String[] version1Splits = org.apache.commons.lang3.StringUtils.split(version1, ".-");
        String[] version2Splits = org.apache.commons.lang3.StringUtils.split(version2, ".-");
        int maxLengthOfVersionSplits = Math.max(version1Splits.length, version2Splits.length);

        for (int i = 0; i < maxLengthOfVersionSplits; i++) {
            Double v1 = i < version1Splits.length ? tryParseDouble(version1Splits[i], 0) : 0;
            Double v2 = i < version2Splits.length ? tryParseDouble(version2Splits[i], 0) : 0;
            int compare = v1.compareTo(v2);
            if (compare != 0) {
                comparisonResult = compare;
                break;
            }
        }
        return comparisonResult;
    }

    /**
     * Attempts to parse a string as double.
     * If not successful, return the string
     *
     * @param str the string
     * @return the number or the string
     */
    public static Object tryParseDoubleOrReturnString(String str) {
        str = StringUtils.nullToEmpty(str);
        str = str.replace(',', '.').replace(" ", "");
        double value;
        if (NumberUtils.isCreatable(str)) {
            value = NumberUtils.createDouble(str);
        } else if (StringUtils.isNullOrEmpty(str)) {
            value = 0d;
        } else if (str.toLowerCase(Locale.ROOT).startsWith("-inf")) {
            value = Double.NEGATIVE_INFINITY;
        } else if (str.toLowerCase(Locale.ROOT).startsWith("inf")) {
            value = Double.POSITIVE_INFINITY;
        } else if (str.equalsIgnoreCase("na") || str.equalsIgnoreCase("nan")) {
            value = Double.NaN;
        } else {
            return str;
        }
        return value;
    }

    public static double tryParseDouble(String str, double defaultValue) {
        str = StringUtils.nullToEmpty(str);
        str = str.replace(',', '.').replace(" ", "");
        double value;
        if (NumberUtils.isCreatable(str)) {
            value = NumberUtils.createDouble(str);
        } else if (StringUtils.isNullOrEmpty(str)) {
            value = 0d;
        } else if (str.toLowerCase(Locale.ROOT).startsWith("-inf")) {
            value = Double.NEGATIVE_INFINITY;
        } else if (str.toLowerCase(Locale.ROOT).startsWith("inf")) {
            value = Double.POSITIVE_INFINITY;
        } else if (str.equalsIgnoreCase("na") || str.equalsIgnoreCase("nan")) {
            value = Double.NaN;
        } else {
            value = defaultValue;
        }
        return value;
    }

    public static int parseInt(String str) {
        double v = parseDouble(str);
        if (Double.isNaN(v)) {
            return 0;
        }
        if (Double.isInfinite(v)) {
            return v < 0 ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        }
        return (int) v;
    }

    public static double parseDouble(String str) {
        str = StringUtils.nullToEmpty(str);
        str = str.replace(',', '.').replace(" ", "");
        double value;
        if (NumberUtils.isCreatable(str)) {
            value = NumberUtils.createDouble(str);
        } else if (StringUtils.isNullOrEmpty(str)) {
            value = 0d;
        } else if (str.toLowerCase(Locale.ROOT).startsWith("-inf")) {
            value = Double.NEGATIVE_INFINITY;
        } else if (str.toLowerCase(Locale.ROOT).startsWith("inf")) {
            value = Double.POSITIVE_INFINITY;
        } else if (str.equalsIgnoreCase("na") || str.equalsIgnoreCase("nan")) {
            value = Double.NaN;
        } else {
            throw new NumberFormatException("String is not a number: " + str);
        }
        return value;
    }

    public static float parseFloat(String str) {
        str = StringUtils.nullToEmpty(str);
        str = str.replace(',', '.').replace(" ", "");
        float value;
        if (NumberUtils.isCreatable(str)) {
            value = NumberUtils.createFloat(str);
        } else if (StringUtils.isNullOrEmpty(str)) {
            value = 0f;
        } else if (str.toLowerCase(Locale.ROOT).startsWith("-inf")) {
            value = Float.NEGATIVE_INFINITY;
        } else if (str.toLowerCase(Locale.ROOT).startsWith("inf")) {
            value = Float.POSITIVE_INFINITY;
        } else if (str.equalsIgnoreCase("na") || str.equalsIgnoreCase("nan")) {
            value = Float.NaN;
        } else {
            throw new NumberFormatException("String is not a number: " + str);
        }
        return value;
    }

    public static double objectToDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        } else {
            return StringUtils.parseDouble(StringUtils.nullToEmpty(o));
        }
    }

    public static boolean isValidVersion(String version) {
        return version.matches("\\d+(\\.\\d+)*");
    }

    public static String[] splitNonRegex(String s, String delimiter) {
        return s.split(Pattern.quote(delimiter));
    }

    public static String wrapHtml(String text) {
        if (!text.trim().startsWith("<html>")) {
            return "<html>" + text + "</html>";
        }
        return text;
    }

    public static String formatSize(long size) {
        if (size <= 0) return "0 bytes";

        final String[] units = {"bytes", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double sizeAsDouble = size;

        while (sizeAsDouble >= 1024 && unitIndex < units.length - 1) {
            sizeAsDouble /= 1024;
            unitIndex++;
        }

        if (sizeAsDouble < 10 && unitIndex > 0) {
            // For smaller sizes like 1.5 KB, show one decimal place
            return String.format("%.1f %s", sizeAsDouble, units[unitIndex]);
        } else {
            // Otherwise, show whole number
            return String.format("%d %s", Math.round(sizeAsDouble), units[unitIndex]);
        }
    }

}
