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

package org.opensaml.ws.soap.client.http;

import net.jcip.annotations.NotThreadSafe;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.opensaml.xml.util.DatatypeHelper;

import java.nio.charset.Charset;

/**
 * A builder for {@link CloseableHttpClient}s.
 * 
 * This builder will produce clients that employ the {@link PoolingHttpClientConnectionManager} and as such users of the
 * clients MUST be sure to invoke {@link org.apache.commons.httpclient.HttpMethod#releaseConnection()} after they have
 * finished with the method.
 */
@NotThreadSafe
public class HttpClientBuilder {

    /** Host name of the HTTP proxy server through which connections will be made. */
    private String proxyHost;

    /** Port number of the HTTP proxy server through which connections will be made. */
    private int proxyPort;

    /** Username used to connect to the HTTP proxy server. */
    private String proxyUsername;

    /** Password used to connect to the HTTP proxy server. */
    private String proxyPassword;

    /** Whether authentication should be performed preemptively, defaults to false. */
    private boolean preemptiveAuthentication;

    /** Character set used for HTTP content, defaults to UTF-8. */
    private String contentCharSet;

    /** Amount of time, in milliseconds, to wait for a connection to be established, default is 5,000. */
    private int connectionTimeout;

    /** Size of the buffer, in bytes, used to hold outbound information, defaults to 4,096. */
    private int sendBufferSize;

    /** Size of the buffer, in bytes, used to hold inbound information, defaults to 16,384. */
    private int receiveBufferSize;

    /** Whether to use TCP No Delay algorithm, defaults to true. */
    private boolean tcpNoDelay;

    /** Total number of connections allowed to a specific host, defaults to 5. */
    private int maxConnectionsPerHost;

    /** Total number of connections allowed to all hosts, defaults to 20. */
    private int maxTotalConnectons;

    /** Number of times a failed connection to a host should be retried. */
    private int connectionRetryAttempts;
    
    /** Amount of time, in milliseconds, to wait for data to be read from a socket, defaults to 90000. */
    private int socketTimeout;

    /** Socket factory used for the 'https' scheme. */
    private SSLConnectionSocketFactory socketFactory;

    /** Constructor. */
    public HttpClientBuilder() {
        resetDefaults();
    }

    /** Resets the builder to its default values. */
    public void resetDefaults() {
        proxyPort = -1;
        preemptiveAuthentication = false;
        contentCharSet = "UTF-8";
        connectionTimeout = 5000;
        sendBufferSize = 4096;
        receiveBufferSize = 16384;
        tcpNoDelay = true;
        maxConnectionsPerHost = 5;
        maxTotalConnectons = 20;
        connectionRetryAttempts = 0;
        socketTimeout = 90*1000;
    }

    /**
     * Builds an HTTP client with the given settings. Settings are NOT reset to their default values after a client has
     * been created.
     * 
     * @return the created client.
     */
    public CloseableHttpClient buildClient() {
        //clientParams.setAuthenticationPreemptive(isPreemptiveAuthentication()); ? redundant

        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager();
        connMgr.setMaxTotal(getMaxTotalConnections());
        connMgr.setDefaultMaxPerRoute(getMaxConnectionsPerHost());
        connMgr.setDefaultConnectionConfig(ConnectionConfig.custom()
                .setCharset(Charset.forName(getContentCharSet()))
                .build());
        connMgr.setDefaultSocketConfig(SocketConfig.custom()
                .setTcpNoDelay(isTcpNoDelay())
                .setSoTimeout(socketTimeout)
                .setSndBufSize(getSendBufferSize())
                .setRcvBufSize(getReceiveBufferSize())
                .build());

        final org.apache.http.impl.client.HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setConnectionManager(connMgr)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(connectionRetryAttempts, false))
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(getConnectionTimeout())
                        .build());

        if (proxyHost != null) {
            httpClientBuilder.setProxy(new HttpHost(proxyHost, proxyPort));

            if (proxyUsername != null) {
                BasicCredentialsProvider provider = new BasicCredentialsProvider();
                provider.setCredentials(AuthScope.ANY, new org.apache.http.auth.UsernamePasswordCredentials(proxyUsername,
                        proxyPassword));
                httpClientBuilder.setDefaultCredentialsProvider(provider);
            }
        }

        if (this.socketFactory != null) {
            httpClientBuilder.setSSLSocketFactory(socketFactory);
        }
