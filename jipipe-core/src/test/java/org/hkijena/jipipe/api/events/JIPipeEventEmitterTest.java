package org.hkijena.jipipe.api.events;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JIPipeEventEmitterTest {

    private static class TestEvent extends AbstractJIPipeEvent {
        private final String message;

        TestEvent(String message) {
            super(null);
            this.message = message;
        }

        String getMessage() {
            return message;
        }
    }

    private interface TestEventListener {
        void onTestEvent(TestEvent event);
    }

    private static class TestEventEmitter extends JIPipeEventEmitter<TestEvent, TestEventListener> {
        @Override
        protected void call(TestEventListener listener, TestEvent event) {
            listener.onTestEvent(event);
        }
    }

    private static class RecordingListener implements TestEventListener {
        final List<String> messages = new ArrayList<>();

        @Override
        public void onTestEvent(TestEvent event) {
            messages.add(event.getMessage());
        }
    }

    @Test
    void subscribeAndEmit_deliversToListener() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(listener.messages).containsExactly("hello");
    }

    @Test
    void unsubscribe_removesStrongSubscriber() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribe(listener);
        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(listener.messages).isEmpty();
    }

    @Test
    void unsubscribe_removesWeakSubscriber() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribeWeak(listener);
        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(listener.messages).isEmpty();
    }

    @Test
    void unsubscribe_withoutSubscribe_isNoOp() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(listener.messages).isEmpty();
    }

    @Test
    void unsubscribe_withWrongListener_doesNothing() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listenerA = new RecordingListener();
        RecordingListener listenerB = new RecordingListener();

        emitter.subscribe(listenerA);
        emitter.unsubscribe(listenerB);
        emitter.emit(new TestEvent("hello"));

        assertThat(listenerA.messages).containsExactly("hello");
        assertThat(listenerB.messages).isEmpty();
    }

    @Test
    void unsubscribe_doesNotAffectOtherListeners() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listenerA = new RecordingListener();
        RecordingListener listenerB = new RecordingListener();

        emitter.subscribe(listenerA);
        emitter.subscribe(listenerB);
        emitter.unsubscribe(listenerA);
        emitter.emit(new TestEvent("hello"));

        assertThat(listenerA.messages).isEmpty();
        assertThat(listenerB.messages).containsExactly("hello");
    }

    @Test
    void unsubscribeAll_removesAllMatchingSubscribers() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribe(listener);
        emitter.subscribe(listener);
        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(listener.messages).isEmpty();
    }

    @Test
    void subscribeLambda_isNotAffectedByUnsubscribe() {
        TestEventEmitter emitter = new TestEventEmitter();
        List<String> received = new ArrayList<>();
        RecordingListener listener = new RecordingListener();

        emitter.subscribeLambda((e, event) -> received.add(event.getMessage()));
        emitter.subscribe(listener);
        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("hello"));

        assertThat(received).containsExactly("hello");
        assertThat(listener.messages).isEmpty();
    }

    @Test
    void emit_afterUnsubscribe_doesNotCallUnsubscribedListener() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribe(listener);
        emitter.emit(new TestEvent("first"));
        emitter.unsubscribe(listener);
        emitter.emit(new TestEvent("second"));

        assertThat(listener.messages).containsExactly("first");
    }

    @Test
    void dispose_clearsAllSubscribers() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribe(listener);
        emitter.dispose();

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> emitter.emit(new TestEvent("hello"))
        );
    }

    @Test
    void gc_removesDeadWeakSubscribers() {
        TestEventEmitter emitter = new TestEventEmitter();
        RecordingListener listener = new RecordingListener();

        emitter.subscribeWeak(listener);
        listener = null;
        System.gc();
        emitter.gc();
        emitter.emit(new TestEvent("hello"));
        // Should not throw or fail
    }
}
