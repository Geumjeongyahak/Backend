package geumjeongyahak.domain.purchase_request.enums;

public enum PurchaseCalculationDetail {
    UNPAID_INSTRUCTOR_TRANSPORTATION("무급강사교통비"),
    UNPAID_ADMIN_TRANSPORTATION("무급행정담당자 교통비"),
    COMMERCIAL_TEXTBOOK("시중교재"),
    BOUND_TEXTBOOK("제본교재"),
    PICNIC_MEAL("(소풍)식비"),
    EVENT_SUPPLIES("행사물품"),
    BANNER("현수막"),
    REFRESHMENTS("다과비"),
    POSTER("포스터"),
    NEWSLETTER("소식지"),
    SIGN_BANNER_STICKER_PRODUCTION("입간판 및 배너, 스티커 제작"),
    TRANSFER_FEE("이체 수수료"),
    OFFICE_SUPPLIES("사무용품비"),
    COMMUNICATION("통신비"),
    ELECTRICITY("전기비"),
    WATER("수도세"),
    WATER_PURIFIER_FILTER_REPLACEMENT("정수기 필터교체"),
    CLEANING_SUPPLIES("청소용품"),
    PRINTER_TONER("프린트토너"),
    DIRECT_INPUT("직접 입력");

    private final String displayName;

    PurchaseCalculationDetail(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
