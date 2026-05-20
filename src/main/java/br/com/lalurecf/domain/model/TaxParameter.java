package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.Status;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain Model - Parâmetro Tributário.
 *
 * <p>POJO puro sem dependências de frameworks (Spring/JPA). Representa um parâmetro tributário no
 * contexto de domínio seguindo princípios da arquitetura hexagonal.
 *
 * <p>Estrutura flat (sem hierarquia parent/child) conforme ADR-001 v2.0. O tipo e natureza são
 * definidos através do relacionamento com TaxParameterType.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TaxParameter {
  private Long id;
  private String code;
  private Long typeId;
  private TaxParameterType type;
  private String description;
  private Status status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long createdBy;
  private Long updatedBy;
}
