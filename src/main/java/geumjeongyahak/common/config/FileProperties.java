package geumjeongyahak.common.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.file")
public class FileProperties {

    private final Upload image = new Upload();
    private final Upload document = new Upload();

    @Getter
    @Setter
    public static class Upload {
        private DataSize maxSize = DataSize.ofMegabytes(10);
        private List<String> allowedMimeTypes = new ArrayList<>();
    }
}
