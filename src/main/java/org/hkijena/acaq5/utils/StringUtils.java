/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.utils;

import com.google.common.html.HtmlEscapers;
import org.apache.commons.lang.WordUtils;

import java.net.URL;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * Utilities for strings
 */
public class StringUtils {

    public static final char[] INVALID_FILESYSTEM_CHARACTERS = new char[]{'<', '>', ':', '"', '/', '\\', '|', '?', '*', '{', '}'};
    public static final char[] INVALID_JSONIFY_CHARACTERS = new char[]{'<', '>', ':', '"', '/', '\\', '|', '?', '*', '{', '}', ' ', '_'};

    private StringUtils() {

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
        return "<table><tr><td>" + HtmlEscapers.htmlEscaper().escape(text) + "</td><td><img src=\"" + iconURL + "\" /></td><td></tr></table>";
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
    public static String wordWrappedInHTML(String text, int wrapColumn) {
        if (text == null)
            text = "";
        return "<html>" + WordUtils.wrap(HtmlEscapers.htmlEscaper().escape(text), wrapColumn).replace("\n", "<br/>") + "</html>";
    }

    /**
     * Returns true if the string is null or empty
     *
     * @param string the string
     * @return if the string is null or empty
     */
    public static boolean isNullOrEmpty(String string) {
        return string == null || string.isEmpty();
    }

    /**
     * Returns the string if its not null or empty or the alternative
     *
     * @param string        the string
     * @param ifNullOrEmpty returned if string is null or empty
     * @return string or ifNullOrEmpty depending on if string is null or empty
     */
    public static String orElse(String string, String ifNullOrEmpty) {
        return isNullOrEmpty(string) ? ifNullOrEmpty : string;
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
        input = input.trim().toLowerCase();
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
        return menuPath;
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
}
