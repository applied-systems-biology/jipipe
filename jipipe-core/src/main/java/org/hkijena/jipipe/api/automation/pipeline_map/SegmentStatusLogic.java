package org.hkijena.jipipe.api.automation.pipeline_map;

public final class SegmentStatusLogic {

    private SegmentStatusLogic() {
    }

    public static SegmentStatus compute(int errorCount, int warningCount, int cachedNodes, int totalNodes) {
        if (errorCount > 0) {
            return SegmentStatus.ERROR;
        }
        if (cachedNodes == totalNodes && totalNodes > 0 && warningCount == 0) {
            return SegmentStatus.OK;
        }
        if (cachedNodes > 0 || warningCount > 0) {
            return SegmentStatus.WARNING;
        }
        return SegmentStatus.UNKNOWN;
    }
}
