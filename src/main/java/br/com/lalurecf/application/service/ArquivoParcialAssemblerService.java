package br.com.lalurecf.application.service;

import br.com.lalurecf.application.port.in.ecf.GenerateArquivoParcialUseCase;
import br.com.lalurecf.application.port.out.CompanyRepositoryPort;
import br.com.lalurecf.application.port.out.EcfFileRepositoryPort;
import br.com.lalurecf.application.port.out.LancamentoParteBRepositoryPort;
import br.com.lalurecf.domain.enums.EcfFileStatus;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.Company;
import br.com.lalurecf.domain.model.EcfFile;
import br.com.lalurecf.domain.model.LancamentoParteB;
import br.com.lalurecf.infrastructure.dto.ecf.GenerateArquivoParcialResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por montar e persistir o Arquivo Parcial ECF.
 *
 * <p>Orquestra: validação de lançamentos → geração do bloco M via PartMGeneratorService →
 * persistência via EcfFileRepositoryPort (upsert) → rebaixamento do COMPLETE_ECF existente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ArquivoParcialAssemblerService implements GenerateArquivoParcialUseCase {

  private final PartMGeneratorService partMGeneratorService;
  private final EcfFileRepositoryPort ecfFileRepositoryPort;
  private final LancamentoParteBRepositoryPort lancamentoParteBRepositoryPort;
  private final CompanyRepositoryPort companyRepositoryPort;

  /**
   * Gera o Arquivo Parcial ECF em 5 passos:
   * 1. Valida existência de lançamentos ACTIVE;
   * 2. Gera conteúdo do bloco M;
   * 3. Constrói EcfFile com metadados;
   * 4. Persiste via upsert;
   * 5. Rebaixa COMPLETE_ECF para DRAFT se necessário.
   */
  @Override
  @Transactional
  public GenerateArquivoParcialResponse generate(
      Integer fiscalYear, Long companyId, String generatedBy) {

    log.info("Gerando Arquivo Parcial ECF: companyId={}, fiscalYear={}", companyId, fiscalYear);

    // Passo 1: validar lançamentos ACTIVE (filtrados no banco)
    List<LancamentoParteB> active =
        lancamentoParteBRepositoryPort.findByCompanyIdAndAnoReferenciaAndStatus(
            companyId, fiscalYear, Status.ACTIVE);

    if (active.isEmpty()) {
      throw new IllegalArgumentException(
          "Nenhum Lançamento da Parte B encontrado para o ano fiscal " + fiscalYear);
    }

    // Passo 2: gerar conteúdo do bloco M reutilizando a lista já carregada
    String content = partMGeneratorService.generateArquivoParcial(active, fiscalYear);

    // Passo 3: montar EcfFile
    Company company = companyRepositoryPort.findById(companyId)
        .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada: " + companyId));

    String cnpj = company.getCnpj() != null ? company.getCnpj().getValue() : companyId.toString();
    String fileName = String.format("Parcial_M_%d_%s.txt", fiscalYear, cnpj);

    EcfFile ecfFile = EcfFile.builder()
        .fileType(EcfFileType.ARQUIVO_PARCIAL)
        .companyId(companyId)
        .fiscalYear(fiscalYear)
        .content(content)
        .fileName(fileName)
        .fileStatus(EcfFileStatus.DRAFT)
        .generatedAt(LocalDateTime.now())
        .generatedBy(generatedBy)
        .status(Status.ACTIVE)
        .build();

    // Passo 4: persistir (upsert)
    EcfFile saved = ecfFileRepositoryPort.saveOrReplace(ecfFile);
    log.info("Arquivo Parcial ECF salvo: id={}, fileName={}", saved.getId(), saved.getFileName());

    // Passo 5: rebaixar COMPLETE_ECF existente para DRAFT
    ecfFileRepositoryPort
        .findByCompanyAndFiscalYearAndType(companyId, fiscalYear, EcfFileType.COMPLETE_ECF)
        .filter(ecf -> ecf.getFileStatus() == EcfFileStatus.VALIDATED
            || ecf.getFileStatus() == EcfFileStatus.FINALIZED)
        .ifPresent(ecf -> {
          ecf.setFileStatus(EcfFileStatus.DRAFT);
          ecf.setValidationErrors(null);
          ecfFileRepositoryPort.saveOrReplace(ecf);
          log.info("COMPLETE_ECF {} rebaixado para DRAFT", ecf.getId());
        });

    // Calcular métricas de resposta
    int periodoCount = (int) content.lines()
        .filter(l -> l.startsWith("|M030|"))
        .count();

    return new GenerateArquivoParcialResponse(
        true,
        "Arquivo Parcial ECF gerado com sucesso",
        saved.getId(),
        saved.getFileName(),
        periodoCount,
        active.size());
  }
}
