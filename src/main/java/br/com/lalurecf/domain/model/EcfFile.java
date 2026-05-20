package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.EcfFileStatus;
import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.enums.Status;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de domínio para arquivo ECF.
 *
 * <p>Representa qualquer um dos três tipos de arquivo ECF gerenciados pelo sistema:
 * ARQUIVO_PARCIAL, IMPORTED_ECF e COMPLETE_ECF. O conteúdo do arquivo é armazenado como
 * String com encoding ISO-8859-1 (LATIN-1), padrão do SPED ECF.
 *
 * <p>Constraint: único por (fileType, companyId, fiscalYear).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EcfFile {

  private Long id;

  /**Tipo do arquivo (ARQUIVO_PARCIAL, IMPORTED_ECF, COMPLETE_ECF). */
  private EcfFileType fileType;

  /**ID da empresa dona do arquivo. */
  private Long companyId;

  /**Ano fiscal de referência (ex: 2024). */
  private Integer fiscalYear;

  /**Conteúdo do arquivo ECF em formato texto ISO-8859-1. */
  private String content;

  /**Nome do arquivo para download (ex: "ECF_PARCIAL_2024.txt"). */
  private String fileName;

  /**Status atual do arquivo no ciclo de vida. */
  private EcfFileStatus fileStatus;

  /**Erros de validação serializados como JSON array (preenchido quando fileStatus = ERROR). */
  private String validationErrors;

  /**Timestamp de geração ou importação do arquivo. */
  private LocalDateTime generatedAt;

  /**Email ou identificador do usuário que gerou/importou o arquivo. */
  private String generatedBy;

  /**ID do ECF Importado usado como base para geração do COMPLETE_ECF (nullable). */
  private Long sourceImportedEcfId;

  /**ID do Arquivo Parcial usado como base para geração do COMPLETE_ECF (nullable). */
  private Long sourceParcialFileId;

  /**Status da entidade para soft delete (ACTIVE/INACTIVE). */
  private Status status;

  /**Timestamp de criação (auditoria). */
  private LocalDateTime createdAt;

  /**Timestamp de última atualização (auditoria). */
  private LocalDateTime updatedAt;

  /**ID do usuário que criou (auditoria). */
  private Long createdBy;

  /**ID do usuário que atualizou (auditoria). */
  private Long updatedBy;
}
