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
        .bind("transactionRows", new LoopRowTableRenderPolicy(true))
        .build();

    @Test
    void rendersDynamicItemAndTransactionRows() throws IOException {
        Map<String, Object> data = Map.ofEntries(
            entry("budgetProject", "세부사업"),
            entry("budgetItem", "세부항목"),
            entry("budgetDetail", "예산요약"),
            entry("budgetAmount", "10,000원"),
            entry("budgetBalance", "90,000원"),
            entry("projectBalance", "900,000원"),
            entry("budgetTotal", "10,000원"),
            entry("budgetTotalBalance", "90,000원"),
            entry("projectTotalBalance", "900,000원"),
            entry("itemRows",
            List.of(
                itemRow("1", "품목1"),
                itemRow("2", "품목2"),
                itemRow("3", "품목3"),
                itemRow("4", "품목4"),
                itemRow("5", "품목5")
            )),
            entry("transactionRows",
            List.of(
                transactionRow("1", "거래1"),
                transactionRow("2", "거래2"),
                transactionRow("3", "거래3"),
                transactionRow("4", "거래4")
            ))
        );

        byte[] rendered = render(data);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(rendered))) {
            assertThat(document.getTables().get(2).getRow(0).getTableCells())
                .extracting(cell -> cell.getText().replace("\n", ""))
                .containsExactly("순번", "내용", "규격", "예상단가", "수량", "예상금액");
            assertThat(document.getTables().get(1).getRows()).hasSize(3);
            assertThat(document.getTables().get(1).getRow(0).getTableCells())
                .extracting(cell -> cell.getText().replace("\n", ""))
                .containsExactly("순번", "세부사업", "세부항목", "산출내역", "품의금액", "예산잔액", "사업잔액");

            String text = document.getTables()
                .stream()
                .flatMap(table -> table.getRows().stream())
                .flatMap(row -> row.getTableCells().stream())
                .map(cell -> cell.getText())
                .reduce("", String::concat);

            assertThat(text)
                .contains("예산요약", "90,000원", "900,000원", "품목1", "품목5", "거래1", "거래4")
                .doesNotContain(
                    "{{itemRows}}",
                    "{{transactionRows}}",
                    "{{budgetProject}}",
                    "[description]",
                    "[detail]"
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

    private static Map<String, Object> transactionRow(String no, String detail) {
        return Map.of(
            "no", no,
            "date", "2026. 06. 30.",
            "detail", detail,
            "amount", "1,000원",
            "note", ""
        );
    }
}
