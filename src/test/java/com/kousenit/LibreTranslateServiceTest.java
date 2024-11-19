package com.kousenit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.kousenit.LibreTranslateService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LibreTranslateServiceTest {
    private LibreTranslateService service;

    @BeforeEach
    void setUp() {
        service = new LibreTranslateService();
    }

    @Test
    @DisplayName("Should translate simple text from English to German")
    void translateBasicEnglishToGerman() {
        var request = new TranslateRequest("en", "de", "Hello world");
        String result = service.translate(request);
        assertThat(result).isEqualTo("Hallo Welt");
    }

    @ParameterizedTest
    @CsvSource({
            "en, es, Hello, Hola",
            "en, fr, Good morning, Bonjour",
            "en, it, Thank you, Grazie",
            "en, de, Goodbye, Auf Wiedersehen"
    })
    @DisplayName("Should translate correctly to multiple languages")
    void translateToMultipleLanguages(String source, String target, String input, String expected) {
        var request = new TranslateRequest(source, target, input);
        String result = service.translate(request);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should handle special characters")
    void translateWithSpecialCharacters() {
        var request = new TranslateRequest(
                "en", "de", "Hello! How are you? 123 #@$"
        );
        String result = service.translate(request);
        assertThat(result).isNotBlank()
                .containsPattern("[!?]")
                .contains("123");
    }

    @Test
    @DisplayName("Should throw exception for unsupported language")
    void translateUnsupportedLanguage() {
        assumeTrue(service != null, "Service must be initialized");
        var request = new TranslateRequest("en", "xx", "Hello");

        assertThatThrownBy(() -> service.translate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported target language: xx");
    }

    @Test
    @DisplayName("Should validate source language")
    void validateSourceLanguage() {
        var request = new TranslateRequest("xx", "de", "test");

        assertThatThrownBy(() -> service.translate(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported source language: xx");
    }

    @Test
    @DisplayName("Should handle long text translations")
    void translateLongText() {
        String longText = """
                This is a very long text that needs to be translated. \
                It contains multiple sentences and should test the system's \
                ability to handle larger content blocks effectively.""";
        var request = new TranslateRequest("en", "de", longText);
        String result = service.translate(request);
        assertThat(result).isNotBlank()
                .contains(".")
                .hasSizeGreaterThan(longText.length() / 2);
    }

    @Test
    @DisplayName("Should maintain newlines in translation")
    void translateWithNewlines() {
        String textWithNewlines = "Line 1\nLine 2\nLine 3";
        var request = new TranslateRequest("en", "de", textWithNewlines);
        String result = service.translate(request);
        assertThat(result).contains("\n")
                .hasLineCount(3);
    }

    @Test
    @DisplayName("Should handle server connection failures")
    void handleServerFailure() {
        assertThatThrownBy(() -> new LibreTranslateService() {
            @Override
            protected String getTranslateUrl() {
                return "http://localhost:9999/translate";
            }
        })
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to fetch languages");
    }
}