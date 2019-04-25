package org.jenkinsci.plugins.github_branch_source;


import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;

import org.kohsuke.github.HttpConnector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;


/**
 * {@link HttpConnector} for {@link OkHttpClient}.
 *
 * Unlike {@link #DEFAULT}, OkHttp does response caching.
 * Making a conditional request against GitHubAPI and receiving a 304
 * response does not count against the rate limit.
 * See http://developer.github.com/v3/#conditional-requests
 *
 * @author Roberto Tyley
 * @author Kohsuke Kawaguchi
 */
public class OkHttp3Connector implements HttpConnector {
    private final OkUrlFactory urlFactory;

    public OkHttp3Connector(OkUrlFactory urlFactory) {
        OkHttpClient.Builder builder = urlFactory.client().newBuilder();
        builder.sslSocketFactory(TlsSocketFactory());
        builder.connectionSpecs(TlsConnectionSpecs());
        urlFactory.setClient(builder.build());

        this.urlFactory = urlFactory;
    }

    public HttpURLConnection connect(URL url) throws IOException {
        return urlFactory.open(url);
    }

    /** Returns TLSv1.2 only SSL Socket Factory. */
    private SSLSocketFactory TlsSocketFactory() {
        SSLContext sc;
        try {
            sc = SSLContext.getInstance("TLSv1.2");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        try {
            sc.init(null, null, null);
            return sc.getSocketFactory();
        } catch (KeyManagementException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /** Returns connection spec with TLS v1.2 in it */
    private List<ConnectionSpec> TlsConnectionSpecs() {
        return Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT);
    }

}
