package geumjeongyahak.domain.file.service;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.validation.FileValidationSupport;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.users.service.UserProxyService;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageUploadService {

    private static final String PROFILE_DIRECTORY = "profiles";
    private static final String EDITOR_DIRECTORY = "editor";
    private static final String SITE_CONTENT_DIRECTORY = "site-contents";
    private static final String PURCHASE_ITEM_DIRECTORY = "documents/purchase-items";
    private static final int PROFILE_IMAGE_WIDTH = 256;
    private static final int PROFILE_IMAGE_HEIGHT = 256;

    private final UserProxyService userProxyService;
    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final FileValidationSupport fileValidationSupport;

    @Transactional
    public FileUploadResponse uploadProfileImage(Long userId, MultipartFile file) {
        userProxyService.getById(userId);
        fileValidationSupport.validateImage(file);

        byte[] resizedImage = resizeToPng(file);
        StorageService.StoredFile storedFile = storageService.upload(
            resizedImage,
            "image/png",
            "profile-" + userId + ".png",
            PROFILE_DIRECTORY
        );

        File savedFile = saveFile(
            storedFile,
            "profile-" + userId + ".png",
            "image/png",
            Long.valueOf(resizedImage.length),
            "png"
        );

        log.debug("프로필 이미지 업로드 완료 (userId={}, fileId={})", userId, savedFile.getId());
        return FileUploadResponse.from(savedFile, storedFile.url());
    }

    @Transactional
    public FileUploadResponse uploadPostImage(MultipartFile file) {
        fileValidationSupport.validateImage(file);

        StorageService.StoredFile storedFile = storageService.upload(file, EDITOR_DIRECTORY);
        File savedFile = saveUploadedImage(file, storedFile);

        log.debug("게시글 이미지 업로드 완료 (fileId={})", savedFile.getId());
        return FileUploadResponse.from(savedFile, storedFile.url());
    }

    @Transactional
    public FileUploadResponse uploadSiteContentImage(MultipartFile file) {
        fileValidationSupport.validateImage(file);

        StorageService.StoredFile storedFile = storageService.upload(file, SITE_CONTENT_DIRECTORY);
        File savedFile = saveUploadedImage(file, storedFile);

        log.debug("사이트 콘텐츠 이미지 업로드 완료 (fileId={})", savedFile.getId());
        return FileUploadResponse.from(savedFile, storedFile.url());
    }

    @Transactional
    public FileUploadResponse uploadPurchaseItemImage(MultipartFile file) {
        fileValidationSupport.validateImage(file);

        StorageService.StoredFile storedFile = storageService.upload(file, PURCHASE_ITEM_DIRECTORY);
        File savedFile = saveUploadedImage(file, storedFile);

        log.debug("구매 대상 이미지 업로드 완료 (fileId={})", savedFile.getId());
        return FileUploadResponse.from(savedFile, storedFile.url());
    }

    private File saveUploadedImage(MultipartFile file, StorageService.StoredFile storedFile) {
        return saveFile(
            storedFile,
            file.getOriginalFilename(),
            file.getContentType(),
            file.getSize(),
            fileValidationSupport.extractExtension(file.getOriginalFilename())
        );
    }

    private File saveFile(
        StorageService.StoredFile storedFile,
        String originalName,
        String contentType,
        Long fileSize,
        String ext
    ) {
        return fileRepository.save(
            File.builder()
                .storageKey(storedFile.path())
                .bucket(storedFile.bucket())
                .originalName(originalName)
                .contentType(contentType)
                .fileSize(fileSize)
                .ext(ext)
                .publicUrl(storedFile.url())
                .build()
        );
    }

    private byte[] resizeToPng(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage sourceImage = ImageIO.read(inputStream);
            if (sourceImage == null) {
                throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "이미지 파일을 읽을 수 없습니다.");
            }

            BufferedImage resizedImage = new BufferedImage(
                PROFILE_IMAGE_WIDTH,
                PROFILE_IMAGE_HEIGHT,
                BufferedImage.TYPE_INT_ARGB
            );

            Graphics2D graphics = resizedImage.createGraphics();
            graphics.setComposite(AlphaComposite.Clear);
            graphics.fillRect(0, 0, PROFILE_IMAGE_WIDTH, PROFILE_IMAGE_HEIGHT);
            graphics.setComposite(AlphaComposite.Src);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, PROFILE_IMAGE_WIDTH, PROFILE_IMAGE_HEIGHT, null);
            graphics.dispose();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "png", outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(CommonErrorCode.FILE_UPLOAD_FAILED, "이미지 변환에 실패했습니다.");
        }
    }
}
