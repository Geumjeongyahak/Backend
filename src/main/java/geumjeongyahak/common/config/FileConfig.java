package geumjeongyahak.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import geumjeongyahak.domain.file.config.DriveUploadProperties;

@Configuration
@EnableConfigurationProperties({FileProperties.class, DriveUploadProperties.class})
public class FileConfig {
}
