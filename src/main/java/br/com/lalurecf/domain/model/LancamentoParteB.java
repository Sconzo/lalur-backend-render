package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.enums.TipoRelacionamento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model para Lançamento da Parte B (e-Lalur/e-Lacs).
 *
 * <p>Representa ajustes fiscais (adições/exclusões) ao lucro líquido para apuração de IRPJ/CSLL.
 * Pode ser vinculado a conta contábil, conta Parte B ou ambas, dependendo do tipoRelacionamento.
 *
 * <p>Pure POJO sem dependências de frameworks (no Spring/JPA annotations).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteB {

  private Long id;

  private Long companyId;

  private Integer mesReferencia;

  private Integer anoReferencia;

  private TipoApuracao tipoApuracao;

  private TipoRelacionamento tipoRelacionamento;

  private Long contaContabilId;

  private Long contaParteBId;

  private Long parametroTributarioId;

  private TipoAjuste tipoAjuste;

  private String descricao;

  private BigDecimal valor;

  private Status status;

  private Long createdBy;

  private LocalDateTime createdAt;

  private Long updatedBy;

  private LocalDateTime updatedAt;
}
