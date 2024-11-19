package com.kousenit;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public class AudioRecorder {
    private TargetDataLine targetLine;
    private ByteArrayOutputStream recordingStream;
    private volatile boolean isRecording = false;
    private CompletableFuture<InputStream> recordingFuture;
    private AudioFormat audioFormat;

    public CompletableFuture<InputStream> startRecording() {
        if (isRecording) {
            throw new IllegalStateException("Recording is already in progress!");
        }

        try {
            // Use 16-bit audio format as required by AssemblyAI
            audioFormat = new AudioFormat(
                    16000,    // Sample rate
                    16,       // Sample size in bits
                    1,        // Channels (mono)
                    true,     // Signed
                    true      // Big endian
            );
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);

            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Line not supported");
            }

            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(audioFormat);
            targetLine.start();

            isRecording = true;
            recordingStream = new ByteArrayOutputStream();
            recordingFuture = new CompletableFuture<>();

            Thread recordingThread = new Thread(this::record);
            recordingThread.start();

            return recordingFuture;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to start recording", ex);
        }
    }

    private void record() {
        byte[] buffer = new byte[4096];

        try {
            // Record raw audio data
            while (isRecording) {
                int count = targetLine.read(buffer, 0, buffer.length);
                if (count > 0) {
                    recordingStream.write(buffer, 0, count);
                }
            }

            // Convert raw data to WAV format after recording is complete
            byte[] audioData = recordingStream.toByteArray();
            var audioInputStream = new AudioInputStream(
                    new ByteArrayInputStream(audioData),
                    audioFormat,
                    audioData.length / audioFormat.getFrameSize()
            );

            // Create a new stream for the WAV data
            var wavStream = new ByteArrayOutputStream();
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavStream);

            // Complete the future with the WAV data
            recordingFuture.complete(new ByteArrayInputStream(wavStream.toByteArray()));

        } catch (Exception e) {
            recordingFuture.completeExceptionally(e);
        }
    }

    public void stopRecording() {
        if (!isRecording) {
            throw new IllegalStateException("No recording is currently in progress!");
        }

        isRecording = false;
        targetLine.stop();
        targetLine.close();
    }

    public boolean isRecording() {
        return isRecording;
    }
}