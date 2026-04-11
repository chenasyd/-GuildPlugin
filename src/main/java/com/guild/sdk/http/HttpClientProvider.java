package com.guild.sdk.http;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP 客户端工具提供者 - 为模块提供安全的 HTTP 请求能力
 * <p>
 * 封装了 HTTP 调用的常用模式，支持 GET/POST 方法，
 * 自动在异步线程执行，避免阻塞服务器主线程。
 * <p>
 * 错误通过 {@link CompletableFuture#failedFuture(Throwable)} 传播，
 * 调用方可使用 {@code .exceptionally()} 或 {@code .handle()} 处理异常。
 * <p>
 * 使用示例：
 * <pre>{@code
 * context.getApi().httpGet("https://api.example.com/data")
 *     .thenAccept(body -> context.runSync(() -> showResult(player, body)))
 *     .exceptionally(err -> {
 *         context.getLogger().warning("request failed: " + err.getMessage());
 *         return null;
 *     });
 * }</pre>
 */
public class HttpClientProvider {

    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 10000;

    private final int connectTimeout;
    private final int readTimeout;

    public HttpClientProvider() {
        this(DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
    }

    public HttpClientProvider(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * 发送 GET 请求（异步）
     *
     * @param url     请求地址
     * @param headers 请求头（可为 null 或空）
     * @return CompletableFuture 包含响应体文本，失败时通过 exceptionally 传播异常
     */
    public CompletableFuture<String> httpGet(String url, Map<String, String> headers) {
        return CompletableFuture.supplyAsync(() -> executeRequest("GET", url, null, headers));
    }

    /**
     * 发送 POST 请求（异步）
     *
     * @param url     请求地址
     * @param body    请求体文本（JSON 等）
     * @param headers 请求头（可为 null 或空）
     * @return CompletableFuture 包含响应体文本，失败时通过 exceptionally 传播异常
     */
    public CompletableFuture<String> httpPost(String url, String body,
                                              Map<String, String> headers) {
        return CompletableFuture.supplyAsync(() ->
                executeRequest("POST", url, body, headers));
    }

    /**
     * 执行实际的 HTTP 请求
     *
     * @throws RuntimeException 包装网络/IO 异常，由 CompletableFuture 传播
     */
    private String executeRequest(String method, String url, String body,
                                  Map<String, String> headers) {
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(connectTimeout);
            conn.setReadTimeout(readTimeout);
            conn.setDoInput(true);

            if ("POST".equals(method)) {
                conn.setDoOutput(true);
            }

            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "GuildPlugin-Module/1.0");

            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            if (body != null && !body.isEmpty()) {
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }

            int responseCode = conn.getResponseCode();
            java.io.InputStream inputStream =
                    responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();

            if (inputStream == null) {
                throw new RuntimeException("HTTP " + responseCode + ": empty response from " + url);
            }

            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(inputStream,
                            java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            conn.disconnect();

            if (responseCode >= 400) {
                throw new RuntimeException("HTTP " + responseCode + ": " + response);
            }

            return response.toString();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }
}
