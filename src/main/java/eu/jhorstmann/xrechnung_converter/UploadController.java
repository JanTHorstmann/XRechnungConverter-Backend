package eu.jhorstmann.xrechnung_converter;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import java.io.*;

@RestController
@RequestMapping("/api")
public class UploadController {

@PostMapping("/upload-pdf")
public ResponseEntity<byte[]> uploadPdf(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
        return ResponseEntity.badRequest()
                .body("Keine Datei hochgeladen.".getBytes());
    }

    File tempPdf = null;
    File outputXml = null;

    try {
        // Temporäre Dateien erstellen
        tempPdf = File.createTempFile("input-", ".pdf");
        file.transferTo(tempPdf);

        outputXml = File.createTempFile("output-", ".xml");

        // Wenn Datei (z. B. durch vorherige Läufe) existiert, löschen
        if (outputXml.exists()) {
            boolean deleted = outputXml.delete();
            if (!deleted) {
                System.err.println("WARNUNG: Konnte alte Ausgabedatei nicht löschen: " + outputXml.getAbsolutePath());
            }
        }

        String mustangPath = "/home/jhorstmann/xrechnung-converter/Mustang-CLI-2.20.0.jar";

        // Mustang CLI starten
        ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar", mustangPath,
                "--action", "extract",
                "--source", tempPdf.getAbsolutePath(),
                "--out", outputXml.getAbsolutePath()
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Debug-Ausgabe der Mustang CLI
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("MUSTANG: " + line);
            }
        }

        int exitCode = process.waitFor();
        System.out.println("Exit-Code: " + exitCode);

        if (exitCode != 0) {
            return ResponseEntity.internalServerError()
                    .body(("Fehler beim Ausführen von Mustang CLI. Exit-Code: " + exitCode).getBytes());
        }

        // XML-Datei lesen
        byte[] xmlBytes = java.nio.file.Files.readAllBytes(outputXml.toPath());

        // XML-Datei als Download zurückgeben
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice.xml\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/xml")
                .body(xmlBytes);

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.internalServerError()
                .body(("Fehler: " + e.getMessage()).getBytes());
    } finally {
        // Temporäre Dateien löschen, um Speicher zu sparen
        if (tempPdf != null && tempPdf.exists()) {
            tempPdf.delete();
        }
        if (outputXml != null && outputXml.exists()) {
            outputXml.delete();
        }
    }
}
}