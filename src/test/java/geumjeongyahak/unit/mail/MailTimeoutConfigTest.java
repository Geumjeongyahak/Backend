package geumjeongyahak.unit.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class MailTimeoutConfigTest {

    @Test
    void send_toSilentSmtpServer_failsWithinConfiguredTimeout() throws Exception {
        // 연결은 받지만 인사말(220)을 보내지 않는 SMTP 서버
        try (ServerSocket silentSmtp = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            List<Socket> accepted = new CopyOnWriteArrayList<>();
            Thread acceptor = Thread.ofVirtual().start(() -> {
                try {
                    while (true) {
                        accepted.add(silentSmtp.accept());
                    }
                } catch (IOException closed) {
                    // 테스트가 끝나 소켓이 닫혔다
                }
            });

            contextRunnerWithApplicationYml()
                .withPropertyValues(
                    "spring.mail.host=127.0.0.1",
                    "spring.mail.port=" + silentSmtp.getLocalPort(),
                    "MAIL_SMTP_AUTH=false",
                    "MAIL_SMTP_STARTTLS_ENABLE=false",
                    "MAIL_SMTP_TIMEOUT_MS=500"
                )
                .run(context -> {
                    JavaMailSender mailSender = context.getBean(JavaMailSender.class);
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom("from@test.com");
                    message.setTo("to@test.com");
                    message.setText("본문");

                    assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                        assertThatThrownBy(() -> mailSender.send(message)).isInstanceOf(MailException.class)
                    );
                });

            assertThat(accepted).isNotEmpty();
            acceptor.interrupt();
        }
    }

    // application.yml 의 spring.mail 설정을 그대로 읽어 JavaMailSender 를 만든다
    private ApplicationContextRunner contextRunnerWithApplicationYml() throws IOException {
        List<PropertySource<?>> applicationYml = new YamlPropertySourceLoader()
            .load("application.yml", new ClassPathResource("application.yml"));
        return new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .withInitializer(context -> applicationYml.forEach(
                source -> context.getEnvironment().getPropertySources().addLast(source)
            ));
    }
}
