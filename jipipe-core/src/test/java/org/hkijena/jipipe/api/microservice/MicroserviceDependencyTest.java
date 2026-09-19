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

package org.hkijena.jipipe.api.microservice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MicroserviceDependencyTest {

    static class TestService extends AbstractMicroservice {
        volatile boolean startCalled = false;
        boolean throwOnStart = false;

        TestService(String name) {
            super(name);
        }

        @Override
        protected void onStart() throws Exception {
            startCalled = true;
            if (throwOnStart) throw new RuntimeException("dep failed");
        }
    }

    @Test
    void startStartsDependenciesFirst() {
        TestService dep = new TestService("Dependency");
        TestService main = new TestService("Main");
        main.addDependency(dep);

        main.start();

        assertEquals(MicroserviceState.Ready, dep.getState());
        assertEquals(MicroserviceState.Ready, main.getState());
    }

    @Test
    void dependencyFailureFailsDependent() {
        TestService dep = new TestService("Dependency");
        dep.throwOnStart = true;
        TestService main = new TestService("Main");
        main.addDependency(dep);

        main.start();

        assertEquals(MicroserviceState.Failed, dep.getState());
        assertEquals(MicroserviceState.Failed, main.getState());
        assertTrue(main.getStateDetail().contains("Dependency"));
    }

    @Test
    void readyDependencyIsNotRestarted() {
        TestService dep = new TestService("Dependency");
        dep.start();
        assertTrue(dep.startCalled);

        dep.startCalled = false;
        TestService main = new TestService("Main");
        main.addDependency(dep);

        main.start();

        assertFalse(dep.startCalled);
        assertEquals(MicroserviceState.Ready, main.getState());
    }

    @Test
    void stopDoesNotStopDependencies() {
        TestService dep = new TestService("Dependency");
        TestService main = new TestService("Main");
        main.addDependency(dep);

        main.start();
        main.stop();

        assertEquals(MicroserviceState.Stopped, main.getState());
        assertEquals(MicroserviceState.Ready, dep.getState());
    }

    @Test
    void circularDependencyRejected() {
        TestService a = new TestService("A");
        TestService b = new TestService("B");

        a.addDependency(b);
        assertThrows(IllegalArgumentException.class, () -> b.addDependency(a));
    }

    @Test
    void transitiveDependencyStarted() {
        TestService a = new TestService("A");
        TestService b = new TestService("B");
        TestService c = new TestService("C");

        b.addDependency(a);  // B depends on A
        c.addDependency(b);  // C depends on B

        c.start();

        assertEquals(MicroserviceState.Ready, a.getState());
        assertEquals(MicroserviceState.Ready, b.getState());
        assertEquals(MicroserviceState.Ready, c.getState());
    }

    @Test
    void transitiveCircularDependencyRejected() {
        TestService a = new TestService("A");
        TestService b = new TestService("B");
        TestService c = new TestService("C");

        b.addDependency(a);  // B depends on A
        c.addDependency(b);  // C depends on B
        assertThrows(IllegalArgumentException.class, () -> a.addDependency(c));
    }
}
