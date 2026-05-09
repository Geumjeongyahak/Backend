package geumjeongyahak.domain.file.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.file.cleanup")
public class FileCleanupProperties {

    @Min(1)
    private long retentionDays = 7;

    @Min(1)
    private int chunkSize = 100;

    @NotBlank
    private String cron = "0 0 3 * * *";
}
