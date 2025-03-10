package org.hkijena.jipipe.api.project;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.run.JIPipeProjectRunSet;
import org.hkijena.jipipe.plugins.parameters.library.graph.GraphNodeReferenceParameter;

import java.util.*;

public class JIPipeProjectRunSetsConfiguration {
    private final RunSetsModifiedEventEmitter modifiedEventEmitter = new RunSetsModifiedEventEmitter();
    private final Set<String> uuidCache = new HashSet<>();
    private List<JIPipeProjectRunSet> runSets = new ArrayList<>();

    @JsonGetter("run-sets")
    public List<JIPipeProjectRunSet> getRunSets() {
        return runSets;
    }

    @JsonSetter("run-sets")
    public void setRunSets(List<JIPipeProjectRunSet> runSets) {
        this.runSets = runSets;
        refreshUUIDCache();
        modifiedEventEmitter.emit(new RunSetsModifiedEvent(this));
    }

    public void add(JIPipeProjectRunSet runSet) {
        runSets.add(runSet);
        refreshUUIDCache();
        modifiedEventEmitter.emit(new RunSetsModifiedEvent(this));
    }

    public void remove(JIPipeProjectRunSet runSet) {
        runSets.remove(runSet);
        refreshUUIDCache();
        modifiedEventEmitter.emit(new RunSetsModifiedEvent(this));
    }

    public RunSetsModifiedEventEmitter getModifiedEventEmitter() {
        return modifiedEventEmitter;
    }

    public void refreshUUIDCache() {
        uuidCache.clear();
        for (JIPipeProjectRunSet runSet : runSets) {
            for (GraphNodeReferenceParameter node : runSet.getNodes()) {
                if (node.isSet()) {
                    uuidCache.add(node.getNodeUUID());
                }
            }
        }
    }

    public Set<String> getUuidCache() {
        return uuidCache;
    }

    public void sortUp(JIPipeProjectRunSet value) {
        int index = runSets.indexOf(value);
        if (index > 0) {
            Collections.swap(runSets, index, index - 1);
            modifiedEventEmitter.emit(new RunSetsModifiedEvent(this));
        }
    }

    public void sortDown(JIPipeProjectRunSet value) {
        int index = runSets.indexOf(value);
        if (index >= 0 && index < runSets.size() - 1) {
            Collections.swap(runSets, index, index + 1);
            modifiedEventEmitter.emit(new RunSetsModifiedEvent(this));
        }
    }

    public interface RunSetsModifiedEventListener {
        void onRunSetsModified(RunSetsModifiedEvent event);
    }

    public static class RunSetsModifiedEvent extends AbstractJIPipeEvent {
        private final JIPipeProjectRunSetsConfiguration runSetsConfiguration;

        public RunSetsModifiedEvent(JIPipeProjectRunSetsConfiguration runSetsConfiguration) {
            super(runSetsConfiguration);
            this.runSetsConfiguration = runSetsConfiguration;
        }

        public JIPipeProjectRunSetsConfiguration getRunSetsConfiguration() {
            return runSetsConfiguration;
        }
    }

    public static class RunSetsModifiedEventEmitter extends JIPipeEventEmitter<RunSetsModifiedEvent, RunSetsModifiedEventListener> {

        @Override
        protected void call(RunSetsModifiedEventListener runSetsModifiedEventListener, RunSetsModifiedEvent event) {
            runSetsModifiedEventListener.onRunSetsModified(event);
        }
    }
}
