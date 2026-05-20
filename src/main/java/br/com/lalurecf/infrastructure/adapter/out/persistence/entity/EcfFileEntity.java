package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import br.com.lalurecf.domain.enums.EcfFileStatus;
import br.com.lalurecf.domain.enums.EcfFileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidade JPA para arquivo ECF.
 *
 * <p>Armazena os três tipos de arquivo ECF: ARQUIVO_PARCIAL, IMPORTED_ECF e COMPLETE_ECF.
 * O conteúdo é persistido como TEXT (sem limite de tamanho no PostgreSQL).
 *
 * <p>Constraint único: (file_type, company_id, fiscal_year) garante um arquivo por tipo
 * por empresa por ano — suporta semântica de upsert (saveOrReplace).
 */
@Entity
@Table(
    name = "tb_ecf_file",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_ecf_file_type_company_year",
            columnNames = {"file_type", "company_id", "fiscal_year"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class EcfFileEntity extends BaseEntity {

  /** Tipo do arquivo (ARQUIVO_PARCIAL, IMPORTED_ECF, COMPLETE_ECF). */
  @Enumerated(EnumType.STRING)
  @Column(name = "file_type", nullable = false, length = 30)
  private EcfFileType fileType;

  /** Empresa dona do arquivo. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyEntity company;

  /** Ano fiscal de referência. */
  @Column(name = "fiscal_year", nullable = false)
  private Integer fiscalYear;

  /** Conteúdo do arquivo ECF em formato texto ISO-8859-1. */
  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  /** Nome do arquivo para download. */
  @Column(name = "file_name", length = 255)
  private String fileName;

  /** Status do arquivo no ciclo de vida. */
  @Enumerated(EnumType.STRING)
  @Column(name = "file_status", length = 20)
  private EcfFileStatus fileStatus;

  /** Erros de validação como JSON array (preenchido quando fileStatus = ERROR). */
  @Column(name = "validation_errors", columnDefinition = "TEXT")
  private String validationErrors;

  /** Timestamp de geração ou importação. */
  @Column(name = "generated_at")
  private LocalDateTime generatedAt;

  /** Usuário que gerou/importou o arquivo. */
  @Column(name = "generated_by", length = 255)
  private String generatedBy;

  /** ECF Importado usado como base para o COMPLETE_ECF (nullable). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_imported_ecf_id")
  private EcfFileEntity sourceImportedEcf;

  /** Arquivo Parcial usado como base para o COMPLETE_ECF (nullable). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "source_parcial_file_id")
  private EcfFileEntity sourceParcialFile;
}
