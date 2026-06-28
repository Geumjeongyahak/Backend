package geumjeongyahak.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import geumjeongyahak.domain.file.config.DriveUploadProperties;
import geumjeongyahak.domain.file.enums.DriveUploadTarget;
import geumjeongyahak.common.exception.BusinessException;

class GoogleDriveStorageServiceTest {

    private HttpServer server;
    private AtomicBoolean permissionCreated;
    private AtomicInteger folderCreateCount;
    private AtomicReference<String> currentFolderId;

    @BeforeEach
    void setUp() throws IOException {
        permissionCreated = new AtomicBoolean(false);
        folderCreateCount = new AtomicInteger(0);
        currentFolderId = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/upload/drive/v3/files", this::handleUpload);
        server.createContext("/drive/v3/files", this::handleDriveFiles);
        server.createContext("/drive/v3/files/drive-file-123/permissions", this::handlePermission);
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void upload_sendsMultipartWithSharedDriveSupportAndCreatesPermission() {
        DriveUploadProperties properties = new DriveUploadProperties();
        properties.setUploadBaseUrl(baseUrl() + "/upload/drive/v3");
        properties.setApiBaseUrl(baseUrl() + "/drive/v3");
        properties.setMakeLinkPublic(true);
        properties.getFolderIds().setBoard("board-folder");

        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(
            "test-token",
            Date.from(Instant.now().plusSeconds(3600))
        ));
        GoogleDriveStorageService service = new GoogleDriveStorageService(
            properties,
            new ObjectMapper(),
            credentials,
            HttpClient.newHttpClient()
        );

        DriveStorageService.StoredDriveFile uploaded = service.upload(
            DriveUploadTarget.BOARD,
            java.util.List.of("공통", "2026", "06"),
            new MockMultipartFile("file", "board.pdf", "application/pdf", "hello drive".getBytes(StandardCharsets.UTF_8))
        );

        assertThat(uploaded.fileId()).isEqualTo("drive-file-123");
        assertThat(uploaded.viewUrl()).contains("/drive-file-123/");
        assertThat(permissionCreated).isTrue();
    }

    @Test
    void upload_failsWhenOAuthConfigIsPartial() {
        DriveUploadProperties properties = new DriveUploadProperties();
        properties.setApiBaseUrl(baseUrl() + "/drive/v3");
        properties.getFolderIds().setBoard("board-folder");
        properties.getOauth().setClientId("client-id");

        GoogleDriveStorageService service = new GoogleDriveStorageService(
            properties,
            new ObjectMapper(),
            "",
            HttpClient.newHttpClient()
        );

        assertThatThrownBy(() -> service.upload(
            DriveUploadTarget.BOARD,
            java.util.List.of(),
            new MockMultipartFile("file", "board.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8))
        )).isInstanceOf(BusinessException.class)
            .hasMessageContaining("Google Drive OAuth 인증 정보가 완전하지 않습니다.");
    }

    private void handleUpload(HttpExchange exchange) throws IOException {
        assertThat(exchange.getRequestURI().getQuery()).contains("uploadType=multipart", "supportsAllDrives=true");
        assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer test-token");
        assertThat(exchange.getRequestHeaders().getFirst("Content-Type")).contains("multipart/related");
        assertThat(folderCreateCount).hasValue(3);

        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(body).contains("\"parents\":[\"" + currentFolderId.get() + "\"]", "hello drive");

        respond(exchange, 200, """
            {"id":"drive-file-123","name":"board.pdf","mimeType":"application/pdf","size":"11","webViewLink":"https://drive.google.com/file/d/drive-file-123/view","webContentLink":"https://drive.google.com/uc?id=drive-file-123"}
            """);
    }

    private void handleDriveFiles(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            assertThat(exchange.getRequestURI().getRawQuery())
                .contains("supportsAllDrives=true", "includeItemsFromAllDrives=true");
            respond(exchange, 200, "{\"files\":[]}");
            return;
        }

        if ("POST".equals(exchange.getRequestMethod())) {
            assertThat(exchange.getRequestURI().getQuery()).contains("supportsAllDrives=true");
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(body).contains(
                "\"mimeType\":\"application/vnd.google-apps.folder\"",
                "\"parents\""
            );
            String folderId = "created-folder-" + folderCreateCount.incrementAndGet();
            currentFolderId.set(folderId);
            respond(exchange, 200, "{\"id\":\"" + folderId + "\"}");
            return;
        }

        respond(exchange, 405, "{}");
    }

    private void handlePermission(HttpExchange exchange) throws IOException {
        assertThat(exchange.getRequestURI().getQuery()).contains("supportsAllDrives=true");
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(body).contains("\"type\":\"anyone\"", "\"role\":\"reader\"");
        permissionCreated.set(true);
        respond(exchange, 200, "{}");
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
