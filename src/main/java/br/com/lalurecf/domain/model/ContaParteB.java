package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoSaldo;
import br.com.lalurecf.domain.enums.TipoTributo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conta da Parte B do e-Lalur/e-Lacs.
 *
 * <p>Representa contas fiscais específicas de IRPJ/CSLL, separadas do plano de contas contábil.
 * Contas da Parte B são usadas para controlar valores fiscais que diferem da contabilidade
 * (prejuízos fiscais, adições/exclusões temporárias, etc).
 *
 * <p>Características:
 *
 * <ul>
 *   <li>Vinculada a uma Company específica
 *   <li>Possui vigência temporal (dataVigenciaInicio/Fim)
 *   <li>Versionamento anual via anoBase
 *   <li>Tipo de tributo (IRPJ, CSLL ou AMBOS)
 *   <li>Saldo inicial configurável
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContaParteB {

  private Long id;

  /** ID da empresa dona do cadastro. */
  private Long companyId;

  /** Código da conta Parte B (ex: "4.01.01"). */
  private String codigoConta;

  /** Descrição da conta. */
  private String descricao;

  /** Ano base de criação/referência da conta. */
  private Integer anoBase;

  /** Data de início de vigência da conta. */
  private LocalDate dataVigenciaInicio;

  /**
   * Data de fim de vigência da conta (nullable).
   *
   * <p>null = conta ainda vigente.
   */
  private LocalDate dataVigenciaFim;

  /** Tipo de tributo (IRPJ, CSLL ou AMBOS). */
  private TipoTributo tipoTributo;

  /** Saldo inicial da conta. */
  private BigDecimal saldoInicial;

  /** Tipo de saldo (DEVEDOR ou CREDOR). */
  private TipoSaldo tipoSaldo;

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
