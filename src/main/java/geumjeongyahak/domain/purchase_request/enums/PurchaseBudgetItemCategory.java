package geumjeongyahak.domain.purchase_request.enums;

public enum PurchaseBudgetItemCategory {
    TRANSPORTATION("교통비"),
    TEXTBOOK("교재비"),
    PROGRAM_OPERATION("프로그램추진비"),
    RENTAL("임차료"),
    PUBLIC_RELATIONS("홍보비"),
    OTHER_OPERATING("기타 운영비"),
    DIRECT_INPUT("직접 입력");

    private final String displayName;

    PurchaseBudgetItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
