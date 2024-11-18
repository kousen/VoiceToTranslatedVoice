package com.kousenit;

import com.assemblyai.api.AssemblyAI;
import com.assemblyai.api.resources.transcripts.types.Transcript;
import com.assemblyai.api.resources.transcripts.types.TranscriptStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class AssemblyAITranscribeService {
    private final AssemblyAI client;

    public AssemblyAITranscribeService() {
        client = AssemblyAI.builder()
                .apiKey(System.getenv("ASSEMBLYAI_API_KEY"))
                .build();
    }

    public Optional<String> transcribe(InputStream audioStream) throws IOException {
        Transcript transcript = client.transcripts().transcribe(audioStream);

        if (transcript.getStatus().equals(TranscriptStatus.ERROR)) {
            throw new IOException("Transcription failed: " + transcript.getError());
        }

        return transcript.getText();
    }
}