package geumjeongyahak;

import java.util.TimeZone;

import geumjeongyahak.common.config.AppConfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.cdimascio.dotenv.Dotenv;

@EnableJpaAuditing
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class GeumjeongyahakApiApplication {

	// 서버 OS 시간대와 상관없이 한국 시간으로 동작한다. 테스트는 main() 을 안 부르고 이 클래스만 읽으므로 초기화 블록에 둔다.
	static {
		TimeZone.setDefault(TimeZone.getTimeZone(AppConfig.ZONE_ID));
	}

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		SpringApplication.run(GeumjeongyahakApiApplication.class, args);
	}

}
