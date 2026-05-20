package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.ParameterNature;
import br.com.lalurecf.domain.enums.Status;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain Model - Tipo de Parâmetro Tributário.
 *
 * <p>POJO puro sem dependências de frameworks (Spring/JPA). Representa um tipo de parâmetro
 * tributário que agrupa parâmetros e define sua natureza (GLOBAL, MONTHLY, QUARTERLY).
 *
 * <p>Cada tipo encapsula a natureza, permitindo que parâmetros herdem essa característica
 * através do relacionamento.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TaxParameterType {
  private Long id;
  private String description;
  private ParameterNature nature;
  private Status status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private Long createdBy;
  private Long updatedBy;
  private Boolean required;
  private Integer displayOrder;
  private Boolean fiscalMovementExclusive;
}
