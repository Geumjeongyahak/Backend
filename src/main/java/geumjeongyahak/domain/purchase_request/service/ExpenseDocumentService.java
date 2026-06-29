package geumjeongyahak.domain.purchase_request.service;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import geumjeongyahak.common.exception.BusinessException;
import geumjeongyahak.domain.purchase_request.exception.PurchaseRequestErrorCode;
import geumjeongyahak.domain.purchase_request.v1.dto.request.GenerateExpenseDocumentRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ExpenseDocumentService {

    public static final String DOCX_CONTENT_TYPE =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String TEMPLATE_PATH = "templates/docx/expense-document-template.docx";

    public byte[] generate(Long purchaseRequestId, GenerateExpenseDocumentRequest request) {
        log.debug("지출증빙서류 생성 요청 (purchaseRequestId={})", purchaseRequestId);
        return loadTemplate();
    }

    private byte[] loadTemplate() {
        ClassPathResource template = new ClassPathResource(TEMPLATE_PATH);
        if (!template.exists()) {
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_TEMPLATE_NOT_FOUND);
        }

        try {
            return StreamUtils.copyToByteArray(template.getInputStream());
        } catch (IOException e) {
            log.error("지출증빙서류 템플릿 로드 실패 (path={})", TEMPLATE_PATH, e);
            throw new BusinessException(PurchaseRequestErrorCode.EXPENSE_DOCUMENT_TEMPLATE_READ_FAILED);
        }
    }
}
