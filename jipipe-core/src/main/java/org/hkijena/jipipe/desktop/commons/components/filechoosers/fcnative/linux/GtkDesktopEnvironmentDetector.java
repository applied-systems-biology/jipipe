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

package org.hkijena.jipipe.desktop.commons.components.filechoosernative.linux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

public class GtkDesktopEnvironmentDetector {
    // Desktop environments commonly regarded as GTK-based
    private static final Set<String> GTK_DESKTOPS = Set.of(
            "gnome", "cinnamon", "mate", "xfce", "xfce4",
            "lxde", "budgie", "pantheon", "unity"
    );
    // Not GTK-based (mostly Qt)
    private static final Set<String> NON_GTK_DESKTOPS = Set.of(
            "kde", "plasma", "lxqt", "deepin", "cutefish", "trinity"
    );
    private static final List<String> DESKTOP_ENV_VARS = List.of(
            "XDG_CURRENT_DESKTOP", "XDG_SESSION_DESKTOP", "DESKTOP_SESSION",
            "GDMSESSION", "GNOME_DESKTOP_SESSION_ID"
    );

    /**
     * Detects if we appear to be running a GTK-based desktop environment on Linux.
     */
    public static Result detect() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("linux")) {
            return new Result(Verdict.UNKNOWN, "Not a Linux OS (os.name=" + os + ")");
        }

        var env = System.getenv();
        var tokens = new LinkedHashSet<String>(); // keep order for nicer messages

        // Collect tokens from common desktop-identifying env vars
        Pattern splitter = Pattern.compile("[^a-zA-Z0-9]+"); // split on non-alphanumerics (handles ':', ';', spaces, etc.)
        for (String key : DESKTOP_ENV_VARS) {
            var val = env.getOrDefault(key, "").trim();
            if (!val.isEmpty()) {
                for (String part : splitter.split(val.toLowerCase(Locale.ROOT))) {
                    if (!part.isEmpty()) tokens.add(part);
                }
            }
        }

        // Heuristic 1: explicit positive/negative matches
        for (String t : tokens) {
            if (GTK_DESKTOPS.contains(t)) {
                return new Result(Verdict.YES, "Matched GTK desktop token: " + t);
            }
            if (NON_GTK_DESKTOPS.contains(t)) {
                return new Result(Verdict.NO, "Matched non-GTK desktop token: " + t);
            }
        }

        // Heuristic 2: GTK-specific environment hints
        // (These indicate a GTK-centric session more often than not.)
        if (!Optional.ofNullable(env.get("GTK_THEME")).orElse("").isBlank()
                || !Optional.ofNullable(env.get("GTK_MODULES")).orElse("").isBlank()) {
            return new Result(Verdict.YES, "GTK_* environment variables present");
        }

        // Heuristic 3: user/system GTK config folders present (weak signal)
        var home = Path.of(System.getProperty("user.home", ""));
        var gtk3UserCfg = home.resolve(".config/gtk-3.0");
        var gtk2UserCfg = home.resolve(".gtkrc-2.0");
        if (Files.isDirectory(gtk3UserCfg) || Files.isRegularFile(gtk2UserCfg)) {
            return new Result(Verdict.YES, "GTK config detected in user profile");
        }

        // Couldn’t be sure either way
        return new Result(Verdict.UNKNOWN, "No decisive desktop/session hints found");
    }

    // Demo
    public static void main(String[] args) {
        Result r = detect();
        System.out.println("GTK-based desktop? " + r.verdict());
        System.out.println("Reason: " + r.reason());

        // If you just need a boolean:
        boolean isGtk = (r.verdict() == Verdict.YES);
        // Use isGtk as needed…
    }

    public enum Verdict {YES, NO, UNKNOWN}

    public record Result(Verdict verdict, String reason) {
    }
}
