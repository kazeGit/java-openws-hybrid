/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensaml.util.resource;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.utils.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.xml.util.DatatypeHelper;

/**
 * A resource representing a file retrieved from a URL using Apache Commons HTTPClient.
 */
public class HttpResource extends AbstractFilteredResource {
    
    /** HttpClient connection timeout in milliseconds. */
    private static final int CONNECTION_TIMEOUT = 90*1000;
    
    /** HttpClient socket timeout in milliseconds. */
    private static final int SOCKET_TIMEOUT = 90*1000;

    /** HTTP URL of the resource. */
    protected String resourceUrl;

    /** HTTP client. */
    private HttpClient httpClient;

    /**
     * Constructor.
     * 
     * @param resource HTTP(S) URL of the resource
     */
    public HttpResource(String resource) {
        super();

        resourceUrl = DatatypeHelper.safeTrimOrNullString(resource);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource URL may not be null or empty");
        }

        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .build();

        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    /**
     * Constructor.
     * 
     * @param resource HTTP(S) URL of the resource
     * @param resourceFilter filter to apply to this resource
     * 
     * @deprecated use {@link #setResourceFilter(ResourceFilter)} instead
     */
    public HttpResource(String resource, ResourceFilter resourceFilter) {
        super(resourceFilter);

        resourceUrl = DatatypeHelper.safeTrimOrNullString(resource);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource URL may not be null or empty");
        }

        RequestConfig requestConfig = RequestConfig.copy(RequestConfig.DEFAULT)
            .setSocketTimeout(SOCKET_TIMEOUT)
            .setConnectTimeout(CONNECTION_TIMEOUT)
            .build();

        httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig).
            build();
    }

    /** {@inheritDoc} */
    public boolean exists() throws ResourceException {
        HttpHead httpHead = new HttpHead(resourceUrl);
        httpHead.addHeader("Connection", "close");

        try {
            final HttpResponse response = httpClient.execute(httpHead);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return false;
            }

            return true;
        } catch (IOException e) {
            throw new ResourceException("Unable to contact resource URL: " + resourceUrl, e);
        } finally {
            httpHead.releaseConnection();
        }
    }

    /** {@inheritDoc} */
    public InputStream getInputStream() throws ResourceException {
        HttpGet getMethod = new HttpGet(resourceUrl);
        final HttpResponse response = getResource(getMethod);
        try {
            return new ConnectionClosingInputStream(getMethod, response, applyFilter(response.getEntity().getContent()));
        } catch (IOException e) {
            throw new ResourceException("Unable to read response", e);
        }
    }

    /** {@inheritDoc} */
    public DateTime getLastModifiedTime() throws ResourceException {
        HttpHead headMethod = new HttpHead(resourceUrl);
        headMethod.addHeader("Connection", "close");

        try {
            final HttpResponse response = httpClient.execute(headMethod);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new ResourceException("Unable to retrieve resource URL " + resourceUrl
                        + ", received HTTP status code " + response.getStatusLine().getStatusCode());
            }
            Header lastModifiedHeader = headMethod.getLastHeader("Last-Modified");
            if (lastModifiedHeader != null && !DatatypeHelper.isEmpty(lastModifiedHeader.getValue())) {
                long lastModifiedTime = DateUtils.parseDate(lastModifiedHeader.getValue()).getTime();
                return new DateTime(lastModifiedTime, ISOChronology.getInstanceUTC());
            }

            return new DateTime();
        } catch (IOException e) {
            throw new ResourceException("Unable to contact resource URL: " + resourceUrl, e);
        } catch (NullPointerException e) {
            throw new ResourceException("Unable to parse last modified date for resource:" + resourceUrl, e);
        } finally {
            headMethod.releaseConnection();
        }
    }

    /** {@inheritDoc} */
    public String getLocation() {
        return resourceUrl;
    }

    /** {@inheritDoc} */
    public String toString() {
        return getLocation();
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return getLocation().hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o instanceof HttpResource) {
            return getLocation().equals(((HttpResource) o).getLocation());
        }

        return false;
    }

    /**
     * Gets remote resource.
     * 
     * @return the remove resource
     * 
     * @throws ResourceException thrown if the resource could not be fetched
     */
    protected HttpResponse getResource(HttpRequestBase httpRequestBase) throws ResourceException {
        httpRequestBase.addHeader("Connection", "close");

        try {
            final HttpResponse response = httpClient.execute(httpRequestBase);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new ResourceException("Unable to retrieve resource URL " + resourceUrl
                        + ", received HTTP status code " + response.getStatusLine().getStatusCode());
            }
            return response;
        } catch (IOException e) {
            throw new ResourceException("Unable to contact resource URL: " + resourceUrl, e);
        }
    }

    /**
     * A wrapper around the {@link InputStream} returned by a {@link HttpRequestBase} that closes the stream and releases the
     * HTTP connection when {@link #close()} is invoked.
     */
    private static class ConnectionClosingInputStream extends InputStream {

        /** HTTP method that was invoked. */
        private final HttpResponse response;

        /** Stream owned by the given HTTP method. */
        private final InputStream stream;

        private final HttpRequestBase request;

        /**
         * Constructor.
         *
         * @param response HTTP method that was invoked
         * @param returnedStream stream owned by the given HTTP method
         */
        public ConnectionClosingInputStream(HttpRequestBase requestBase, HttpResponse response, InputStream returnedStream) {
            this.response = response;
            stream = returnedStream;
            this.request = requestBase;
        }

        /** {@inheritDoc} */
        public int available() throws IOException {
            return stream.available();
        }

        /** {@inheritDoc} */
        public void close() throws IOException {
            stream.close();
            request.releaseConnection();
        }

        /** {@inheritDoc} */
        public void mark(int readLimit) {
            stream.mark(readLimit);
        }

        /** {@inheritDoc} */
        public boolean markSupported() {
            return stream.markSupported();
        }

        /** {@inheritDoc} */
        public int read() throws IOException {
            return stream.read();
        }

        /** {@inheritDoc} */
        public int read(byte[] b) throws IOException {
            return stream.read(b);
        }

        /** {@inheritDoc} */
        public int read(byte[] b, int off, int len) throws IOException {
            return stream.read(b, off, len);
        }

        /** {@inheritDoc} */
        public synchronized void reset() throws IOException {
            stream.reset();
        }

        /** {@inheritDoc} */
        public long skip(long n) throws IOException {
            return stream.skip(n);
        }
    }
}