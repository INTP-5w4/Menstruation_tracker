package com.example.andrs.data.sync;

import androidx.annotation.Nullable;

import com.example.andrs.domain.model.BackupInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Cliente mínimo de la API REST de Google Drive v3 restringido a {@code appDataFolder}.
 * Usa HttpURLConnection para no añadir dependencias pesadas.
 */
public class DriveRestClient {
    public static final String BACKUP_FILE_NAME = "cycle_backup.enc";
    private static final String API = "https://www.googleapis.com/drive/v3/files";
    private static final String UPLOAD = "https://www.googleapis.com/upload/drive/v3/files";
    private static final int TIMEOUT_MS = 30_000;
    private static final int MAX_DOWNLOAD_BYTES = 64 * 1024 * 1024;

    public static class HttpException extends IOException {
        public final int code;

        public HttpException(int code, String body) {
            super("HTTP " + code + ": " + body);
            this.code = code;
        }
    }

    @Nullable
    public BackupInfo findBackup(String token) throws IOException {
        String q = URLEncoder.encode("name = '" + BACKUP_FILE_NAME + "' and trashed = false", "UTF-8");
        String url = API + "?spaces=appDataFolder&orderBy=modifiedTime%20desc&pageSize=10"
                + "&fields=files(id,modifiedTime,size)&q=" + q;
        HttpURLConnection c = open(url, "GET", token);
        String body = readText(c);
        try {
            JSONArray files = new JSONObject(body).optJSONArray("files");
            if (files == null || files.length() == 0) return null;
            JSONObject f = files.getJSONObject(0);
            long modified = Instant.parse(f.getString("modifiedTime")).toEpochMilli();
            return new BackupInfo(f.getString("id"), modified, f.optLong("size", 0));
        } catch (JSONException | RuntimeException e) {
            throw new IOException("Respuesta inesperada de Drive", e);
        }
    }

    /** Crea o reemplaza el archivo de respaldo. Devuelve el id remoto. */
    public String upload(String token, @Nullable String existingId, byte[] content) throws IOException {
        if (existingId != null) {
            HttpURLConnection c = open(UPLOAD + "/" + existingId + "?uploadType=media&fields=id",
                    "PATCH", token);
            c.setRequestProperty("Content-Type", "application/octet-stream");
            writeBody(c, content);
            return parseId(readText(c));
        }

        String boundary = "bnd" + UUID.randomUUID().toString().replace("-", "");
        String metadata = "{\"name\":\"" + BACKUP_FILE_NAME + "\",\"parents\":[\"appDataFolder\"]}";
        ByteArrayOutputStream body = new ByteArrayOutputStream(content.length + 512);
        String part1 = "--" + boundary + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n"
                + metadata + "\r\n--" + boundary + "\r\nContent-Type: application/octet-stream\r\n\r\n";
        body.write(part1.getBytes(StandardCharsets.UTF_8));
        body.write(content);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpURLConnection c = open(UPLOAD + "?uploadType=multipart&fields=id", "POST", token);
        c.setRequestProperty("Content-Type", "multipart/related; boundary=" + boundary);
        writeBody(c, body.toByteArray());
        return parseId(readText(c));
    }

    public byte[] download(String token, String fileId) throws IOException {
        HttpURLConnection c = open(API + "/" + fileId + "?alt=media", "GET", token);
        int code = c.getResponseCode();
        if (code / 100 != 2) throw new HttpException(code, readError(c));
        try (InputStream in = c.getInputStream()) {
            return readAll(in, MAX_DOWNLOAD_BYTES);
        } finally {
            c.disconnect();
        }
    }

    public void delete(String token, String fileId) throws IOException {
        HttpURLConnection c = open(API + "/" + fileId, "DELETE", token);
        int code = c.getResponseCode();
        c.disconnect();
        if (code / 100 != 2 && code != 404) throw new HttpException(code, "");
    }

    // ---------------------------------------------------------------------------------------------

    private static HttpURLConnection open(String url, String method, String token) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setUseCaches(false);
        if ("PATCH".equals(method)) {
            // HttpURLConnection no admite PATCH; Drive acepta este encabezado.
            c.setRequestMethod("POST");
            c.setRequestProperty("X-HTTP-Method-Override", "PATCH");
        } else {
            c.setRequestMethod(method);
        }
        c.setRequestProperty("Authorization", "Bearer " + token);
        return c;
    }

    private static void writeBody(HttpURLConnection c, byte[] body) throws IOException {
        c.setDoOutput(true);
        c.setFixedLengthStreamingMode(body.length);
        try (OutputStream out = c.getOutputStream()) {
            out.write(body);
        }
    }

    private static String readText(HttpURLConnection c) throws IOException {
        try {
            int code = c.getResponseCode();
            if (code / 100 != 2) throw new HttpException(code, readError(c));
            try (InputStream in = c.getInputStream()) {
                return new String(readAll(in, 1024 * 1024), StandardCharsets.UTF_8);
            }
        } finally {
            c.disconnect();
        }
    }

    private static String readError(HttpURLConnection c) {
        try (InputStream err = c.getErrorStream()) {
            if (err == null) return "";
            return new String(readAll(err, 16 * 1024), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private static String parseId(String json) throws IOException {
        try {
            return new JSONObject(json).getString("id");
        } catch (JSONException e) {
            throw new IOException("Respuesta inesperada de Drive", e);
        }
    }

    private static byte[] readAll(InputStream in, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
            if (out.size() > max) throw new IOException("Respuesta demasiado grande");
        }
        return out.toByteArray();
    }
}
