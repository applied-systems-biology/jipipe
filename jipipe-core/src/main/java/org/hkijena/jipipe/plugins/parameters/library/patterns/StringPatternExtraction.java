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

package org.hkijena.jipipe.plugins.parameters.library.patterns;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A parameter that extracts a pattern from a string
 */
public class StringPatternExtraction implements Function<String, String>, JIPipeValidatable {

    private Mode mode = Mode.SplitAndPick;
    private String splitCharacter = "_";
    private int splitPickedIndex = 0;
    private String regexString = "";


    /**
     * Creates a new instance
     */
    public StringPatternExtraction() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public StringPatternExtraction(StringPatternExtraction other) {
        this.mode = other.mode;
        this.splitCharacter = other.splitCharacter;
        this.splitPickedIndex = other.splitPickedIndex;
        this.regexString = other.regexString;
    }

    @Override
    public String apply(String s) {
        if (s == null)
            s = "";
        switch (mode) {
            case SplitAndPick: {
                String[] components = s.split(splitCharacter);
                return splitPickedIndex < components.length ? components[splitPickedIndex] : null;
            }
            case SplitAndFind: {
                String[] components = s.split(splitCharacter);
                for (String component : components) {
                    if (component.matches(regexString))
                        return component;
                }
                return null;
            }
            case Regex: {
                Matcher matcher = Pattern.compile(regexString).matcher(s);
                if (matcher.find()) {
                    return matcher.groupCount() > 0 ? matcher.group(1) : null;
                }
                return null;
            }
            default:
                throw new UnsupportedOperationException("Unsupported: " + mode);
        }
    }

    @JsonGetter("mode")
    public Mode getMode() {
        return mode;
    }

    @JsonSetter("mode")
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @JsonGetter("split-character")
    public String getSplitCharacter() {
        return splitCharacter;
    }

    @JsonSetter("split-character")
    public void setSplitCharacter(String splitCharacter) {
        this.splitCharacter = splitCharacter;
    }

    @JsonGetter("split-selected-index")
    public int getSplitPickedIndex() {
        return splitPickedIndex;
    }

    @JsonSetter("split-selected-index")
    public void setSplitPickedIndex(int splitPickedIndex) {
        this.splitPickedIndex = splitPickedIndex;
    }

    @JsonGetter("regex-string")
    public String getRegexString() {
        return regexString;
    }

    @JsonSetter("regex-string")
    public void setRegexString(String regexString) {
        this.regexString = regexString;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {
        switch (mode) {
            case SplitAndPick:
                if (StringUtils.isNullOrEmpty(splitCharacter)) {
                    reportContext.custom("Split character").error().title("Empty split character!").explanation("The split character cannot be empty!").report(report);
                }
                break;
            case SplitAndFind:
                if (StringUtils.isNullOrEmpty(splitCharacter)) {
                    reportContext.custom("Split character").error().title("Empty split character!").explanation("The split character cannot be empty!").report(report);
                }
                if (splitPickedIndex < 0) {
                    reportContext.custom("Selected index").error().title("Negative selected index!").explanation("The selected index cannot be negative!").report(report);
                }
                break;
            case Regex:
                try {
                    Pattern.compile(regexString);
                } catch (PatternSyntaxException e) {
                    reportContext.custom("RegEx").error().title("RegEx syntax is wrong!").explanation("The regular expression string is wrong.").solution("Please check the syntax. If you are not familiar with it, you can find plenty of resources online.").report(report);
                }
                break;
        }
    }

    @Override
    public String toString() {
        switch (mode) {
            case Regex:
                return "RegexSelect " + regexString;
            case SplitAndPick:
                return "Split(\"" + splitCharacter + "\")[" + splitPickedIndex + "]";
            case SplitAndFind:
                return "Split(\"" + splitCharacter + "\")[WHERE REGEX " + regexString + "]";
            default:
                throw new UnsupportedOperationException("Unsupported: " + mode);
        }
    }

    /**
     * Available modes
     */
    public enum Mode {
        SplitAndPick,
        SplitAndFind,
        Regex
    }

}
