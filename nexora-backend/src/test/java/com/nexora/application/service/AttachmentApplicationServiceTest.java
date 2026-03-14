package com.nexora.application.service;

import com.nexora.domain.exception.BusinessRuleException;
import com.nexora.domain.exception.ResourceNotFoundException;
import com.nexora.domain.model.*;
import com.nexora.domain.port.StoragePort;
import com.nexora.domain.repository.*;
import com.nexora.infrastructure.config.StorageProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentApplicationService")
class AttachmentApplicationServiceTest {

    @Mock AttachmentRepository attachmentRepository;
    @Mock ProductRepository    productRepository;
    @Mock UserRepository       userRepository;
    @Mock StoragePort          storagePort;

    // StorageProperties não pode ser mockado com @Mock pois é um record —
    // instanciamos com valores de teste
    StorageProperties storageProperties = new StorageProperties(
            "http://localhost:9000", "nexora", "nexora123",
            false, "test-images", "test-documents"
    );

    AttachmentApplicationService service;

    private Product product;
    private User    user;

    @BeforeEach
    void setUp() {
        service = new AttachmentApplicationService(
                attachmentRepository, productRepository, userRepository,
                storagePort, storageProperties
        );
        product = Product.create("Notebook", "desc", "NB-001",
                Money.brl("999.00"), StockQuantity.of(10));
        user = User.create("Ana", "ana@test.com", "$2a$hash", UserRole.CUSTOMER);
    }

    // ─── uploadProductImage ────────────────────────────────────────────────

    @Nested
    @DisplayName("uploadProductImage")
    class UploadProductImage {

