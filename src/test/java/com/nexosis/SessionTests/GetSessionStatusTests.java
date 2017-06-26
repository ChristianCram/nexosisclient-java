package com.nexosis.SessionTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.nexosis.impl.ApiConnection;
import com.nexosis.impl.HttpClientFactory;
import com.nexosis.impl.NexosisClient;
import com.nexosis.impl.NexosisClientException;
import com.nexosis.model.SessionResponse;
import com.nexosis.model.SessionResultStatus;
import com.nexosis.model.SessionStatus;
import com.nexosis.util.Action;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.UUID;

import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {ApiConnection.class })
public class GetSessionStatusTests {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private HttpClientFactory httpClientFactory;
    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse httpResponse;
    @Mock
    private HttpEntity httpEntity;
    @Mock
    private StatusLine statusLine;

    private NexosisClient target;
    private String fakeEndpoint = "https://nada.nexosis.com/not-here";
    private String fakeApiKey = "abcdefg";
    private URI apiFakeEndpointUri;
    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        target = new NexosisClient(fakeApiKey, fakeEndpoint, httpClientFactory);
        apiFakeEndpointUri = new URI(fakeEndpoint);
        mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        PowerMockito.when(httpClientFactory.createClient()).thenReturn(httpClient);
        PowerMockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
    }

    @Test
    public void statusHeaderIsAssignedToResult() throws Exception {
        UUID sessionId = UUID.randomUUID();
        HttpHead head = new HttpHead();

        SessionResultStatus sessionStatus = new SessionResultStatus();
        sessionStatus.setSessionId(sessionId);
        sessionStatus.setStatus(SessionStatus.STARTED);
        HttpEntity entity = new StringEntity(mapper.writeValueAsString(sessionStatus));

        PowerMockito.when(statusLine.getStatusCode()).thenReturn(200);
        PowerMockito.whenNew(HttpHead.class).withNoArguments().thenReturn(head);
        PowerMockito.when(httpClient.execute(any(HttpHead.class))).thenReturn(httpResponse);
        PowerMockito.when(httpResponse.getEntity()).thenReturn(entity);

        SessionResultStatus result = target.getSessions().getStatus(sessionId);

        Assert.assertEquals(sessionId, result.getSessionId());
        Assert.assertEquals(SessionStatus.STARTED, result.getStatus());
    }

    @Test
    public void httpTransformerIsWrappedAndCalled() throws Exception {
        UUID sessionId = UUID.randomUUID();
        HttpHead head = new HttpHead();
        SessionResultStatus sessionStatus = new SessionResultStatus();
        sessionStatus.setSessionId(sessionId);
        sessionStatus.setStatus(SessionStatus.STARTED);
        HttpEntity entity = new StringEntity(mapper.writeValueAsString(sessionStatus));

        PowerMockito.when(statusLine.getStatusCode()).thenReturn(200);
        PowerMockito.whenNew(HttpHead.class).withNoArguments().thenReturn(head);
        PowerMockito.when(httpClient.execute(any(HttpHead.class))).thenReturn(httpResponse);
        PowerMockito.when(httpResponse.getEntity()).thenReturn(entity);

        TestIfCalled isCalled = new TestIfCalled(false);
        target.getSessions().getStatus(sessionId, isCalled);

        Assert.assertTrue("Http transform function not called", isCalled.called);
    }

    class TestIfCalled implements Action<HttpRequest, HttpResponse> {
        private boolean called = false;

        TestIfCalled(boolean called) {
            this.called = called;
        }

        boolean getCalled() {
            return called;
        }

        @Override
        public void invoke(HttpRequest target1, HttpResponse target2) throws Exception {
            called = true;
        }
    }
}
