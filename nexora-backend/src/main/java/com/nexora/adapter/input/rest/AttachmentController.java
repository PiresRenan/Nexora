package com.nexora.adapter.input.rest;

import com.nexora.application.dto.attachment.AttachmentResponse;
import com.nexora.application.usecase.AttachmentUseCase;
import com.nexora.domain.model.AttachmentCategory;
import com.nexora.domain.model.EntityType;
import com.nexora.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Gerenciamento de arquivos do Nexora: imagens de produto, notas fiscais,
 * fotos de perfil e documentos pessoais de funcionários.
 *
 * Organização de endpoints:
 *  /attachments/products/{id}/images      — imagens de produto
 *  /attachments/products/{id}/documents   — documentos de produto (NF, manual, etc.)
 *  /attachments/users/{id}/photo          — foto de perfil
 *  /attachments/users/{id}/documents      — documentos pessoais (RG, CNH, contrato)
 *  /attachments/products/{id}             — listar tudo de um produto
 *  /attachments/users/{id}                — listar tudo de um usuário
 *  /attachments/{id}/url                  — presigned URL para download
 *  /attachments/{id}                      — deletar attachment
 *
 * Controle de acesso:
 *  - Listar imagens de produto: público (para catálogo)
 *  - Listar documentos de produto: SELLER+
 *  - Upload de imagens: SELLER+
 *  - Upload/leitura de documentos de produto: SELLER+
 *  - Upload/leitura de documentos de usuário: MANAGER+
 *  - Usuário pode atualizar/ver a própria foto
 */
@RestController
@RequestMapping("/api/v1/attachments")
@Tag(name = "Attachments", description = "Upload e gestão de imagens e documentos")
public class AttachmentController {

    private final AttachmentUseCase attachmentUseCase;

    public AttachmentController(AttachmentUseCase attachmentUseCase) {
        this.attachmentUseCase = attachmentUseCase;
    }

    // ─── Produto: imagens ─────────────────────────────────────────────────

