package geumjeongyahak.domain.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
    public FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        FirebaseOptions.Builder builder = FirebaseOptions.builder()
            .setCredentials(resolveCredentials(properties));

        if (StringUtils.hasText(properties.projectId())) {
            builder.setProjectId(properties.projectId());
        }

        if (FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.initializeApp(builder.build());
        }
        return FirebaseApp.getInstance();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    private GoogleCredentials resolveCredentials(FirebaseProperties properties) throws IOException {
        if (!StringUtils.hasText(properties.credentialsBase64())) {
            return GoogleCredentials.getApplicationDefault();
        }

        byte[] decoded = Base64.getDecoder().decode(properties.credentialsBase64());
        return GoogleCredentials.fromStream(new ByteArrayInputStream(decoded));
    }
}
