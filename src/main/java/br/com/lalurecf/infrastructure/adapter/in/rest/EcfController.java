package br.com.lalurecf.infrastructure.adapter.in.rest;

import br.com.lalurecf.application.port.in.ecf.DownloadEcfFileUseCase;
import br.com.lalurecf.application.port.in.ecf.FinalizeEcfFileUseCase;
import br.com.lalurecf.application.port.in.ecf.GenerateArquivoParcialUseCase;
import br.com.lalurecf.application.port.in.ecf.GenerateCompleteEcfUseCase;
import br.com.lalurecf.application.port.in.ecf.ListEcfFilesUseCase;
import br.com.lalurecf.application.port.in.ecf.UploadImportedEcfUseCase;
import br.com.lalurecf.application.port.in.ecf.ValidateEcfFileUseCase;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.model.EcfFileDownloadData;
import br.com.lalurecf.infrastructure.dto.ecf.EcfFileListResponse;
import br.com.lalurecf.infrastructure.dto.ecf.FinalizeEcfFileResponse;
import br.com.lalurecf.infrastructure.dto.ecf.GenerateArquivoParcialResponse;
import br.com.lalurecf.infrastructure.dto.ecf.GenerateCompleteEcfResponse;
import br.com.lalurecf.infrastructure.dto.ecf.UploadImportedEcfResponse;
import br.com.lalurecf.infrastructure.dto.ecf.ValidationResult;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import br.com.lalurecf.infrastructure.security.FiscalYearContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller para operações de geração, upload e gestão de arquivos ECF.
 *
 * <p>Todos os endpoints requerem role CONTADOR e header X-Company-Id.
 */
@RestController
@RequestMapping("/ecf")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "ECF", description = "Geração e gestão de arquivos ECF (e-Lalur/e-Lacs)")
public class EcfController {

  private final GenerateArquivoParcialUseCase generateArquivoParcialUseCase;
  private final UploadImportedEcfUseCase uploadImportedEcfUseCase;
  private final GenerateCompleteEcfUseCase generateCompleteEcfUseCase;
  private final ValidateEcfFileUseCase validateEcfFileUseCase;
  private final DownloadEcfFileUseCase downloadEcfFileUseCase;
  private final ListEcfFilesUseCase listEcfFilesUseCase;
  private final FinalizeEcfFileUseCase finalizeEcfFileUseCase;