        @Test
        @DisplayName("Deve fazer upload de imagem JPEG válida")
        void shouldUploadValidJpeg() {
            var file = new MockMultipartFile("file", "photo.jpg",
                    "image/jpeg", new byte[1024]);

            given(productRepository.findById(product.getId()))
                    .willReturn(Optional.of(product));
            given(attachmentRepository.findPrimaryByEntityAndCategory(any(), any(), any()))
                    .willReturn(Optional.empty());
            willDoNothing().given(storagePort).store(any(), any(), any(), anyLong(), any());
            given(attachmentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(storagePort.generatePresignedUrl(any(), any(), any()))
                    .willReturn("https://minio/test/photo.jpg?sig=abc");

            var result = service.uploadProductImage(product.getId(), file, true, user.getId());

            assertThat(result.category()).isEqualTo(AttachmentCategory.PRODUCT_IMAGE);
            assertThat(result.primary()).isTrue();
            assertThat(result.contentType()).isEqualTo("image/jpeg");
            assertThat(result.url()).contains("https://minio");
            then(storagePort).should().store(
                    eq("test-images"), any(), any(), eq(1024L), eq("image/jpeg")
            );
        }

        @Test
        @DisplayName("Deve desmarcar imagem principal anterior ao subir nova primária")
        void shouldUnsetPreviousPrimaryWhenUploadingNewPrimary() {
            var oldPrimary = Attachment.create(
                    EntityType.PRODUCT, product.getId(), AttachmentCategory.PRODUCT_IMAGE,
                    "old.jpg", "products/old.jpg", "test-images",
                    "image/jpeg", 512, true, user.getId()
            );
            var file = new MockMultipartFile("file", "new.jpg",
                    "image/jpeg", new byte[2048]);

            given(productRepository.findById(product.getId()))
                    .willReturn(Optional.of(product));
            given(attachmentRepository.findPrimaryByEntityAndCategory(
                    EntityType.PRODUCT, product.getId(), AttachmentCategory.PRODUCT_IMAGE))
                    .willReturn(Optional.of(oldPrimary));
            given(attachmentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            willDoNothing().given(storagePort).store(any(), any(), any(), anyLong(), any());
            given(storagePort.generatePresignedUrl(any(), any(), any()))
                    .willReturn("https://minio/new.jpg");

            service.uploadProductImage(product.getId(), file, true, user.getId());

            // O attachment antigo deve ter primary=false salvo
            then(attachmentRepository).should().save(argThat(a -> !a.isPrimary()));
        }

        @Test
        @DisplayName("Deve rejeitar tipo de arquivo inválido (PDF para imagem)")
        void shouldRejectInvalidMimeType() {
            var file = new MockMultipartFile("file", "doc.pdf",
                    "application/pdf", new byte[1024]);

            given(productRepository.findById(product.getId()))
                    .willReturn(Optional.of(product));

            assertThatThrownBy(() ->
                    service.uploadProductImage(product.getId(), file, false, user.getId())
            ).isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid image type");
        }

        @Test
        @DisplayName("Deve rejeitar imagem maior que 5 MB")
        void shouldRejectOversizedImage() {
            byte[] largeContent = new byte[6 * 1024 * 1024]; // 6 MB
            var file = new MockMultipartFile("file", "large.jpg",
                    "image/jpeg", largeContent);

            given(productRepository.findById(product.getId()))
                    .willReturn(Optional.of(product));

            assertThatThrownBy(() ->
                    service.uploadProductImage(product.getId(), file, false, user.getId())
            ).isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Image too large");
        }

        @Test
        @DisplayName("Deve rejeitar produto inexistente")
        void shouldRejectUnknownProduct() {
            var unknownId = UUID.randomUUID();
            var file = new MockMultipartFile("file", "photo.jpg",
                    "image/jpeg", new byte[1024]);

            given(productRepository.findById(unknownId)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.uploadProductImage(unknownId, file, false, user.getId())
            ).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── uploadProductDocument ─────────────────────────────────────────────

    @Nested
    @DisplayName("uploadProductDocument")
    class UploadProductDocument {

        @Test
        @DisplayName("Deve fazer upload de nota fiscal PDF")
        void shouldUploadInvoicePdf() {
            var file = new MockMultipartFile("file", "nf.pdf",
                    "application/pdf", new byte[512 * 1024]); // 512 KB

            given(productRepository.findById(product.getId()))
                    .willReturn(Optional.of(product));
            willDoNothing().given(storagePort).store(any(), any(), any(), anyLong(), any());
            given(attachmentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(storagePort.generatePresignedUrl(any(), any(), any()))
                    .willReturn("https://minio/nf.pdf?sig=xyz");

            var result = service.uploadProductDocument(
                    product.getId(), file, AttachmentCategory.PRODUCT_INVOICE, user.getId());

            assertThat(result.category()).isEqualTo(AttachmentCategory.PRODUCT_INVOICE);
            assertThat(result.contentType()).isEqualTo("application/pdf");
            // Documentos vão para bucket de documentos
            then(storagePort).should().store(eq("test-documents"), any(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("Deve rejeitar categoria inválida para documento de produto")
        void shouldRejectInvalidDocumentCategory() {
            var file = new MockMultipartFile("file", "doc.pdf",
                    "application/pdf", new byte[1024]);

            given(productRepository.findById(product.getId()))
                    .willReturn(Optional.of(product));

            assertThatThrownBy(() ->
                    service.uploadProductDocument(
                            product.getId(), file, AttachmentCategory.USER_DOCUMENT, user.getId())
            ).isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Invalid category");
        }

        @Test
        @DisplayName("Deve rejeitar documento maior que 20 MB")
        void shouldRejectOversizedDocument() {
            byte[] huge = new byte[21 * 1024 * 1024]; // 21 MB
            var file = new MockMultipartFile("file", "big.pdf",
                    "application/pdf", huge);

            given(productRepository.findById(product.getId()))
                    .willReturn(Optional.of(product));

            assertThatThrownBy(() ->
                    service.uploadProductDocument(
                            product.getId(), file, AttachmentCategory.PRODUCT_INVOICE, user.getId())
            ).isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Document too large");
        }
    }

    // ─── uploadUserPhoto ───────────────────────────────────────────────────

    @Nested
    @DisplayName("uploadUserPhoto")
    class UploadUserPhoto {

        @Test
        @DisplayName("Deve remover foto anterior ao subir nova")
        void shouldRemoveOldPhotoOnUpload() {
            var oldPhoto = Attachment.create(
                    EntityType.USER, user.getId(), AttachmentCategory.USER_PHOTO,
                    "old.jpg", "user/old.jpg", "test-images",
                    "image/jpeg", 800, true, user.getId()
            );
            var file = new MockMultipartFile("file", "new.jpg",
                    "image/jpeg", new byte[1024]);

            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(attachmentRepository.findPrimaryByEntityAndCategory(
                    EntityType.USER, user.getId(), AttachmentCategory.USER_PHOTO))
                    .willReturn(Optional.of(oldPhoto));
            willDoNothing().given(storagePort).delete(any(), any());
            willDoNothing().given(storagePort).store(any(), any(), any(), anyLong(), any());
            given(attachmentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(storagePort.generatePresignedUrl(any(), any(), any()))
                    .willReturn("https://minio/new.jpg");

            service.uploadUserPhoto(user.getId(), file, user.getId());

            // Foto antiga deve ser deletada do storage
            then(storagePort).should().delete("test-images", "user/old.jpg");
            // E removida do banco
            then(attachmentRepository).should().deleteById(oldPhoto.getId());
        }

        @Test
        @DisplayName("Deve aceitar PNG como foto de perfil")
        void shouldAcceptPng() {
            var file = new MockMultipartFile("file", "avatar.png",
                    "image/png", new byte[2048]);

            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
            given(attachmentRepository.findPrimaryByEntityAndCategory(any(), any(), any()))
                    .willReturn(Optional.empty());
            willDoNothing().given(storagePort).store(any(), any(), any(), anyLong(), any());
            given(attachmentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(storagePort.generatePresignedUrl(any(), any(), any()))
                    .willReturn("https://minio/avatar.png");

            var result = service.uploadUserPhoto(user.getId(), file, user.getId());

            assertThat(result.category()).isEqualTo(AttachmentCategory.USER_PHOTO);
            assertThat(result.primary()).isTrue();
        }
    }

    // ─── delete ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Deve remover do storage e do banco")
        void shouldDeleteFromStorageAndDb() {
            var attachment = Attachment.create(
                    EntityType.PRODUCT, product.getId(), AttachmentCategory.PRODUCT_IMAGE,
                    "img.jpg", "products/img.jpg", "test-images",
                    "image/jpeg", 1024, false, user.getId()
            );

            given(attachmentRepository.findById(attachment.getId()))
                    .willReturn(Optional.of(attachment));
            willDoNothing().given(storagePort).delete(any(), any());

            service.delete(attachment.getId(), user.getId());

            then(storagePort).should().delete("test-images", "products/img.jpg");
            then(attachmentRepository).should().deleteById(attachment.getId());
        }

        @Test
        @DisplayName("Deve lançar ResourceNotFoundException para attachment inexistente")
        void shouldThrowForUnknownAttachment() {
            var unknownId = UUID.randomUUID();
            given(attachmentRepository.findById(unknownId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(unknownId, user.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}