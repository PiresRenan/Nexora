package com.nexora.infrastructure.config;

import com.nexora.domain.port.StoragePort;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura o MinIO client quando {@code nexora.storage.endpoint} está definido.
 *
 * O padrão {@code @ConditionalOnProperty} garante que:
 *  - Em produção/dev com MinIO: client é criado → MinioStorageAdapter ativado
 *  - Em testes sem MinIO: client não é criado → StorageAdapterConfig fornece no-op
 *
 * Os buckets são criados na inicialização via {@code @PostConstruct}
 * no MinioStorageAdapter.
 */
@Configuration
@ConditionalOnProperty(name = "nexora.storage.endpoint")
@EnableConfigurationProperties(StorageProperties.class)
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Bean
    public MinioClient minioClient(StorageProperties props) {
        log.info("Configuring MinIO client → endpoint={} secure={}",
                props.endpoint(), props.secure());

        var builder = MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey());

        return builder.build();
    }

    /**
     * Garante que os buckets necessários existem ao iniciar a aplicação.
     * Executado após todos os beans serem criados.
     */
    @Bean
    public BucketInitializer bucketInitializer(StoragePort storagePort,
                                               StorageProperties props) {
        return new BucketInitializer(storagePort, props);
    }

    /** Componente de inicialização de buckets. */
    public static class BucketInitializer {
        private static final Logger log = LoggerFactory.getLogger(BucketInitializer.class);

        public BucketInitializer(StoragePort storagePort, StorageProperties props) {
            log.info("Initializing storage buckets...");
            storagePort.ensureBucketExists(props.imagesBucket());
            storagePort.ensureBucketExists(props.documentsBucket());
            log.info("Storage buckets ready: images='{}' documents='{}'",
                    props.imagesBucket(), props.documentsBucket());
        }
    }
}