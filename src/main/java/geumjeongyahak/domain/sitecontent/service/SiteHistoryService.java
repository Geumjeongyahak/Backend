package geumjeongyahak.domain.sitecontent.service;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.file.entity.File;
import geumjeongyahak.domain.file.repository.FileRepository;
import geumjeongyahak.domain.sitecontent.entity.SiteHistory;
import geumjeongyahak.domain.sitecontent.entity.SiteHistory.LinkValue;
import geumjeongyahak.domain.sitecontent.entity.SiteHistory.PhotoValue;
import geumjeongyahak.domain.sitecontent.entity.SiteHistoryLink;
import geumjeongyahak.domain.sitecontent.entity.SiteHistoryPhoto;
import geumjeongyahak.domain.sitecontent.exception.SiteContentErrorCode;
import geumjeongyahak.domain.sitecontent.repository.SiteHistoryRepository;
import geumjeongyahak.domain.sitecontent.v1.dto.request.CreateSiteHistoryRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.SiteHistoryLinkRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.SiteHistoryPhotoRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.UpdateSiteHistoryRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteHistoriesResponse;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteHistoryResponse;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteHistoryService {

    private static final int LINK_LABEL_MAX_LENGTH = 120;

    private final SiteHistoryRepository siteHistoryRepository;
    private final FileRepository fileRepository;

    public SiteHistoriesResponse getHistories() {
        return new SiteHistoriesResponse(siteHistoryRepository.findAllByOrderByHistoryDateAscSortOrderAscIdAsc()
            .stream()
            .map(SiteHistoryResponse::from)
            .toList());
    }

    public List<SiteHistoryAdminRow> getAdminHistories() {
        return siteHistoryRepository.findAllByOrderByHistoryDateAscSortOrderAscIdAsc()
            .stream()
            .map(SiteHistoryAdminRow::from)
            .toList();
    }

    public SiteHistoryAdminRow getAdminHistory(Long historyId) {
        return SiteHistoryAdminRow.from(findHistory(historyId));
    }

    @Transactional
    public SiteHistoryResponse createHistory(CreateSiteHistoryRequest request) {
        SiteHistory history = SiteHistory.builder()
            .title(request.title())
            .historyDate(request.historyDate())
            .detail(request.detail())
            .build();
        history.replaceLinks(toLinkValues(request.links()));
        history.replacePhotos(toPhotoValues(Map.of(), request.photos()));

        SiteHistory saved = siteHistoryRepository.save(history);
        log.info("사이트 연혁 생성 완료 - id={}", saved.getId());
        return SiteHistoryResponse.from(saved);
    }

    @Transactional
    public Long createAdminHistory(
        String title,
        String detail,
        LocalDate historyDate,
        Integer sortOrder,
        String linksText,
        List<SiteHistoryPhotoRequest> photos
    ) {
        SiteHistory history = SiteHistory.builder()
            .title(title)
            .historyDate(historyDate)
            .detail(detail)
            .sortOrder(sortOrder)
            .build();
        history.replaceLinks(toLinkValues(parseLinkRequests(linksText)));
        history.replacePhotos(toPhotoValues(Map.of(), photos));
        return siteHistoryRepository.save(history).getId();
    }

    @Transactional
    public SiteHistoryResponse updateHistory(Long historyId, UpdateSiteHistoryRequest request) {
        SiteHistory history = findHistory(historyId);
        Set<UUID> oldFileIds = collectFileIds(history);
        List<PhotoValue> photoValues = toPhotoValues(toPhotoMap(history), request.photos());
        history.update(request.title(), request.detail(), request.historyDate(), null);
        history.replaceLinks(toLinkValues(request.links()));
        history.replacePhotos(photoValues);
        markRemovedFilesDeleted(oldFileIds, collectPhotoValueFileIds(photoValues));

        SiteHistory saved = siteHistoryRepository.save(history);
        log.info("사이트 연혁 수정 완료 - id={}", saved.getId());
        return SiteHistoryResponse.from(saved);
    }

    @Transactional
    public void updateAdminHistory(
        Long historyId,
        String title,
        String detail,
        LocalDate historyDate,
        Integer sortOrder,
        String linksText,
        List<SiteHistoryPhotoRequest> photos
    ) {
        SiteHistory history = findHistory(historyId);
        Set<UUID> oldFileIds = collectFileIds(history);
        List<PhotoValue> photoValues = toPhotoValues(toPhotoMap(history), photos);
        history.update(title, detail, historyDate, sortOrder);
        history.replaceLinks(toLinkValues(parseLinkRequests(linksText)));
        history.replacePhotos(photoValues);
        markRemovedFilesDeleted(oldFileIds, collectPhotoValueFileIds(photoValues));
        siteHistoryRepository.save(history);
    }

    @Transactional
    public void deleteHistory(Long historyId) {
        SiteHistory history = findHistory(historyId);
        Set<UUID> fileIds = collectFileIds(history);
        siteHistoryRepository.delete(history);
        markFilesDeleted(fileIds);
        log.info("사이트 연혁 삭제 완료 - id={}", historyId);
    }

    private SiteHistory findHistory(Long historyId) {
        return siteHistoryRepository.findById(historyId)
            .orElseThrow(() -> new ResourceNotFoundException(SiteContentErrorCode.HISTORY_NOT_FOUND, historyId));
    }

    private List<PhotoValue> toPhotoValues(Map<Long, File> existingPhotoFiles, List<SiteHistoryPhotoRequest> photos) {
        if (photos == null) {
            return List.of();
        }
        return photos.stream()
            .map(photo -> new PhotoValue(resolvePhotoFile(existingPhotoFiles, photo), photo.src(), photo.alt()))
            .toList();
    }

    private List<LinkValue> toLinkValues(List<SiteHistoryLinkRequest> links) {
        if (links == null) {
            return List.of();
        }
        return links.stream()
            .map(link -> toLinkValue(link.label(), link.href()))
            .toList();
    }

    private List<SiteHistoryLinkRequest> parseLinkRequests(String linksText) {
        if (linksText == null || linksText.isBlank()) {
            return null;
        }
        return linksText.lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .map(line -> {
                String[] parts = line.split("\\|", 2);
                if (parts.length < 2) {
                    throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "링크는 label|href 형식으로 입력해야 합니다.");
                }
                return new SiteHistoryLinkRequest(parts[0].trim(), parts[1].trim());
            })
            .toList();
    }

    private LinkValue toLinkValue(String label, String href) {
        if (label == null || label.isBlank()) {
            throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "링크 라벨은 필수입니다.");
        }
        if (label.length() > LINK_LABEL_MAX_LENGTH) {
            throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "링크 라벨은 120자를 초과할 수 없습니다.");
        }
        if (href == null || href.isBlank()) {
            throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "링크 URL은 필수입니다.");
        }
        return new LinkValue(label.trim(), href.trim());
    }

    private File resolvePhotoFile(Map<Long, File> existingPhotoFiles, SiteHistoryPhotoRequest photo) {
        if (photo.fileId() != null) {
            return fileRepository.findByIdAndIsDeletedFalse(photo.fileId())
                .orElseThrow(() -> new ResourceNotFoundException(CommonErrorCode.RESOURCE_NOT_FOUND, "파일을 찾을 수 없습니다."));
        }
        if (photo.id() != null) {
            return existingPhotoFiles.get(photo.id());
        }
        return null;
    }

    private Map<Long, File> toPhotoMap(SiteHistory history) {
        return history.getPhotos().stream()
            .filter(photo -> photo.getId() != null)
            .filter(photo -> photo.getFile() != null)
            .collect(Collectors.toMap(SiteHistoryPhoto::getId, SiteHistoryPhoto::getFile));
    }

    private Set<UUID> collectFileIds(SiteHistory history) {
        return history.getPhotos().stream()
            .map(SiteHistoryPhoto::getFile)
            .filter(Objects::nonNull)
            .map(File::getId)
            .collect(Collectors.toSet());
    }

    private Set<UUID> collectPhotoValueFileIds(List<PhotoValue> photos) {
        return photos.stream()
            .map(PhotoValue::file)
            .filter(Objects::nonNull)
            .map(File::getId)
            .collect(Collectors.toSet());
    }

    private void markRemovedFilesDeleted(Set<UUID> oldFileIds, Set<UUID> currentFileIds) {
        oldFileIds.removeAll(currentFileIds);
        markFilesDeleted(oldFileIds);
    }

    private void markFilesDeleted(Set<UUID> fileIds) {
        if (fileIds.isEmpty()) {
            return;
        }
        fileRepository.findAllByIdInAndIsDeletedFalse(fileIds).forEach(File::delete);
    }

    public record SiteHistoryAdminRow(
        Long id,
        String title,
        LocalDate historyDate,
        String detail,
        String linksText,
        int sortOrder,
        String photosText
    ) {
        public static SiteHistoryAdminRow from(SiteHistory history) {
            String linksText = history.getLinks().stream()
                .sorted(Comparator
                    .comparingInt(SiteHistoryLink::getSortOrder)
                    .thenComparing(SiteHistoryLink::getId))
                .map(link -> link.getLabel() + "|" + link.getHref())
                .collect(Collectors.joining("\n"));
            String photosText = history.getPhotos().stream()
                .sorted(Comparator
                    .comparingInt(SiteHistoryPhoto::getSortOrder)
                    .thenComparing(SiteHistoryPhoto::getId))
                .map(photo -> formatPhotoText(photo))
                .collect(Collectors.joining("\n"));
            return new SiteHistoryAdminRow(
                history.getId(),
                history.getTitle(),
                history.getHistoryDate(),
                history.getDetail(),
                linksText,
                history.getSortOrder(),
                photosText
            );
        }

        private static String formatPhotoText(SiteHistoryPhoto photo) {
            String alt = photo.getAlt() == null ? "" : photo.getAlt();
            if (photo.getFile() == null) {
                return photo.getSrc() + "|" + alt;
            }
            return photo.getFile().getId() + "|" + photo.getSrc() + "|" + alt;
        }
    }
}
