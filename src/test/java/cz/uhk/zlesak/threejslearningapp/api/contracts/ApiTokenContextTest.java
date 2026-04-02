package cz.uhk.zlesak.threejslearningapp.api.contracts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiTokenContextTest {

    @AfterEach
    void tearDown() {
        ApiTokenContext.clear();
    }

    @Test
    void get_shouldReturnNullWhenNothingSet() {
        assertNull(ApiTokenContext.get());
    }

    @Test
    void set_shouldStoreTokenReturnedByGet() {
        ApiTokenContext.set("my-token");

        assertEquals("my-token", ApiTokenContext.get());
    }

    @Test
    void clear_shouldRemoveStoredToken() {
        ApiTokenContext.set("token-to-remove");

        ApiTokenContext.clear();

        assertNull(ApiTokenContext.get());
    }

    @Test
    void set_shouldOverwritePreviousValue() {
        ApiTokenContext.set("first");
        ApiTokenContext.set("second");

        assertEquals("second", ApiTokenContext.get());
    }

    @Test
    void threadIsolation_tokenSetInMainThreadShouldNotBeVisibleInOtherThread() throws InterruptedException {
        ApiTokenContext.set("main-thread-token");
        AtomicReference<String> otherThreadValue = new AtomicReference<>();

        Thread thread = new Thread(() -> otherThreadValue.set(ApiTokenContext.get()));
        thread.start();
        thread.join();

        assertNull(otherThreadValue.get(), "Token from main thread must not leak into other threads");
        assertEquals("main-thread-token", ApiTokenContext.get(), "Token in main thread must still be present");
    }

    @Test
    void threadIsolation_tokenSetInOtherThreadShouldNotAffectMainThread() throws InterruptedException {
        AtomicReference<String> otherThreadValue = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            ApiTokenContext.set("other-thread-token");
            otherThreadValue.set(ApiTokenContext.get());
        });
        thread.start();
        thread.join();

        assertEquals("other-thread-token", otherThreadValue.get());
        assertNull(ApiTokenContext.get(), "Main thread should still see null after other thread set its own token");
    }
}
