package com.guild.sdk.http;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HttpClientProvider {
    public HttpClientProvider() {
    }

    public HttpClientProvider(int connectTimeout, int readTimeout) {
    }

    public CompletableFuture<String> httpGet(String url, Map<String, String> headers) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Compile-time SDK stub"));
    }

    public CompletableFuture<String> httpPost(String url, String body, Map<String, String> headers) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException("Compile-time SDK stub"));
    }
}
