package br.com.lalurecf.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing a Company (Empresa).
 * Extends BaseEntity for auditing and soft delete.
 * Table name and columns follow snake_case convention (ADR-001).
 */
@Entity
@Table(name = "tb_empresa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyEntity extends BaseEntity {

  @Column(nullable = false, unique = true, length = 14)
  private String cnpj;

  @Column(name = "razao_social", nullable = false)
  private String razaoSocial;

  @Column(name = "periodo_contabil", nullable = false)
  private LocalDate periodoContabil;

  @Column(name = "mascara_niveis", length = 50)
  private String mascaraNiveis;

  // CNAE, Qualificação PJ e Natureza Jurídica são gerenciados como parâmetros tributários
  // via tabela tb_empresa_parametros_tributarios (ADR-001 v2.0)
}
