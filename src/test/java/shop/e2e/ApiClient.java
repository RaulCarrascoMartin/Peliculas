package shop.e2e;

import com.jayway.jsonpath.JsonPath;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/** Minimal client over the real HTTP server of an e2e test: one request in, status and JSON body out. */
final class ApiClient {

    /** Status and raw body of a response, with a JsonPath reader. */
    record Reply(int status, String body) {
        <T> T json(String path) {
            return JsonPath.read(body, path);
        }
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;

    ApiClient(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    Reply get(String path, String token) {
        return send("GET", path, null, token);
    }

    Reply post(String path, String json, String token) {
        return send("POST", path, json, token);
    }

    Reply put(String path, String json, String token) {
        return send("PUT", path, json, token);
    }

    Reply delete(String path, String token) {
        return send("DELETE", path, null, token);
    }

    private Reply send(String method, String path, String json, String token) {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Reply(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