  /**
   * Lista os arquivos ECF de uma empresa para um ano fiscal.
   *
   * <p>Retorna um sumário dos três tipos (ARQUIVO_PARCIAL, IMPORTED_ECF, COMPLETE_ECF).
   * Campos são null quando o arquivo ainda não existe para o tipo. Suporta filtro opcional
   * por fileType.
   *
   * @param fileType filtro opcional por tipo de arquivo (ARQUIVO_PARCIAL, IMPORTED_ECF,
   *                 COMPLETE_ECF)
   * @return DTO com sumário de cada tipo de arquivo
   */
  @GetMapping
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Listar arquivos ECF",
      description =
          "Retorna sumário dos arquivos ECF (ARQUIVO_PARCIAL, IMPORTED_ECF, COMPLETE_ECF) "
              + "para o ano fiscal informado. Campos null quando arquivo não existe. "
              + "Requer headers X-Company-Id e X-Fiscal-Year.")
  public ResponseEntity<EcfFileListResponse> listEcfFiles(
      @RequestParam(required = false) String fileType) {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context é obrigatório (header X-Company-Id ausente)");
    }

    Integer fiscalYear = FiscalYearContext.getCurrentFiscalYear();
    if (fiscalYear == null) {
      throw new IllegalArgumentException(
          "Fiscal year context is required (header X-Fiscal-Year missing)");
    }

    log.info("GET /api/v1/ecf - companyId={}, fiscalYear={}, fileType={}",
        companyId, fiscalYear, fileType);

    EcfFileListResponse response = listEcfFilesUseCase.list(companyId, fiscalYear, fileType);
    return ResponseEntity.ok(response);
  }

  /**
   * Gera o Arquivo Parcial ECF com os registros do bloco M.
   *
   * <p>Agrupa os Lançamentos da Parte B ACTIVE do ano fiscal informado e gera os registros
   * M030/M300/M305/M310 (IRPJ) e M350/M355/M360 (CSLL). O arquivo é persistido via upsert
   * (um por empresa+tipo+ano). Se existir um COMPLETE_ECF com status VALIDATED ou FINALIZED,
   * ele é rebaixado para DRAFT.
   *
   * @return metadados do arquivo gerado
   */
  @PostMapping("/generate-parcial")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Gerar Arquivo Parcial ECF",
      description =
          "Gera o Arquivo Parcial ECF com registros M (IRPJ/CSLL/Parte B) a partir dos "
              + "Lançamentos da Parte B ACTIVE. Requer headers X-Company-Id e X-Fiscal-Year.")
  public ResponseEntity<GenerateArquivoParcialResponse> generateArquivoParcial() {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context é obrigatório (header X-Company-Id ausente)");
    }

    Integer fiscalYear = FiscalYearContext.getCurrentFiscalYear();
    if (fiscalYear == null) {
      throw new IllegalArgumentException(
          "Fiscal year context is required (header X-Fiscal-Year missing)");
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String generatedBy = authentication != null ? authentication.getName() : "unknown";

    log.info("POST /api/v1/ecf/generate-parcial - companyId={}, fiscalYear={}",
        companyId, fiscalYear);

    GenerateArquivoParcialResponse response =
        generateArquivoParcialUseCase.generate(fiscalYear, companyId, generatedBy);

    return ResponseEntity.ok(response);
  }

  /**
   * Faz upload e armazena o ECF Importado de sistema externo.
   *
   * <p>Valida extensão, tamanho (máx 50MB), formato SPED e presença do bloco M.
   * Persiste o arquivo com encoding ISO-8859-1 via upsert. Rebaixa COMPLETE_ECF
   * existente com status VALIDATED ou FINALIZED para DRAFT.
   *
   * @param file arquivo ECF no formato SPED (extensão .txt)
   * @param overwrite se true, sobrescreve ECF importada existente; se false e existir, retorna
   *                  aviso com success=false
   * @return metadados do arquivo armazenado
   * @throws IOException se ocorrer erro ao ler o arquivo
   */
  @PostMapping(value = "/upload-importado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Upload do ECF Importado",
      description =
          "Armazena o ECF gerado por sistema externo (Receita Federal/outro sistema). "
              + "Valida extensão .txt, tamanho máx 50MB, formato SPED e presença de bloco M. "
              + "Requer headers X-Company-Id e X-Fiscal-Year.")
  public ResponseEntity<UploadImportedEcfResponse> uploadImportado(
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "overwrite", required = false, defaultValue = "false")
          boolean overwrite) throws IOException {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context é obrigatório (header X-Company-Id ausente)");
    }

    Integer fiscalYear = FiscalYearContext.getCurrentFiscalYear();
    if (fiscalYear == null) {
      throw new IllegalArgumentException(
          "Fiscal year context is required (header X-Fiscal-Year missing)");
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String generatedBy = authentication != null ? authentication.getName() : "unknown";

    log.info("POST /api/v1/ecf/upload-importado - companyId={}, fiscalYear={}, file={}",
        companyId, fiscalYear, file.getOriginalFilename());

    UploadImportedEcfResponse response = uploadImportedEcfUseCase.upload(
        file.getBytes(), file.getOriginalFilename(), fiscalYear, companyId, generatedBy,
        overwrite);

    return ResponseEntity.ok(response);
  }

  /**
   * Gera o ECF Completo via merge do ECF Importado com o Arquivo Parcial.
   *
   * <p>Mescla os dois arquivos usando algoritmo de merge por chave (codigoApuracao +
   * codigoEnquadramento). Registros M300/M350 presentes no Parcial substituem os do Importado;
   * demais registros do Importado são preservados. M030 do Parcial ausentes no Importado são
   * adicionados. M400/M410/M405 do Parcial substituem os do Importado.
   *
   * @return metadados do ECF Completo gerado
   */
  @PostMapping("/generate-completo")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Gerar ECF Completo",
      description =
          "Faz merge do ECF Importado com o Arquivo Parcial para gerar o ECF Completo. "
              + "Requer upload prévio do ECF Importado e geração do Arquivo Parcial. "
              + "Requer headers X-Company-Id e X-Fiscal-Year.")
  public ResponseEntity<GenerateCompleteEcfResponse> generateCompleto() {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context é obrigatório (header X-Company-Id ausente)");
    }

    Integer fiscalYear = FiscalYearContext.getCurrentFiscalYear();
    if (fiscalYear == null) {
      throw new IllegalArgumentException(
          "Fiscal year context is required (header X-Fiscal-Year missing)");
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String generatedBy = authentication != null ? authentication.getName() : "unknown";

    log.info("POST /api/v1/ecf/generate-completo - companyId={}, fiscalYear={}",
        companyId, fiscalYear);

    GenerateCompleteEcfResponse response =
        generateCompleteEcfUseCase.generate(fiscalYear, companyId, generatedBy);

    return ResponseEntity.ok(response);
  }

  /**
   * Valida os campos obrigatórios do arquivo ECF conforme layout SPED.
   *
   * <p>Delega ao método correto de EcfValidatorService conforme fileType.
   * Atualiza fileStatus para VALIDATED (se válido) ou ERROR (se inválido).
   *
   * @param ecfFileId ID do arquivo ECF a validar
   * @return resultado da validação com erros e avisos
   */
  @PostMapping("/{ecfFileId}/validate")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Validar arquivo ECF",
      description =
          "Valida campos obrigatórios dos registros M conforme layout SPED. "
              + "Atualiza fileStatus para VALIDATED ou ERROR. Requer header X-Company-Id.")
  public ResponseEntity<ValidationResult> validateEcfFile(@PathVariable Long ecfFileId) {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context é obrigatório (header X-Company-Id ausente)");
    }

    log.info("POST /api/v1/ecf/{}/validate - companyId={}", ecfFileId, companyId);

    ValidationResult result = validateEcfFileUseCase.validate(ecfFileId, companyId);
    return ResponseEntity.ok(result);
  }

  /**
   * Faz download do arquivo ECF com encoding ISO-8859-1 (padrão SPED).
   *
   * @param fileType tipo do arquivo (ARQUIVO_PARCIAL, IMPORTED_ECF, COMPLETE_ECF)
   * @return arquivo .txt como byte[] com headers Content-Disposition, Content-Type
   */
  @GetMapping("/download/{fileType}")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Download do arquivo ECF",
      description =
          "Retorna o arquivo ECF (.txt) com encoding ISO-8859-1. "
              + "Informe o tipo: ARQUIVO_PARCIAL, IMPORTED_ECF ou COMPLETE_ECF. "
              + "Requer headers X-Company-Id e X-Fiscal-Year.")
  public ResponseEntity<byte[]> downloadEcfFile(@PathVariable EcfFileType fileType) {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context é obrigatório (header X-Company-Id ausente)");
    }

    Integer fiscalYear = FiscalYearContext.getCurrentFiscalYear();
    if (fiscalYear == null) {
      throw new IllegalArgumentException(
          "Fiscal year context is required (header X-Fiscal-Year missing)");
    }

    log.info("GET /api/v1/ecf/download/{} - companyId={}, fiscalYear={}",
        fileType, companyId, fiscalYear);

    EcfFileDownloadData data = downloadEcfFileUseCase.download(fileType, companyId, fiscalYear);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/plain; charset=ISO-8859-1"));
    headers.setContentDispositionFormData("attachment", data.getFileName());
    headers.setContentLength(data.getFileSizeBytes());

    return ResponseEntity.ok().headers(headers).body(data.getContent());
  }

  /**
   * Finaliza o ECF Completo, indicando que foi transmitido ao SPED.
   *
   * <p>Apenas COMPLETE_ECF com status VALIDATED pode ser finalizado.
   * Após a finalização, o status é alterado para FINALIZED.
   *
   * @param ecfFileId ID do arquivo ECF a finalizar
   * @return confirmação com novo status
   */
  @PatchMapping("/{ecfFileId}/finalize")
  @PreAuthorize("hasRole('CONTADOR')")
  @Operation(
      summary = "Finalizar ECF Completo",
      description =
          "Marca o ECF Completo como FINALIZED, indicando transmissão ao SPED. "
              + "Apenas COMPLETE_ECF com status VALIDATED pode ser finalizado. "
              + "Requer header X-Company-Id.")
  public ResponseEntity<FinalizeEcfFileResponse> finalizeEcfFile(
      @PathVariable Long ecfFileId) {

    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      throw new IllegalArgumentException(
          "Company context é obrigatório (header X-Company-Id ausente)");
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = authentication != null ? authentication.getName() : "unknown";

    log.info("PATCH /api/v1/ecf/{}/finalize - companyId={}", ecfFileId, companyId);

    FinalizeEcfFileResponse response = finalizeEcfFileUseCase.finalize(
        ecfFileId, companyId, userId);
    return ResponseEntity.ok(response);
  }
}
