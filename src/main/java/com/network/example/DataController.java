package com.network.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@RestController
@RequestMapping("/data-controller")
public class DataController {
    @Operation(summary = "Get data String", description = "Returns a text with the requested size in MB")
    @GetMapping("/data")
    public ResponseEntity<String> generateData(@Parameter(description = "Size in MB to be generated") @RequestParam(defaultValue = "1") int size,
                               @Parameter(description = "Unit of byte (GB, MB, KB, B)") @RequestParam(defaultValue = "MB") String unit) {

        int bytes = quantityOfData(unit, size);
        if (bytes == 0) { return ResponseEntity.badRequest().build(); }
        return ResponseEntity.ok().body(generateString(bytes).toString());
    }

    @Operation(summary = "Get file", description = "Returns a file with the requested size in MB")
    @GetMapping("/file")
    public ResponseEntity<byte[]> generateFile(@Parameter(description = "Size to be generated") @RequestParam(defaultValue = "1") int size,
                                               @Parameter(description = "Unit of byte (GB, MB, KB, B)") @RequestParam(defaultValue = "MB") String unit) {

        int bytes = quantityOfData(unit, size);
        if (bytes == 0) { return ResponseEntity.badRequest().build(); }

        byte[] fileContent = generateString(bytes).toString().getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"data.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(fileContent);
    }

    @Operation(summary = "Upload a log file", description = "Uploads a JSON log file or gzip-compressed JSON log file for a specific device. The file must contain a JSON array of log entries. The first log entry must include 'event_id' and 'name' fields for validation purposes. If the file is gzip-compressed, it will be decompressed before processing.")
    @PostMapping(value = "/itdp/log", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadALogFile(
            @RequestParam("logFile") MultipartFile logFile,
            @RequestParam("deviceId") String deviceId
    ) {

        try {
            if (logFile.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("status", "error", "code", "IMG4001", "message", "No file uploaded")
                );
            }

            String json = extractJson(logFile);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            if (!root.isArray() || root.size() == 0) {
                return ResponseEntity.badRequest().body(
                        Map.of("status", "error", "code", "IMG4002", "message", "Log file must contain a JSON array")
                );
            }

            JsonNode firstEntry = root.get(0);

            if (!firstEntry.has("event_id") || !firstEntry.has("name")) {
                return ResponseEntity.badRequest().body(
                        Map.of("status", "error", "code", "IMG4003", "message", "First log entry must contain event_id and name.")
                );
            }

            long min = Long.MAX_VALUE;
            long max = Long.MIN_VALUE;

            for (JsonNode entry : root) {
                if (entry.has("timestamp")) {
                    long ts = entry.get("timestamp").asLong();
                    min = Math.min(min, ts);
                    max = Math.max(max, ts);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "status", "ok",
                    "message", "Successfully processed " + root.size() + " log entries.",
                    "min_timestamp", min,
                    "max_timestamp", max
            ));
        } catch (Exception error) {
            return ResponseEntity.internalServerError().body(
                    Map.of("status", "error", "code", "IMG5001", "message", "An unexpected error occurred.")
            );
        }
    }


    private StringBuilder generateString(int bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < bytes; i++) {
            // Character A is 1 byte in UTF-8
            stringBuilder.append("A");
        }
        return stringBuilder;
    }

    private int quantityOfData(String unit, int size) {
        return switch (unit) {
            case "GB" -> size * 1024 * 1024 * 1024;
            case "MB" -> size * 1024 * 1024;
            case "KB" -> size * 1024;
            case "B" -> size;
            default -> 0;
        };
    }

    private String extractJson(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();

        InputStream inputStream = file.getInputStream();
        if (filename != null && filename.endsWith(".gz")) {
            try (GZIPInputStream gzip = new GZIPInputStream(inputStream)) {
                return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