    @PostMapping(
            value    = "/products/{productId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Upload de imagem de produto (SELLER+)",
            description = """
            Tipos aceitos: JPEG, PNG, WebP. Tamanho máximo: **5 MB**.
            
            Use `primary=true` para definir como imagem principal do produto
            (exibida no catálogo). A imagem principal anterior é automaticamente
            promovida para imagem secundária.
            """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Imagem enviada com sucesso"),
                    @ApiResponse(responseCode = "400", description = "Tipo ou tamanho inválido"),
                    @ApiResponse(responseCode = "404", description = "Produto não encontrado"),
                    @ApiResponse(responseCode = "422", description = "Regra de negócio violada")
            }
    )
    public ResponseEntity<AttachmentResponse> uploadProductImage(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean primary,
            @CurrentUser UUID userId
    ) {
        var response = attachmentUseCase.uploadProductImage(productId, file, primary, userId);
        return ResponseEntity
                .created(URI.create("/api/v1/attachments/" + response.id()))
                .body(response);
    }

    @PostMapping(
            value    = "/products/{productId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Upload de documento de produto (SELLER+)",
            description = """
            Tipos aceitos: PDF, XML. Tamanho máximo: **20 MB**.
            
            **Categorias disponíveis:**
            - `PRODUCT_INVOICE` — Nota fiscal de compra
            - `PRODUCT_DOCUMENT` — Manual técnico, ficha de especificação, etc.
            """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Documento enviado"),
                    @ApiResponse(responseCode = "400", description = "Categoria inválida ou tipo de arquivo inválido")
            }
    )
    public ResponseEntity<AttachmentResponse> uploadProductDocument(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam AttachmentCategory category,
            @CurrentUser UUID userId
    ) {
        var response = attachmentUseCase.uploadProductDocument(productId, file, category, userId);
        return ResponseEntity
                .created(URI.create("/api/v1/attachments/" + response.id()))
                .body(response);
    }

    // ─── Usuário: foto de perfil ──────────────────────────────────────────

    @PostMapping(
            value    = "/users/{userId}/photo",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Upload de foto de perfil",
            description = """
            Tipos aceitos: JPEG, PNG, WebP. Tamanho máximo: **5 MB**.
            
            Cada usuário possui **uma única foto de perfil**. O upload de uma nova
            foto remove automaticamente a anterior do sistema de armazenamento.
            
            Acesso: o próprio usuário ou MANAGER+.
            """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Foto atualizada"),
                    @ApiResponse(responseCode = "400", description = "Tipo ou tamanho inválido")
            }
    )
    public ResponseEntity<AttachmentResponse> uploadUserPhoto(
            @PathVariable UUID userId,
            @RequestParam("file") MultipartFile file,
            @CurrentUser UUID currentUserId
    ) {
        var response = attachmentUseCase.uploadUserPhoto(userId, file, currentUserId);
        return ResponseEntity
                .created(URI.create("/api/v1/attachments/" + response.id()))
                .body(response);
    }

    @PostMapping(
            value    = "/users/{userId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Upload de documento pessoal (MANAGER+)",
            description = """
            Tipos aceitos: PDF, XML. Tamanho máximo: **20 MB**.
            
            **Categorias disponíveis:**
            - `USER_DOCUMENT` — RG, CNH, contrato de trabalho, etc.
            """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Documento enviado"),
                    @ApiResponse(responseCode = "400", description = "Tipo de arquivo inválido")
            }
    )
    public ResponseEntity<AttachmentResponse> uploadUserDocument(
            @PathVariable UUID userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam AttachmentCategory category,
            @CurrentUser UUID currentUserId
    ) {
        var response = attachmentUseCase.uploadUserDocument(userId, file, category, currentUserId);
        return ResponseEntity
                .created(URI.create("/api/v1/attachments/" + response.id()))
                .body(response);
    }

    // ─── Listagem ─────────────────────────────────────────────────────────

    @GetMapping("/products/{productId}")
    @Operation(
            summary = "Listar todos os arquivos de um produto",
            description = "Retorna imagens e documentos. Inclui presigned URLs para download direto."
    )
    public ResponseEntity<List<AttachmentResponse>> listProductAttachments(
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(
                attachmentUseCase.findByEntity(EntityType.PRODUCT, productId)
        );
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN') or #userId == authentication.principal.id")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Listar arquivos de um usuário (MANAGER+ ou o próprio)",
            description = "Retorna foto de perfil e documentos pessoais."
    )
    public ResponseEntity<List<AttachmentResponse>> listUserAttachments(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                attachmentUseCase.findByEntity(EntityType.USER, userId)
        );
    }

    // ─── Presigned URL ────────────────────────────────────────────────────

    @GetMapping("/{id}/url")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Gerar nova presigned URL para download",
            description = """
            Gera uma nova URL temporária de acesso direto ao arquivo no storage.
            
            - **Imagens**: válida por **1 hora**
            - **Documentos**: válida por **30 minutos**
            
            Use quando a URL anterior expirou ou para compartilhamento temporário.
            """
    )
    public ResponseEntity<AttachmentResponse> getPresignedUrl(@PathVariable UUID id) {
        return ResponseEntity.ok(attachmentUseCase.refreshUrl(id));
    }

    // ─── Deleção ──────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SELLER', 'MANAGER', 'ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Remover attachment (SELLER+)",
            description = """
            Remove o arquivo do sistema de armazenamento e os metadados do banco.
            **Operação irreversível.**
            
            - Documentos de usuário: requer MANAGER+
            - Imagens/documentos de produto: SELLER+ pode remover
            """
    )
    public void delete(
            @PathVariable UUID id,
            @CurrentUser UUID userId
    ) {
        attachmentUseCase.delete(id, userId);
    }
}