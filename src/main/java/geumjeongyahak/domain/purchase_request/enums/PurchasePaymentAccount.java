package geumjeongyahak.domain.purchase_request.enums;

public enum PurchasePaymentAccount {
    NATIONAL_SUBSIDY_04("국비04"),
    DISTRICT_BUDGET_01("구비01"),
    DISTRICT_BUDGET_08("구비08");

    private final String displayName;

    PurchasePaymentAccount(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
