package org.hkijena.jipipe.api.project;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;
import org.hkijena.jipipe.api.events.JIPipeEventEmitter;
import org.hkijena.jipipe.api.run.JIPipeProjectRunSet;
import org.hkijena.jipipe.plugins.parameters.library.graph.GraphNodeReferenceParameter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JIPipeProjectRunSetsConfiguration {
    private List<JIPipeProjectRunSet> runSets = new ArrayList<>();
    private final RunSetsModifiedEventEmitter modifiedEventEmitter = new RunSetsModifiedEventEmitter();
    private final Set<String> uuidCache = new HashSet<>();

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
                if(node.isSet()) {
                    uuidCache.add(node.getNodeUUID());
                }
            }
        }
    }

    public Set<String> getUuidCache() {
        return uuidCache;
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

    public interface RunSetsModifiedEventListener {
        void onRunSetsModified(RunSetsModifiedEvent event);
    }

    public static class RunSetsModifiedEventEmitter extends JIPipeEventEmitter<RunSetsModifiedEvent, RunSetsModifiedEventListener> {

        @Override
        protected void call(RunSetsModifiedEventListener runSetsModifiedEventListener, RunSetsModifiedEvent event) {
            runSetsModifiedEventListener.onRunSetsModified(event);
        }
    }
}
