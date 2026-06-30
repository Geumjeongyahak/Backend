package geumjeongyahak.domain.purchase_request.enums;

public enum PurchaseRequestStatus {
    PENDING,    // 결재 신청
    APPROVED,   // 결재 승인
    PURCHASED,  // 구매 완료 보고
    CONFIRMED,  // 결재 확인 완료
    REJECTED    // 반려
}
