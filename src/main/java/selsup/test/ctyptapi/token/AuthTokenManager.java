package selsup.test.ctyptapi.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import selsup.test.ctyptapi.exception.ApiException;
import selsup.test.ctyptapi.json.AuthRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthTokenManager {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String token;
    private Instant tokenExpiry;
    private final ConcurrentHashMap<String, String> uuidDataCache = new ConcurrentHashMap<>();

    public AuthTokenManager(String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }


    public boolean isTokenValid() {
        return token != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry);
    }

    public String getToken() {
        return token;
    }

    public String refreshToken() throws Exception {
        // Шаг 1: Получение UUID и данных для подписи
        HttpRequest keyRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/cert/key"))
                .GET()
                .build();
        HttpResponse<String> keyResponse = httpClient.send(keyRequest, HttpResponse.BodyHandlers.ofString());
        if (keyResponse.statusCode() != 200) {
            throw new ApiException("Не удалось получить ключ аутентификации: статус " + keyResponse.statusCode());
        }

        var keyResponseMap = objectMapper.readValue(keyResponse.body(), Map.class);
        String uuid = (String) keyResponseMap.get("uuid");
        String data = (String) keyResponseMap.get("data");

        // Симуляция подписи данных УКЭП (в реальной системе использовать криптобиблиотеку)
        String signedData = Base64.getEncoder().encodeToString(("SIGNED_" + data).getBytes());

        // Шаг 2: Получение токена
        AuthRequest authRequestBody = new AuthRequest(uuid, signedData);
        String authJson = objectMapper.writeValueAsString(authRequestBody);
        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth/cert/"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(authJson))
                .build();

        HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() != 200) {
            throw new ApiException("Не удалось получить токен: статус " + tokenResponse.statusCode());
        }

        var tokenResponseMap = objectMapper.readValue(tokenResponse.body(), Map.class);
        this.token = (String) tokenResponseMap.get("token");
        // Токен действителен 10 часов согласно документации
        this.tokenExpiry = Instant.now().plus(Duration.ofHours(10));
        return this.token;
    }
}
