package org.hkijena.jipipe.plugins.statistics;

public enum StatisticsPrivacyLevel {
    None(0, "No statistics collected or sent"),
    Installation(1, "Machine ID and JIPipe version (sent once)"),
    ActiveInstallation(2, "Daily ping to prove installation is still active"),
    RoughProjects(3, "Project count, workflow runs, RO-Crates created"),
    Everything(4, "All available statistics");

    private final int level;
    private final String description;

    StatisticsPrivacyLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + " - " + description;
    }
}
