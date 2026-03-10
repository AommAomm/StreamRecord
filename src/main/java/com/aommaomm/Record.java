package com.aommaomm;

import org.slf4j.Logger;
import java.io.IOException;
import java.io.BufferedReader;
import org.slf4j.LoggerFactory;
import java.io.InputStreamReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Record extends Thread {
    protected String url;
    protected String output;
    protected String streamerName;
    protected String streamTitle;

    private static final Logger logger = LoggerFactory.getLogger(Record.class);

    private static final ObjectMapper objmapper = new ObjectMapper();

    public Record(String streamer, String output){
        this.url = createURL(streamer);
        this.streamerName = streamer;
        this.output = output;
    }

    protected String createURL(String streamer){
        if (streamer.startsWith("@")) {
            return "https://www.youtube.com/%s/live".formatted(streamer);
        }
        return "https://www.twitch.tv/%s".formatted(streamer);
    }

    @Override
    public void run(){
        while (true){
            try {
                String jsonOutput = fetchTwitchData();
                JsonNode nodeData = objmapper.readTree(jsonOutput);
                streamTitle = nodeData.path("metadata").path("title").asText("");

                if (!streamTitle.isEmpty()){
                    logger.info("Found media stream for: {} (Title: {})", url, streamTitle);
                    beginRecord();
                } else {
                    logger.info("{} - No video media stream found.", url);
                }


            } catch (IOException e) {
                logger.error("IO Error for {}: {}", url, e.getMessage());
            } catch (Exception e) {
                // slf4j can handle e-stacktrace without {}
                logger.error("Unexpected error for {}: ", url, e);
            }

            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                logger.error("Interrupted {}", url, e);
            }
        }
    }

    protected String fetchTwitchData() throws IOException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("streamlink", "--json", url);
        Process p = pb.start(); // do not inheri io
        StringBuilder jsonBuffer = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while((line = reader.readLine()) != null){
                jsonBuffer.append(line);
            }
        }

        return jsonBuffer.toString();
    }
    
    protected void beginRecord() {
        try {
            java.io.File streamerDir = new java.io.File(output, streamerName);
            streamerDir.mkdirs();

            // video name and location
            String safeTitle = streamTitle.replaceAll("[\\\\/:*?\"<>|]", "_");
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
            String fileName = "%s_%s.ts".formatted(timestamp, safeTitle);
            java.io.File destination = new java.io.File(streamerDir, fileName);

            logger.info("Starting record for {} -> {}", streamerName, destination.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder("streamlink", url, "best", "-o", destination.getAbsolutePath());
            pb.inheritIO(); // inheritio is fine in thie method
            Process p = pb.start();
            p.waitFor(); // jvm hangs here until stream finishes

            logger.info("Recording finished for {}", streamerName);

        } catch (IOException | InterruptedException e) {
            logger.error("Error recording {}: {}", streamerName, e.getMessage());
        }
    }
}