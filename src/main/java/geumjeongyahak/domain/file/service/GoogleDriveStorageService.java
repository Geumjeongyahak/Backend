package geumjeongyahak.domain.file.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.domain.file.config.DriveUploadProperties;
import geumjeongyahak.domain.file.enums.DriveUploadTarget;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GoogleDriveStorageService implements DriveStorageService {

    private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";
    private static final String DEFAULT_CONTENT_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;

    private final DriveUploadProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String encodedCredentials;
    private GoogleCredentials credentials;

    @Autowired
    public GoogleDriveStorageService(
        DriveUploadProperties properties,
        ObjectMapper objectMapper,
        @Value("${spring.cloud.gcp.credentials.encoded-key:}") String encodedCredentials
    ) {
        this(properties, objectMapper, encodedCredentials, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build());
    }

    GoogleDriveStorageService(
        DriveUploadProperties properties,
        ObjectMapper objectMapper,
        String encodedCredentials,
        HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.encodedCredentials = encodedCredentials;
        this.httpClient = httpClient;
    }

    GoogleDriveStorageService(
        DriveUploadProperties properties,
        ObjectMapper objectMapper,
        GoogleCredentials credentials,
        HttpClient httpClient
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.encodedCredentials = "";
        this.httpClient = httpClient;
        this.credentials = credentials;
    }

    @Override
    public StoredDriveFile upload(DriveUploadTarget target, List<String> folderPath, MultipartFile file) {
        String originalFilename = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : "file";
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType().toLowerCase(Locale.ROOT) : DEFAULT_CONTENT_TYPE;

        try {
            String folderId = resolveUploadFolderId(properties.folderIdFor(target), folderPath);
            JsonNode uploaded = sendUpload(folderId, originalFilename, contentType, file.getBytes());
            String fileId = uploaded.path("id").asText();
            if (!StringUtils.hasText(fileId)) {
                throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "Google Drive 업로드 응답에 파일 ID가 없습니다.");
            }
            if (properties.isMakeLinkPublic()) {
                createPublicReaderPermission(fileId);
            }

            String viewUrl = textOrDefault(uploaded.path("webViewLink"), "https://drive.google.com/file/d/" + fileId + "/view");
            String downloadUrl = textOrDefault(uploaded.path("webContentLink"), "https://drive.google.com/uc?export=download&id=" + fileId);
            String name = textOrDefault(uploaded.path("name"), originalFilename);
            String mimeType = textOrDefault(uploaded.path("mimeType"), contentType);
            Long size = uploaded.hasNonNull("size") ? uploaded.path("size").asLong() : file.getSize();
            return new StoredDriveFile(fileId, viewUrl, downloadUrl, name, mimeType, size);
        } catch (IOException exception) {
            log.error("Google Drive 파일 업로드 실패: target={}, filename={}", target.path(), originalFilename, exception);
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "Google Drive 파일 업로드에 실패했습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "Google Drive 파일 업로드가 중단되었습니다.");
        }
    }

    @Override
    public byte[] download(String fileId) {
        try {
            HttpRequest request = HttpRequest.newBuilder(fileDownloadUri(fileId))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
                .GET()
                .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("Google Drive 파일 다운로드 실패: status={}, fileId={}", response.statusCode(), fileId);
                throw new BusinessException(CommonErrorCode.FILE_DOWNLOAD_FAILED, "Google Drive 파일 다운로드에 실패했습니다.");
            }
            return response.body();
        } catch (IOException exception) {
            log.error("Google Drive 파일 다운로드 실패: fileId={}", fileId, exception);
            throw new BusinessException(CommonErrorCode.FILE_DOWNLOAD_FAILED, "Google Drive 파일 다운로드에 실패했습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(CommonErrorCode.FILE_DOWNLOAD_FAILED, "Google Drive 파일 다운로드가 중단되었습니다.");
        }
    }

    public String findOrCreateFolder(String parentId, String folderName) throws IOException, InterruptedException {
        JsonNode files = sendJson(HttpRequest.newBuilder(folderSearchUri(parentId, folderName))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
            .GET()
            .build()).path("files");

        if (files.isArray() && !files.isEmpty()) {
            String existingId = files.get(0).path("id").asText();
            if (StringUtils.hasText(existingId)) {
                return existingId;
            }
        }

        JsonNode created = sendJson(HttpRequest.newBuilder(folderCreateUri())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(writeJson(Map.of(
                "name", folderName,
                "mimeType", "application/vnd.google-apps.folder",
                "parents", List.of(parentId)
            ))))
            .build());
        String createdId = created.path("id").asText();
        if (!StringUtils.hasText(createdId)) {
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "Google Drive 폴더 생성 응답에 폴더 ID가 없습니다.");
        }
        return createdId;
    }

    private String resolveUploadFolderId(String rootFolderId, List<String> folderPath) throws IOException, InterruptedException {
        String folderId = rootFolderId;
        for (String folderName : folderPath) {
            folderId = findOrCreateFolder(folderId, folderName);
        }
        return folderId;
    }

    private JsonNode sendUpload(String folderId, String originalFilename, String contentType, byte[] content)
        throws IOException, InterruptedException {
        String metadataJson = writeJson(Map.of(
            "name", originalFilename,
            "parents", List.of(folderId)
        ));
        String boundary = "gjlearn-" + UUID.randomUUID();
        byte[] body = multipartRelatedBody(boundary, metadataJson, contentType, content);

        HttpRequest request = HttpRequest.newBuilder(uploadUri())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
            .header(HttpHeaders.CONTENT_TYPE, "multipart/related; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
        return sendJson(request);
    }

    private void createPublicReaderPermission(String fileId) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(permissionUri(fileId))
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(writeJson(Map.of(
                "type", "anyone",
                "role", "reader"
            ))))
            .build();
        sendJson(request);
    }

    private JsonNode sendJson(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("Google Drive API 요청 실패: status={}, body={}", response.statusCode(), response.body());
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "Google Drive API 요청에 실패했습니다.");
        }
        if (!StringUtils.hasText(response.body())) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(response.body());
    }

    private URI uploadUri() {
        return URI.create(properties.getUploadBaseUrl()
            + "/files?uploadType=multipart&supportsAllDrives=true&fields=id,name,mimeType,size,webViewLink,webContentLink");
    }

    private URI folderSearchUri(String parentId, String folderName) {
        String query = "'" + escapeDriveQuery(parentId) + "' in parents"
            + " and name = '" + escapeDriveQuery(folderName) + "'"
            + " and mimeType = 'application/vnd.google-apps.folder'"
            + " and trashed = false";
        return URI.create(properties.getApiBaseUrl()
            + "/files?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
            + "&pageSize=1&fields=files(id,name)&supportsAllDrives=true&includeItemsFromAllDrives=true");
    }

    private URI folderCreateUri() {
        return URI.create(properties.getApiBaseUrl()
            + "/files?supportsAllDrives=true&fields=id");
    }

    private URI permissionUri(String fileId) {
        return URI.create(properties.getApiBaseUrl()
            + "/files/" + URLEncoder.encode(fileId, StandardCharsets.UTF_8)
            + "/permissions?supportsAllDrives=true");
    }

    private URI fileDownloadUri(String fileId) {
        return URI.create(properties.getApiBaseUrl()
            + "/files/" + URLEncoder.encode(fileId, StandardCharsets.UTF_8)
            + "?alt=media&supportsAllDrives=true");
    }

    private String escapeDriveQuery(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private String accessToken() throws IOException {
        GoogleCredentials scopedCredentials = credentials();
        scopedCredentials.refreshIfExpired();
        return scopedCredentials.getAccessToken().getTokenValue();
    }

    private GoogleCredentials credentials() throws IOException {
        if (credentials != null) {
            return credentials;
        }
        DriveUploadProperties.OAuth oauth = properties.getOauth();
        boolean hasOAuthClientId = StringUtils.hasText(oauth.getClientId());
        boolean hasOAuthClientSecret = StringUtils.hasText(oauth.getClientSecret());
        boolean hasOAuthRefreshToken = StringUtils.hasText(oauth.getRefreshToken());
        if (hasOAuthClientId || hasOAuthClientSecret || hasOAuthRefreshToken) {
            if (!hasOAuthClientId || !hasOAuthClientSecret || !hasOAuthRefreshToken) {
                throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "Google Drive OAuth 인증 정보가 완전하지 않습니다.");
            }
            credentials = UserCredentials.newBuilder()
                .setClientId(oauth.getClientId())
                .setClientSecret(oauth.getClientSecret())
                .setRefreshToken(oauth.getRefreshToken())
                .build();
            return credentials;
        }
        if (!StringUtils.hasText(encodedCredentials)) {
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "Google Drive 서비스 계정 인증 정보가 설정되지 않았습니다.");
        }
        byte[] decoded = Base64.getDecoder().decode(encodedCredentials);
        credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(decoded))
            .createScoped(List.of(DRIVE_SCOPE));
        return credentials;
    }

    private byte[] multipartRelatedBody(String boundary, String metadataJson, String contentType, byte[] content)
        throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: application/json; charset=UTF-8\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(metadataJson.getBytes(StandardCharsets.UTF_8));
        output.write(("\r\n--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(content);
        output.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private String writeJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    private String textOrDefault(JsonNode node, String fallback) {
        return node.isTextual() && StringUtils.hasText(node.asText()) ? node.asText() : fallback;
    }
}
