# Voice to Translated Voice

Java application that records audio from your microphone,
transcribes the audio using AssemblyAI, translates the text
using `LibreTranslate` running on your local machine, and
finally generates mp3 files for each translation using the
ElevenLabs API.

You need API keys for AssemblyAI and ElevenLabs. The system
expects LibreTranslate to be running on your local machine
on port 5001.

The good news is that the translations run in parallel, and
the audio generation submits five requests concurrently.
