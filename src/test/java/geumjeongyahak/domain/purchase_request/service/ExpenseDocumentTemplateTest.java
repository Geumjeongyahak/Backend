package geumjeongyahak.domain.purchase_request.service;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;

class ExpenseDocumentTemplateTest {

    private static final Configure RENDER_CONFIG = Configure.builder()
        .bind("itemRows", new LoopRowTableRenderPolicy(true))
        .build();

    @Test
    void rendersDynamicItemRowsAndThreeApprovalSlots() throws IOException {
        Map<String, Object> data = Map.ofEntries(
            entry("detailProject", "세부사업"),
            entry("budgetItem", "세부항목"),
            entry("budgetDetail", "예산요약"),
            entry("draftAmount", "10,000원"),
            entry("itemTotalQuantity", "5"),
            entry("itemTotalAmount", "10,000원"),
            entry("itemRows",
            List.of(
                itemRow("1", "품목1"),
                itemRow("2", "품목2"),
                itemRow("3", "품목3"),
                itemRow("4", "품목4"),
                itemRow("5", "품목5")
            ))
        );

        byte[] rendered = render(data);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(rendered))) {
            assertThat(document.getTables().get(2).getRow(1).getTableCells())
                .extracting(cell -> cell.getText().replace("\n", ""))
                .containsExactly("순번", "내용", "규격", "수량", "예상단가", "예상금액");
            assertThat(document.getTables().get(1).getRows()).hasSize(4);
            assertThat(document.getTables().get(1).getRow(1).getTableCells())
                .extracting(cell -> cell.getText().replace("\n", ""))
                .containsExactly("순번", "세부사업", "세부항목", "산출내역", "품의금액", "예산잔액", "사업잔액");
            assertThat(document.getTables().get(4).getRow(0).getTableCells()).hasSize(11);
            assertThat(document.getTables().get(6).getRow(0).getTableCells()).hasSize(11);

            String text = document.getTables()
                .stream()
                .flatMap(table -> table.getRows().stream())
                .flatMap(row -> row.getTableCells().stream())
                .map(cell -> cell.getText())
                .reduce("", String::concat);

            assertThat(text)
                .contains("예산요약", "품목1", "품목5")
                .doesNotContain(
                    "{{itemRows}}",
                    "{{resolutionAmount}}",
                    "[description]"
                );
        }
    }

    private byte[] render(Map<String, Object> data) throws IOException {
        ClassPathResource templateResource = new ClassPathResource(ExpenseDocumentService.TEMPLATE_PATH);
        try (
            XWPFTemplate template = XWPFTemplate.compile(templateResource.getInputStream(), RENDER_CONFIG).render(data);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
        ) {
            template.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static Map<String, Object> itemRow(String no, String description) {
        return Map.of(
            "no", no,
            "description", description,
            "spec", "",
            "quantity", "1",
            "unitPrice", "",
            "amount", ""
        );
    }
}