//        if (httpsProtocolSocketFactory != null) {
//            Protocol.registerProtocol("https", new Protocol("https", httpsProtocolSocketFactory, 443));
//        }


        final CloseableHttpClient client = httpClientBuilder.build();


        return client;
    }

    /**
     * Gets the host name of the HTTP proxy server through which connections will be made.
     * 
     * @return host name of the HTTP proxy server through which connections will be made
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * Sets the host name of the HTTP proxy server through which connections will be made.
     * 
     * @param host host name of the HTTP proxy server through which connections will be made
     */
    public void setProxyHost(String host) {
        proxyHost = DatatypeHelper.safeTrimOrNullString(host);
    }

    /**
     * Gets the port of the HTTP proxy server through which connections will be made.
     * 
     * @return port of the HTTP proxy server through which connections will be made
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Sets the port of the HTTP proxy server through which connections will be made.
     * 
     * @param port port of the HTTP proxy server through which connections will be made
     */
    public void setProxyPort(int port) {
        proxyPort = port;
    }

    /**
     * Gets the username used to connect to the HTTP proxy server.
     * 
     * @return username used to connect to the HTTP proxy server
     */
    public String getProxyUsername() {
        return proxyUsername;
    }

    /**
     * Sets the username used to connect to the HTTP proxy server.
     * 
     * @param username username used to connect to the HTTP proxy server
     */
    public void setProxyUsername(String username) {
        proxyUsername = DatatypeHelper.safeTrimOrNullString(username);
    }

    /**
     * Gets the password used to connect to the HTTP proxy server.
     * 
     * @return password used to connect to the HTTP proxy server
     */
    public String getProxyPassword() {
        return proxyPassword;
    }

    /**
     * Sets the password used to connect to the HTTP proxy server.
     * 
     * @param password password used to connect to the HTTP proxy server
     */
    public void setProxyPassword(String password) {
        proxyPassword = DatatypeHelper.safeTrimOrNullString(password);
    }

    /**
     * Gets whether authentication is performed preemptively. Default value is <code>false</code>.
     * 
     * @return whether authentication is performed preemptively
     */
    public boolean isPreemptiveAuthentication() {
        return preemptiveAuthentication;
    }

    /**
     * Sets whether authentication is performed preemptively.
     * 
     * @param preemptive whether authentication is performed preemptively
     */
    public void setPreemptiveAuthentication(boolean preemptive) {
        preemptiveAuthentication = preemptive;
    }

    /**
     * Gets the character set used for HTTP content. Default value is <code>UTF-8</code>.
     * 
     * @return character set used for HTTP content
     */
    public String getContentCharSet() {
        return contentCharSet;
    }

    /**
     * Sets the character set used for HTTP content.
     * 
     * @param charSet character set used for HTTP content
     */
    public void setContentCharSet(String charSet) {
        contentCharSet = charSet;
    }

    /**
     * Gets the time, in milliseconds, to wait for connection establishments. Default value is 5,000. A value of 0
     * indicates there is no timeout.
     * 
     * @return time, in milliseconds, to wait for connection establishments
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Sets the time, in milliseconds, to wait for connection establishments. A value of 0 indicates there is no
     * timeout.
     * 
     * @param timeout time, in milliseconds, to wait for connection establishments
     */
    public void setConnectionTimeout(int timeout) {
        connectionTimeout = timeout;
    }

    /**
     * Gets the size of buffer, in bytes, used when sending content. Default value is 4,096.
     * 
     * @return size of buffer, in bytes, used when sending content
     */
    public int getSendBufferSize() {
        return sendBufferSize;
    }

    /**
     * Sets the size of buffer, in bytes, used when sending content.
     * 
     * @param size size of buffer, in bytes, used when sending content
     */
    public void setSendBufferSize(int size) {
        sendBufferSize = size;
    }

    /**
     * Gets the size of buffer, in bytes, used when receiving content. Default value is 16,384.
     * 
     * @return size of buffer, in bytes, used when sending content
     */
    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    /**
     * Sets the size of buffer, in bytes, used when sending content.
     * 
     * @param size size of buffer, in bytes, used when sending content
     */
    public void setReceiveBufferSize(int size) {
        receiveBufferSize = size;
    }

    /**
     * Gets whether to use TCP No Delay when sending data. Default value is <code>true</code>.
     * 
     * @return whether to use TCP No Delay when sending data
     */
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    /**
     * Sets whether to use TCP No Delay when sending data.
     * 
     * @param noDelay whether to use TCP No Delay when sending data
     */
    public void setTcpNoDelay(boolean noDelay) {
        tcpNoDelay = noDelay;
    }

    /**
     * Gets the maximum number of connections, per host, that the client will create. Default value is 5. A value of 0
     * indicates there is no maximum.
     * 
     * @return maximum number of connections, per host, that the client will create
     */
    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    /**
     * Sets the maximum number of connections, per host, that the client will create. A value of 0 indicates there is no
     * maximum.
     * 
     * @param max maximum number of connections, per host, that the client will create
     */
    public void setMaxConnectionsPerHost(int max) {
        maxConnectionsPerHost = max;
    }

    /**
     * Gets the maximum number of total connections the client will create. Default value is 20.
     * 
     * @return maximum number of total connections the client will create
     */
    public int getMaxTotalConnections() {
        return maxTotalConnectons;
    }

    /**
     * Sets the maximum number of total connections the client will create.
     * 
     * @param max maximum number of total connections the client will create, must be greater than zero
     */
    public void setMaxTotalConnections(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Maximum total number of connections must be greater than zero.");
        }
        maxTotalConnectons = max;
    }

    /**
     * Gets the number of times a connection will be tried if a host is unreachable.
     * 
     * @return number of times a connection will be tried if a host is unreachable
     */
    public int getConnectionRetryAttempts() {
        return connectionRetryAttempts;
    }

    /**
     * Sets the number of times a connection will be tried if a host is unreachable.
     * 
     * @param attempts number of times a connection will be tried if a host is unreachable
     */
    public void setConnectionRetryAttempts(int attempts) {
        connectionRetryAttempts = attempts;
    }

    /**
     * Gets the protocol socket factory used for the https scheme.
     * 
     * @return protocol socket factory used for the https scheme
     */
    public SSLConnectionSocketFactory getSSLConnectionSocketFactory() {
        return this.socketFactory;
    }

    /**
     * Sets the protocol socket factory used for the https scheme.
     * 
     * @param factory the SSLConnectionSocketFactory to set
     */
    public void setSSLConnectionSocketFactory(SSLConnectionSocketFactory factory) {
        this.socketFactory = factory;
    }
}