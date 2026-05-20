package br.com.lalurecf.infrastructure.dto.contaparteb;

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
 * DTO para resposta de conta da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Contém todos os dados da conta fiscal incluindo metadados de auditoria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class ContaParteBResponse {

  private Long id;
  private String codigoConta;
  private String descricao;
  private Integer anoBase;
  private LocalDate dataVigenciaInicio;
  private LocalDate dataVigenciaFim;
  private TipoTributo tipoTributo;
  private BigDecimal saldoInicial;
  private TipoSaldo tipoSaldo;
  private Status status;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
