package org.jenkinsci.plugins.github_branch_source;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.OkUrlFactory;

import org.kohsuke.github.HttpConnector;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.Arrays;
import java.util.List;


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
    private final OkHttpClient client;
    private final OkUrlFactory urlFactory;

    public OkHttp3Connector(OkHttpClient client) {

        OkHttpClient.Builder builder = client.newBuilder();

        builder.connectionSpecs(TlsConnectionSpecs());
        this.client = builder.build();
        this.urlFactory = new OkUrlFactory(this.client);
    }

    public HttpURLConnection connect(URL url) throws IOException {
        return urlFactory.open(url);
    }

    /** Returns connection spec with TLS v1.2 in it */
    private List<ConnectionSpec> TlsConnectionSpecs() {
        return Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.CLEARTEXT);
    }

}