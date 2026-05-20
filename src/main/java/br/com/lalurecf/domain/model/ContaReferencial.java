package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.Status;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conta Referencial da Receita Federal Brasil (RFB).
 *
 * <p>Tabela mestra global de contas oficiais ECF. Não vinculada a empresas específicas - todas
 * empresas usam a mesma referência RFB.
 *
 * <p>Suporta versionamento anual via anoValidade (null = válido para todos anos).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContaReferencial {

  private Long id;

  /** Código oficial RFB (ex: "1.01.01", "3.01"). */
  private String codigoRfb;

  /** Descrição oficial da conta referencial. */
  private String descricao;

  /**
   * Ano de validade da conta (nullable).
   *
   * <p>null = válido para todos anos. Permite versionamento quando RFB altera estrutura entre anos
   * fiscais.
   */
  private Integer anoValidade;

  /** Modelo da conta referencial (uso futuro). */
  private String modelo;

  /** Status da conta (ACTIVE/INACTIVE). */
  private Status status;

  /** Timestamp de criação (auditoria). */
  private LocalDateTime createdAt;

  /** Timestamp de última atualização (auditoria). */
  private LocalDateTime updatedAt;

  /** ID do usuário que criou (auditoria). */
  private Long createdBy;

  /** ID do usuário que atualizou (auditoria). */
  private Long updatedBy;
}
