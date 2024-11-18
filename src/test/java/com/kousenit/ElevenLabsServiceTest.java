package com.kousenit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ElevenLabsServiceTest {
    private ElevenLabsService service;

    @BeforeEach
    void setUp() {
        // Only create service if API key exists
        if (System.getenv("ELEVENLABS_API_KEY") != null) {
            service = new ElevenLabsService();
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ELEVENLABS_API_KEY", matches = ".+")
    @DisplayName("Should generate speech file successfully")
    void shouldGenerateSpeechFile() throws Exception {
        String text = "Hello, this is a test of the speech generation system.";
        String fileName = "test_speech";

        service.generateSpeech(text, fileName);

        Path outputFile = Path.of("src/main/resources", fileName + ".mp3");
        assertThat(outputFile)
                .exists()
                .isRegularFile()
                .isReadable();
        assertThat(Files.size(outputFile)).isGreaterThan(0L);

        // Cleanup
        Files.deleteIfExists(outputFile);
    }
}