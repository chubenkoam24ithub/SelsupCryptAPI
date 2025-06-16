package selsup.test.ctyptapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import selsup.test.ctyptapi.exception.ApiException;
import selsup.test.ctyptapi.json.Document;
import selsup.test.ctyptapi.json.DocumentRequest;
import selsup.test.ctyptapi.token.AuthTokenManager;
import selsup.test.ctyptapi.token.RateLimiter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CryptApi {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;
    private final String baseUrl;
    private final AuthTokenManager authTokenManager;
    private final Lock authLock = new ReentrantLock();

    /**
     * Конструктор для инициализации API с ограничением запросов.
     *
     * @param timeUnit     Единица времени для окна ограничения (секунды, минуты и т.д.).
     * @param requestLimit Максимальное количество запросов в указанный интервал времени.
     */
    public CryptApi(TimeUnit timeUnit, int requestLimit) {
        this(timeUnit, requestLimit, "https://ismp.crpt.ru/api/v3");
    }

    // Конструктор для тестирования с настраиваемой базовой URL
    CryptApi(TimeUnit timeUnit, int requestLimit, String baseUrl) {
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("Лимит запросов должен быть положительным");
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.rateLimiter = new RateLimiter(timeUnit, requestLimit);
        this.baseUrl = baseUrl;
        this.authTokenManager = new AuthTokenManager(baseUrl, httpClient, objectMapper);
    }

    /**
     * Создает документ для ввода в оборот товара, произведенного в РФ.
     *
     * @param document  Объект документа с данными.
     * @param signature Подпись УКЭП в формате base64.
     * @return Идентификатор созданного документа.
     * @throws Exception Если запрос не удался или превышен лимит.
     */
    public String createDocument(Document document, String signature) throws Exception {
        rateLimiter.acquire();
        String token = getValidToken();

        String jsonBody = objectMapper.writeValueAsString(new DocumentRequest(document, signature));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/lk/documents/create"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201 && response.statusCode() != 202) {
            throw new ApiException("Ошибка API: статус " + response.statusCode() + ", тело: " + response.body());
        }

        // Предполагается, что ответ содержит JSON с идентификатором документа
        var responseMap = objectMapper.readValue(response.body(), Map.class);
        return (String) responseMap.getOrDefault("documentId", "");
    }

    /**
     * Создает ассинхронно документ для ввода в оборот товара, произведенного в РФ.
     *
     * @param document  Объект документа с данными.
     * @param signature Подпись УКЭП в формате base64.
     * @return Идентификатор созданного документа.
     * @throws Exception Если запрос не удался или превышен лимит.
     */
    public CompletableFuture<Object> createDocumentAsync(Document document, String signature) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                rateLimiter.acquire();
                String token = getValidToken();
                String jsonBody = objectMapper.writeValueAsString(new DocumentRequest(document, signature));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/lk/documents/create"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200 && response.statusCode() != 201 && response.statusCode() != 202) {
                    throw new ApiException("Ошибка API: статус " + response.statusCode());
                }
                var responseMap = objectMapper.readValue(response.body(), Map.class);
                return responseMap.getOrDefault("documentId", "");
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    private String getValidToken() throws Exception {
        authLock.lock();
        try {
            if (authTokenManager.isTokenValid()) {
                return authTokenManager.getToken();
            }
            return authTokenManager.refreshToken();
        } finally {
            authLock.unlock();
        }
    }

}

