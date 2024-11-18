package com.kousenit;

import net.andrewcpu.elevenlabs.ElevenLabs;
import net.andrewcpu.elevenlabs.builders.SpeechGenerationBuilder;
import net.andrewcpu.elevenlabs.enums.ElevenLabsVoiceModel;
import net.andrewcpu.elevenlabs.enums.StreamLatencyOptimization;
import net.andrewcpu.elevenlabs.model.voice.Voice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

public class ElevenLabsService {
    private final Logger logger = Logger.getLogger(ElevenLabsService.class.getName());
    private static final String VOICE_ID = "CXJAacovzWn9Fp4Rcjcs";

    public ElevenLabsService() {
        String apiKey = System.getenv("ELEVENLABS_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ELEVENLABS_API_KEY environment variable not set");
        }
        ElevenLabs.setApiKey(apiKey);
    }

    public void generateSpeech(String text, String fileName) {
        logger.info("Starting speech generation for file: %s".formatted(fileName));
        Instant start = Instant.now();

        try {
            Path outputDir = Paths.get("src/main/resources/");
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            Path outputPath = outputDir.resolve(fileName + ".mp3");

            // Get the input stream with streamed audio
            try (InputStream inputStream = SpeechGenerationBuilder.textToSpeech()
                    .streamed()
                    .setText(text)
                    .setVoice(Voice.getVoice(VOICE_ID))
                    .setModel(ElevenLabsVoiceModel.ELEVEN_MULTILINGUAL_V2)
                    .setLatencyOptimization(StreamLatencyOptimization.NONE)
                    .build()) {

                // Copy the stream to the file
                Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
            }

            Duration duration = Duration.between(start, Instant.now());
            logger.info("Successfully generated speech file: %s in %d seconds".formatted(
                    fileName, duration.toSeconds()));

        } catch (IOException e) {
            logger.severe("Failed to generate speech for file: %s".formatted(fileName));
            throw new RuntimeException("Failed to generate speech: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.severe("Unexpected error generating speech for file: %s".formatted(fileName));
            throw new RuntimeException("Unexpected error generating speech: " + e.getMessage(), e);
        }
    }
}