package org.hkijena.acaq5.utils;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Predicate;

/**
 * A filter for path filenames that can handle multiple filter modes
 */
public class PathFilter implements Predicate<Path> {

    private Mode mode = Mode.Contains;
    private String filterString;
    private PathMatcher globPathMatcher;

    public PathFilter() {

    }

    @JsonGetter
    public Mode getMode() {
        return mode;
    }

    @JsonSetter
    public void setMode(Mode mode) {
        this.mode = mode;
        if(mode == Mode.Glob)
            setFilterString(filterString);
    }

    @JsonGetter
    public String getFilterString() {
        return filterString;
    }

    @JsonSetter
    public void setFilterString(String filterString) {
        this.filterString = filterString;
        if(mode == Mode.Glob && filterString != null && !filterString.isEmpty()) {
            try {
                globPathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + filterString);
            }
            catch (Exception e) {
                globPathMatcher = null;
            }
        }
        else {
            globPathMatcher = null;
        }
    }

    @Override
    public boolean test(Path path) {
        switch(mode) {
            case Contains:
                return path.getFileName().toString().contains(filterString);
            case Glob:
                return globPathMatcher.matches(path.getFileName());
            case Regex:
                return path.getFileName().toString().matches(filterString);
            default:
                throw new RuntimeException("Unknown mode!");
        }
    }

    public enum Mode {
        Contains,
        Glob,
        Regex
    }

}
