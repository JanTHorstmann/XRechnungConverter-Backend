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
        return ResponseEntity
            .badRequest()
            .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
            .body("Keine Datei hochgeladen.".getBytes());
    }
    
        File tempPdf = null;
        File outputXml = null;
    
        try {
            // Temporäre Dateien erstellen
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "xrechnung");
            tempDir.mkdirs();
        
            tempPdf = File.createTempFile("input-", ".pdf");
            file.transferTo(tempPdf);
        
            outputXml = File.createTempFile("output-", ".xml");
        
            // Wenn Datei (z. B. durch vorherige Läufe) existiert, löschen
            if (outputXml.exists() && !outputXml.delete()) {
                System.err.println("WARNUNG: Alte Ausgabedatei konnte nicht gelöscht werden: " + outputXml.getAbsolutePath());
            }
        
            String mustangPath = "/home/jhorstmann/xrechnung-converter/Mustang-CLI-2.20.0.jar";
        
            // Mustang CLI starten
            ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", mustangPath,
            "--action", "extract",
            "--source", tempPdf.getAbsolutePath(),
            "--out", outputXml.getAbsolutePath()
            );
            pb.directory(tempPdf.getParentFile());
            pb.redirectErrorStream(true);
        
            Process process = pb.start();        
        
            int exitCode = process.waitFor();
            System.out.println("Exit-Code: " + exitCode);
        
            if (exitCode != 0 || !outputXml.exists()) {
                String msg = "Fehler beim Ausführen von Mustang CLI (Exit-Code: " + exitCode + ")";
                if (!outputXml.exists()) {
                    msg += " – keine Ausgabedatei gefunden.";
            }
                return ResponseEntity.internalServerError()
                    .header(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8")
                    .body(msg.getBytes());
            }
        
            // XML-Datei lesen
            byte[] xmlBytes = java.nio.file.Files.readAllBytes(outputXml.toPath());
        
            // XML-Datei als Download zurückgeben
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice.xml\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/xml; charset=UTF-8");
        
            return ResponseEntity.ok()
                .headers(headers)
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