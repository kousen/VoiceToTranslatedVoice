package com.kousenit;

import org.junit.jupiter.api.*;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class AudioRecorderTest {
    private AudioRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new AudioRecorder();
    }

    @Test
    @DisplayName("Should start recording successfully")
    void startRecordingSuccessfully() {
        CompletableFuture<InputStream> future = recorder.startRecording();
        assertThat(recorder.isRecording()).isTrue();
        assertThat(future).isNotNull();
        recorder.stopRecording();
    }

    @Test
    @DisplayName("Should not allow multiple simultaneous recordings")
    void preventSimultaneousRecordings() {
        recorder.startRecording();
        assertThatThrownBy(() -> recorder.startRecording())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Recording is already in progress!");
        recorder.stopRecording();
    }

    @Test
    @DisplayName("Should stop recording successfully")
    void stopRecordingSuccessfully() {
        recorder.startRecording();
        recorder.stopRecording();
        assertThat(recorder.isRecording()).isFalse();
    }

    @Test
    @DisplayName("Should not allow stopping when not recording")
    void preventStoppingWhenNotRecording() {
        assertThatThrownBy(() -> recorder.stopRecording())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No recording is currently in progress!");
    }

    @Test
    @DisplayName("Should record audio in correct format")
    void recordInCorrectFormat() throws Exception {
        CompletableFuture<InputStream> future = recorder.startRecording();
        // Record for 1 second
        Thread.sleep(1000);
        recorder.stopRecording();

        // Get recording and verify format
        InputStream recordedData = future.get(5, TimeUnit.SECONDS);
        assertThat(recordedData).isNotNull();

        // Verify WAV format
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(recordedData);
        assertThat(audioInputStream.getFormat().getSampleRate()).isEqualTo(16000f);
        assertThat(audioInputStream.getFormat().getSampleSizeInBits()).isEqualTo(16);
        assertThat(audioInputStream.getFormat().getChannels()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle recording thread interruption")
    void handleRecordingInterruption() throws Exception {
        CompletableFuture<InputStream> future = recorder.startRecording();
        Thread.sleep(100); // Let recording start

        // Force an interruption by stopping abruptly
        recorder.stopRecording();

        // Verify we still get valid data
        InputStream result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isNotNull();
    }

    @AfterEach
    void tearDown() {
        if (recorder.isRecording()) {
            recorder.stopRecording();
        }
    }
}