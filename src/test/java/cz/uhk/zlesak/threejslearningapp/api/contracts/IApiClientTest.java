package cz.uhk.zlesak.threejslearningapp.api.contracts;

import cz.uhk.zlesak.threejslearningapp.common.SpringContextUtils;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.security.AccessTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IApiClientTest {

    /** Minimal concrete implementation used only to exercise default/static interface methods. */
    static class MinimalClient implements IApiClient<Object, Object, Object> {
        @Override public Object create(Object entity) { return null; }
        @Override public Object read(String id) { return null; }
        @Override public Object readQuick(String id) { return null; }
        @Override public PageResult<Object> readEntities(FilterParameters<Object> pageRequest) { return null; }
        @Override public Object update(String id, Object entity) { return null; }
        @Override public boolean delete(String id) { return false; }
    }

    @AfterEach
    void tearDown() {
        ApiTokenContext.clear();
    }

    @Test
    void getBaseUrl_shouldEndWithApiSlashWhenBackendUrlNotSet() {
        String url = IApiClient.getBaseUrl();
        assertTrue(url.endsWith("/api/"), "Expected URL ending with /api/ but was: " + url);
        assertFalse(url.isBlank());
    }

    @Test
    void getExternalAppUrl_shouldReturnNonBlankUrl() {
        String url = IApiClient.getExternalAppUrl();
        assertNotNull(url);
        assertFalse(url.isBlank());
    }

    @Test
    void getJwtToken_shouldReturnAsyncTokenWhenApiTokenContextHasValue() {
        ApiTokenContext.set("async-token-xyz");

        String token = new MinimalClient().getJwtToken();

        assertEquals("async-token-xyz", token);
    }

    @Test
    void getJwtToken_shouldFallBackToSpringContextWhenNoAsyncToken() {
        ApiTokenContext.clear();
        AccessTokenProvider provider = mock(AccessTokenProvider.class);
        when(provider.getValidAccessToken()).thenReturn("spring-token-abc");
        registerBean(AccessTokenProvider.class, provider);

        String token = new MinimalClient().getJwtToken();

        assertEquals("spring-token-abc", token);
    }

    @Test
    void getJwtToken_shouldReturnNullWhenSpringContextThrowsRuntimeException() {
        ApiTokenContext.clear();
        AccessTokenProvider provider = mock(AccessTokenProvider.class);
        when(provider.getValidAccessToken()).thenThrow(new RuntimeException("no token"));
        registerBean(AccessTokenProvider.class, provider);

        String token = new MinimalClient().getJwtToken();

        assertNull(token);
    }

    @Test
    void getJwtToken_shouldReturnNullWhenAsyncTokenIsBlank() {
        ApiTokenContext.set("   ");
        AccessTokenProvider provider = mock(AccessTokenProvider.class);
        when(provider.getValidAccessToken()).thenReturn(null);
        registerBean(AccessTokenProvider.class, provider);

        String token = new MinimalClient().getJwtToken();

        assertNull(token);
    }

    private <T> void registerBean(Class<T> type, T bean) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean(type, () -> bean);
        context.refresh();
        SpringContextUtils.setContext(context);
    }
}
