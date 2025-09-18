package com.sidi.zitter_mao;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
