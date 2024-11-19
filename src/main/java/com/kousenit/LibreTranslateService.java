package com.kousenit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

public class LibreTranslateService {
    private final static String LOCAL_TRANSLATE_URL = "http://localhost:5001/translate";

    private final Set<Language> availableLanguages;
    private final Set<String> supportedTargetCodes;

    private final Gson gson = new Gson();

    public record TranslateRequest(String source, String target, String q) {}
    public record TranslateResponse(String translatedText) {}
    public record Language(String code, String name, List<String> targets) {}

    public LibreTranslateService() {
        this.availableLanguages = fetchLanguagesFromService();
        this.supportedTargetCodes = extractSupportedTargetCodes();
    }

    private Set<Language> fetchLanguagesFromService() {
        try (var httpClient = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(getTranslateUrl().replace("/translate", "/languages")))
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Type setType = new TypeToken<HashSet<Language>>(){}.getType();
            return Collections.unmodifiableSet(gson.fromJson(response.body(), setType));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch languages", e);
        }
    }

    private Set<String> extractSupportedTargetCodes() {
        return availableLanguages.stream()
                .flatMap(lang -> lang.targets().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String translate(TranslateRequest request) {
        validateTranslateRequest(request);

        String requestJson = gson.toJson(request);
        try (var httpClient = HttpClient.newHttpClient()) {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getTranslateUrl()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Translation failed with HTTP response code: " + response.statusCode());
            }

            return parseTranslationResponse(response.body());
        } catch (IOException | InterruptedException e) {
            String msg = "Error translating text. Is the local server running?";
            System.err.println(msg);
            throw new RuntimeException(msg, e);
        }
    }

    private void validateTranslateRequest(TranslateRequest request) {
        if (request.q() == null || request.q().isBlank()) {
            throw new IllegalArgumentException("Translation text cannot be empty");
        }
        if (!supportedTargetCodes.contains(request.target())) {
            throw new IllegalArgumentException("Unsupported target language: " + request.target());
        }
        if (!supportedTargetCodes.contains(request.source())) {
            throw new IllegalArgumentException("Unsupported source language: " + request.source());
        }
    }

    private String parseTranslationResponse(String responseBody) {
        try {
            var response = gson.fromJson(responseBody, TranslateResponse.class);
            if (response == null || response.translatedText() == null) {
                throw new RuntimeException("Failed to parse translation response: null response");
            }
            return response.translatedText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse translation response: " + e.getMessage(), e);
        }
    }

    protected String getTranslateUrl() {
        return LOCAL_TRANSLATE_URL;
    }
}