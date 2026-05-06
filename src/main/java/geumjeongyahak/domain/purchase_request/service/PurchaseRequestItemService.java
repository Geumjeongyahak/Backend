package geumjeongyahak.domain.purchase_request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.common.exception.ResourceNotFoundException;
import geumjeongyahak.domain.file.service.FileProxyService;
import geumjeongyahak.domain.file.service.ImageUploadService;
import geumjeongyahak.domain.file.v1.dto.response.FileUploadResponse;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequest;
import geumjeongyahak.domain.purchase_request.entity.PurchaseRequestItem;
import geumjeongyahak.domain.purchase_request.exception.PurchaseRequestErrorCode;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestItemRepository;
import geumjeongyahak.domain.purchase_request.repository.PurchaseRequestRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseRequestItemService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository purchaseRequestItemRepository;
    private final FileProxyService fileProxyService;
    private final ImageUploadService imageUploadService;

    @Transactional
    public FileUploadResponse uploadItemReceipt(
        Long requesterId, Long requestId, Long itemId, MultipartFile file, boolean isAdmin
    ) {
        log.debug("구입 항목 영수증 업로드 (requestId={}, itemId={})", requestId, itemId);

        PurchaseRequest purchaseRequest = purchaseRequestRepository.findById(requestId)
            .orElseThrow(() -> new ResourceNotFoundException(PurchaseRequestErrorCode.NOT_FOUND, requestId));

        if (!isAdmin && !purchaseRequest.getRequestedBy().getId().equals(requesterId)) {
            throw new BusinessException(PurchaseRequestErrorCode.FORBIDDEN);
        }

        PurchaseRequestItem item = purchaseRequestItemRepository.findById(itemId)
            .filter(i -> i.getPurchaseRequest().getId().equals(requestId))
            .orElseThrow(() -> new ResourceNotFoundException(PurchaseRequestErrorCode.ITEM_NOT_FOUND, itemId));

        FileUploadResponse response = imageUploadService.uploadPurchaseItemImage(file);
        item.assignReceiptFile(fileProxyService.getReferenceById(response.fileId()));

        log.debug("영수증 업로드 완료 (itemId={}, fileId={})", itemId, response.fileId());
        return response;
    }
}
