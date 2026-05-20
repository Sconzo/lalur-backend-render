package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Entidade JPA para Conta Referencial RFB.
 *
 * <p>Tabela mestra global de contas oficiais da Receita Federal Brasil. Não vinculada a empresas
 * - todas empresas usam mesma referência.
 *
 * <p>Constraint único: (codigo_rfb, ano_validade) permite versionamento anual quando RFB altera
 * layout ECF.
 */
@Entity
@Table(
    name = "tb_conta_referencial",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_conta_referencial_codigo_ano",
            columnNames = {"codigo_rfb", "ano_validade"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ContaReferencialEntity extends BaseEntity {

  /**
   * Código oficial RFB (ex: "1.01.01", "3.01").
   *
   * <p>Não pode ser único sozinho pois mesmo código pode existir em anos diferentes.
   */
  @Column(name = "codigo_rfb", nullable = false, length = 50)
  private String codigoRfb;

  /** Descrição oficial da conta referencial. */
  @Column(name = "descricao", nullable = false, length = 1000)
  private String descricao;

  /**
   * Ano de validade (nullable).
   *
   * <p>null = válido para todos anos. Permite versionamento quando RFB altera estrutura de contas
   * entre anos fiscais.
   */
  @Column(name = "ano_validade")
  private Integer anoValidade;

  /** Modelo da conta referencial (uso futuro). */
  @Column(name = "modelo", length = 50)
  private String modelo;
}
