package geumjeongyahak.domain.sitecontent.service;

import geumjeongyahak.common.exception.BadRequestException;
import geumjeongyahak.common.exception.CommonErrorCode;
import geumjeongyahak.common.exception.DuplicateResourceException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.sitecontent.v1.dto.request.CreateSiteContentClassRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.CreateSiteContentDepartmentRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.UpdateSiteContentClassRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.request.UpdateSiteContentDepartmentRequest;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteContentClassResponse;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteContentClassesResponse;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteContentDepartmentResponse;
import geumjeongyahak.domain.sitecontent.v1.dto.response.SiteContentDepartmentsResponse;
import geumjeongyahak.domain.sitecontent.entity.SiteContent;
import geumjeongyahak.domain.sitecontent.enums.SiteContentGroup;
import geumjeongyahak.domain.sitecontent.enums.SiteContentType;
import geumjeongyahak.domain.sitecontent.exception.SiteContentErrorCode;
import geumjeongyahak.domain.sitecontent.repository.SiteContentRepository;
import geumjeongyahak.domain.sitecontent.entity.SiteContentItem;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteContentService {

    private static final List<SiteContentType> DEPARTMENT_PAGE_TYPES = List.of(
        SiteContentType.PRINCIPAL,
        SiteContentType.DEPARTMENT
    );

    private final SiteContentRepository siteContentRepository;

    public SiteContentDepartmentsResponse getDepartmentPageContents() {
        List<SiteContent> contents = siteContentRepository
            .findAllByContentTypeInOrderBySortOrderAscIdAsc(DEPARTMENT_PAGE_TYPES);

        SiteContentDepartmentResponse principal = contents.stream()
            .filter(content -> content.getContentType() == SiteContentType.PRINCIPAL)
            .map(SiteContentDepartmentResponse::from)
            .findFirst()
            .orElseGet(SiteContentDepartmentResponse::defaultPrincipal);
        List<SiteContentDepartmentResponse> departments = contents.stream()
            .filter(content -> content.getContentType() == SiteContentType.DEPARTMENT)
            .map(SiteContentDepartmentResponse::from)
            .toList();

        return new SiteContentDepartmentsResponse(principal, departments);
    }

    public SiteContentClassesResponse getClassPageContents() {
        List<SiteContent> contents = siteContentRepository.findAllByContentTypeInOrderBySortOrderAscIdAsc(
            List.of(SiteContentType.CLASSROOM)
        );

        return new SiteContentClassesResponse(
            toClassResponseByGroup(contents, SiteContentGroup.WEEKDAY),
            toClassResponseByGroup(contents, SiteContentGroup.WEEKEND_MORNING),
            toClassResponseByGroup(contents, SiteContentGroup.WEEKEND_AFTERNOON)
        );
    }

    public List<SiteContentAdminRow> getAdminContents() {
        return siteContentRepository.findAllByContentTypeInOrderBySortOrderAscIdAsc(
                List.of(SiteContentType.PRINCIPAL, SiteContentType.DEPARTMENT, SiteContentType.CLASSROOM))
            .stream()
            .map(SiteContentAdminRow::from)
            .toList();
    }

    public SiteContentAdminRow getAdminContent(Long contentId) {
        return SiteContentAdminRow.from(findSiteContent(contentId));
    }

    @Transactional
    public Long createAdminContent(
        SiteContentType contentType,
        Long refId,
        String title,
        String name,
        SiteContentGroup group,
        Integer sortOrder,
        List<String> items
    ) {
        validateContentTypeAndGroup(contentType, group);
        validateSinglePrincipalCreatable(contentType);

        SiteContent siteContent = SiteContent.builder()
            .contentType(contentType)
            .refId(refId)
            .title(title)
            .name(name)
            .group(group)
            .sortOrder(sortOrder)
            .build();
        siteContent.replaceItems(items);
        return siteContentRepository.save(siteContent).getId();
    }

    @Transactional
    public void updateAdminContent(
        Long contentId,
        SiteContentType contentType,
        Long refId,
        String title,
        String name,
        SiteContentGroup group,
        Integer sortOrder,
        List<String> items
    ) {
        validateContentTypeAndGroup(contentType, group);
        validateSinglePrincipalUpdatable(contentId, contentType);

        SiteContent siteContent = findSiteContent(contentId);
        siteContent.update(contentType, refId, title, name, group, sortOrder);
        siteContent.replaceItems(items);
        siteContentRepository.save(siteContent);
    }

    @Transactional
    public void deleteAdminContent(Long contentId) {
        siteContentRepository.delete(findSiteContent(contentId));
    }

    @Transactional
    public SiteContentDepartmentResponse createDepartmentPageContent(
        CreateSiteContentDepartmentRequest request
    ) {
        SiteContent siteContent = SiteContent.builder()
            .contentType(SiteContentType.DEPARTMENT)
            .title(request.title())
            .name(request.name())
            .build();
        siteContent.replaceItems(request.responsibilities());

        SiteContent saved = siteContentRepository.save(siteContent);
        log.info("사이트 부서/교장 콘텐츠 생성 완료 - id={}, type={}", saved.getId(), saved.getContentType());
        return SiteContentDepartmentResponse.from(saved);
    }

    @Transactional
    public SiteContentDepartmentResponse updateDepartmentPageContent(
        Long contentId,
        UpdateSiteContentDepartmentRequest request
    ) {
        SiteContent siteContent = findSiteContent(contentId);
        validateDepartmentPageType(siteContent.getContentType());

        siteContent.update(
            siteContent.getContentType(),
            siteContent.getRefId(),
            request.title(),
            request.name(),
            null,
            null
        );
        siteContent.replaceItems(request.responsibilities());

        SiteContent saved = siteContentRepository.save(siteContent);
        log.info("사이트 부서/교장 콘텐츠 수정 완료 - id={}, type={}", saved.getId(), saved.getContentType());
        return SiteContentDepartmentResponse.from(saved);
    }

    @Transactional
    public void deleteDepartmentPageContent(Long contentId) {
        SiteContent siteContent = findSiteContent(contentId);
        validateDepartmentPageType(siteContent.getContentType());
        if (siteContent.getContentType() == SiteContentType.PRINCIPAL) {
            throw new BadRequestException(CommonErrorCode.INVALID_INPUT, "교장 콘텐츠는 삭제할 수 없습니다.");
        }
        siteContentRepository.delete(siteContent);
        log.info("사이트 부서/교장 콘텐츠 삭제 완료 - id={}", contentId);
    }

    @Transactional
    public SiteContentClassResponse createClassPageContent(CreateSiteContentClassRequest request) {
        SiteContent siteContent = SiteContent.builder()
            .contentType(SiteContentType.CLASSROOM)
            .title(request.name())
            .group(resolveClassGroup(request.groupId()))
            .build();
        siteContent.replaceItems(request.description());

        SiteContent saved = siteContentRepository.save(siteContent);
        log.info("사이트 반 콘텐츠 생성 완료 - id={}, group={}", saved.getId(), saved.getGroup());
        return SiteContentClassResponse.from(saved);
    }

    @Transactional
    public SiteContentClassResponse updateClassPageContent(
        Long contentId,
        UpdateSiteContentClassRequest request
    ) {
        SiteContent siteContent = findSiteContent(contentId);
        if (siteContent.getContentType() != SiteContentType.CLASSROOM) {
            throw new ResourceNotFoundException(SiteContentErrorCode.CLASS_CONTENT_NOT_FOUND, contentId);
        }

        siteContent.update(
            SiteContentType.CLASSROOM,
            siteContent.getRefId(),
            request.name(),
            null,
            resolveClassGroup(request.groupId()),
            null
        );
        siteContent.replaceItems(request.description());

        SiteContent saved = siteContentRepository.save(siteContent);
        log.info("사이트 반 콘텐츠 수정 완료 - id={}, group={}", saved.getId(), saved.getGroup());
        return SiteContentClassResponse.from(saved);
    }

    @Transactional
    public void deleteClassPageContent(Long contentId) {
        SiteContent siteContent = findSiteContent(contentId);
        if (siteContent.getContentType() != SiteContentType.CLASSROOM) {
            throw new ResourceNotFoundException(SiteContentErrorCode.CLASS_CONTENT_NOT_FOUND, contentId);
        }
        siteContentRepository.delete(siteContent);
        log.info("사이트 반 콘텐츠 삭제 완료 - id={}", contentId);
    }

    private SiteContent findSiteContent(Long contentId) {
        return siteContentRepository.findById(contentId)
            .orElseThrow(() -> new ResourceNotFoundException(SiteContentErrorCode.SITE_CONTENT_NOT_FOUND, contentId));
    }

    private void validateDepartmentPageType(SiteContentType contentType) {
        if (!DEPARTMENT_PAGE_TYPES.contains(contentType)) {
            throw new ResourceNotFoundException(SiteContentErrorCode.DEPARTMENT_CONTENT_NOT_FOUND);
        }
    }

    private void validateContentTypeAndGroup(SiteContentType contentType, SiteContentGroup group) {
        if (contentType == SiteContentType.CLASSROOM && group == null) {
            throw new ResourceNotFoundException(SiteContentErrorCode.CLASS_CONTENT_NOT_FOUND);
        }
        if (contentType != SiteContentType.CLASSROOM && group != null) {
            throw new ResourceNotFoundException(SiteContentErrorCode.DEPARTMENT_CONTENT_NOT_FOUND);
        }
    }

    private void validateSinglePrincipalCreatable(SiteContentType contentType) {
        if (contentType == SiteContentType.PRINCIPAL && siteContentRepository.existsByContentType(contentType)) {
            throw new DuplicateResourceException(SiteContentErrorCode.PRINCIPAL_ALREADY_EXISTS);
        }
    }

    private void validateSinglePrincipalUpdatable(Long contentId, SiteContentType contentType) {
        if (contentType == SiteContentType.PRINCIPAL
            && siteContentRepository.existsByContentTypeAndIdNot(contentType, contentId)) {
            throw new DuplicateResourceException(SiteContentErrorCode.PRINCIPAL_ALREADY_EXISTS);
        }
    }

    private List<SiteContentClassResponse> toClassResponseByGroup(
        List<SiteContent> contents,
        SiteContentGroup group
    ) {
        return contents.stream()
            .filter(content -> content.getGroup() == group)
            .map(SiteContentClassResponse::from)
            .toList();
    }

    private SiteContentGroup resolveClassGroup(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw new BadRequestException(CommonErrorCode.MISSING_REQUIRED_FIELD, "반 그룹은 필수입니다.");
        }

        return switch (groupId.trim()) {
            case "weekday", "WEEKDAY" -> SiteContentGroup.WEEKDAY;
            case "weekendMorning", "WEEKEND_MORNING" -> SiteContentGroup.WEEKEND_MORNING;
            case "weekendAfternoon", "WEEKEND_AFTERNOON" -> SiteContentGroup.WEEKEND_AFTERNOON;
            default -> throw new BadRequestException(
                CommonErrorCode.INVALID_INPUT,
                "반 그룹은 weekday, weekendMorning, weekendAfternoon 중 하나여야 합니다."
            );
        };
    }

    public record SiteContentAdminRow(
        Long id,
        SiteContentType contentType,
        Long refId,
        String title,
        String name,
        SiteContentGroup group,
        int sortOrder,
        String itemsText
    ) {
        public static SiteContentAdminRow from(SiteContent content) {
            String itemsText = content.getItems().stream()
                .sorted(Comparator
                    .comparingInt(SiteContentItem::getSortOrder)
                    .thenComparing(SiteContentItem::getId))
                .map(SiteContentItem::getContent)
                .collect(Collectors.joining("\n"));
            return new SiteContentAdminRow(
                content.getId(),
                content.getContentType(),
                content.getRefId(),
                content.getTitle(),
                content.getName(),
                content.getGroup(),
                content.getSortOrder(),
                itemsText
            );
        }
    }
}
