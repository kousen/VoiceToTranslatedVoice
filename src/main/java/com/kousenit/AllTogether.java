package com.kousenit;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.kousenit.LibreTranslateService.TranslateRequest;

public class AllTogether {
    private static final Logger logger = Logger.getLogger(AllTogether.class.getName());

    private final AudioRecorder recorder = new AudioRecorder();
    private final AssemblyAITranscribeService assemblyAITranscribe = new AssemblyAITranscribeService();
    private final LibreTranslateService libreTranslate = new LibreTranslateService();
    private final ElevenLabsService elevenLabs = new ElevenLabsService();

    public void run(List<String> languageCodes) throws IOException {
        // Record audio
        CompletableFuture<InputStream> recordingFuture = recordAudio();

        // Transcribe audio
        String transcribedText = transcribeAudio(recordingFuture);
        if (transcribedText.isBlank()) {
            logger.severe("No text transcribed. Exiting...");
            return;
        }
        logger.info("Transcription successful. Text: " + transcribedText);

        // Translate and generate speech
        translateAndGenerateSpeech(languageCodes, transcribedText);
        logger.info("All processing completed successfully");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private CompletableFuture<InputStream> recordAudio() throws IOException {
        CompletableFuture<InputStream> recordingFuture = recorder.startRecording();
        try (var input = System.in) {
            logger.info("Recording started. Press Enter to stop...");
            input.read();
        }
        recorder.stopRecording();
        logger.info("Recording stopped successfully");
        return recordingFuture;
    }

    private String transcribeAudio(CompletableFuture<InputStream> recordingFuture) {
        try {
            InputStream audioStream = recordingFuture.join();
            String result = assemblyAITranscribe.transcribe(audioStream).orElseThrow();
            logger.info("Transcription completed successfully");
            System.out.println("Transcription: " + result);
            return result;
        } catch (IOException e) {
            logger.severe("Error during transcription: " + e.getMessage());
            throw new RuntimeException("Transcription failed", e);
        } catch (Exception e) {
            logger.fine("Unexpected error during transcription: " + e.getMessage());
            throw new RuntimeException("Unexpected error during transcription", e);
        }
    }

    private void translateAndGenerateSpeech(List<String> languages, String transcribedText) {
        // Perform translations in parallel
        Map<String, String> translations;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var start = System.currentTimeMillis();
            logger.info("Starting parallel translations for %d languages".formatted(languages.size()));

            Map<String, CompletableFuture<String>> translationFutures = languages.stream()
                    .collect(Collectors.toMap(
                            language -> language,
                            language -> CompletableFuture.supplyAsync(
                                    () -> {
                                        logger.fine("Starting translation for language: " + language);
                                        return libreTranslate.translate(
                                                new TranslateRequest("en", language, transcribedText));
                                    },
                                    executor)
                    ));

            translations = translationFutures.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> {
                                try {
                                    String result = entry.getValue().join();
                                    System.out.printf("Translation for %s: %s%n", entry.getKey(), result);
                                    return result;
                                } catch (Exception e) {
                                    logger.severe("Translation failed for language " + entry.getKey() +
                                ": " + e.getMessage());
                                    throw e;
                                }
                            }
                    ));

            var end = System.currentTimeMillis();
            logger.info("All translations completed in %dms".formatted(end - start));
        } catch (Exception e) {
            logger.severe("Error during translation phase: " + e.getMessage());
            throw new RuntimeException(e);
        }

        // Generate speech sequentially using the completed translations
        // Use a thread pool of size 5 because that's our current limit on the subscription
        try (var speechExecutor = Executors.newFixedThreadPool(5)) {
            var start = System.currentTimeMillis();
            logger.info("Starting parallel speech generation for " + translations.size() + " languages");

            List<CompletableFuture<Void>> speechFutures = translations.entrySet().stream()
                    .map(entry -> CompletableFuture.runAsync(() -> {
                        try {
                            String fileName = "translated_audio_" + entry.getKey();
                            logger.info("Generating speech for language: %s".formatted(entry.getKey()));
                            elevenLabs.generateSpeech(entry.getValue(), fileName);
                            logger.info("Successfully generated speech for language: " + entry.getKey());
                        } catch (Exception e) {
                            logger.severe("Error generating speech for language %s: %s".formatted(entry.getKey(), e.getMessage()));
                            throw e;
                        }
                    }, speechExecutor))
                    .toList();

            // Wait for all speech generation to complete
            CompletableFuture.allOf(speechFutures.toArray(new CompletableFuture[0])).join();

            var end = System.currentTimeMillis();
            logger.info("All speech generation completed in %dms".formatted(end - start));

        } catch (Exception e) {
            logger.severe("Error during speech generation phase: " + e.getMessage());
            throw new RuntimeException("Speech generation failed", e);
        }
    }

    public static void main(String[] args) throws IOException {
        new AllTogether().run(List.of("en", "hi", "de", "fr"));
    }
}