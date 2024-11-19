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

    private final Set<Language> languages;
    private final Set<String> supportedTargetLanguages;
    private final Set<Language> supportedLanguages;

    private final Gson gson = new Gson();

    public record TranslateRequest(String source, String target, String q) {}
    public record TranslateResponse(String translatedText) {}
    public record Language(String code, String name, List<String> targets) {}

    public LibreTranslateService() {
        this.languages = fetchLanguages();
        this.supportedTargetLanguages = fetchSupportedLanguages();
        this.supportedLanguages = getSupportedLanguages();
    }

    private Set<Language> fetchLanguages() {
        try (var httpClient = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(getTranslateUrl().replace("/translate", "/languages")))
                    .GET()
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Type setType = new TypeToken<HashSet<Language>>(){}.getType();
            return gson.fromJson(response.body(), setType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch languages", e);
        }
    }

    private Set<String> fetchSupportedLanguages() {
        return languages.stream()
                .flatMap(lang -> lang.targets().stream())
                .collect(Collectors.toSet());
    }

    public Set<Language> validateLanguages(List<String> languageCodes) {
        return languageCodes.stream()
                .map(this::findLanguageByCode)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

    public Optional<Language> findLanguageByCode(String code) {
        return supportedLanguages.stream()
                .filter(lang -> lang.code().equals(code))
                .findFirst();
    }

    public Set<Language> getSupportedLanguages() {
        return languages;
    }

    public String translate(TranslateRequest request) {
        if (request.q() == null || request.q().isBlank()) {
            return "";
        }
        if (!supportedTargetLanguages.contains(request.target())) {
            throw new IllegalArgumentException("Unsupported target language: " + request.target());
        }
        if (!supportedTargetLanguages.contains(request.source())) {
            throw new IllegalArgumentException("Unsupported source language: " + request.source());
        }

        String text = gson.toJson(request);
        try (var httpClient = HttpClient.newHttpClient()) {
            var httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(getTranslateUrl()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(text))
                    .build();
            var response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Translation failed with HTTP response code: " + response.statusCode());
            }

            try {
                var json = gson.fromJson(response.body(), TranslateResponse.class);
                if (json == null || json.translatedText() == null) {
                    throw new RuntimeException("Failed to parse translation response: null response");
                }
                return json.translatedText();
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse translation response: " + e.getMessage(), e);
            }
        } catch (IOException | InterruptedException e) {
            String msg = "Error translating text. Is the local server running?";
            System.err.println(msg);
            throw new RuntimeException(msg, e);
        }
    }

    protected String getTranslateUrl() {
        return LOCAL_TRANSLATE_URL;
    }
}