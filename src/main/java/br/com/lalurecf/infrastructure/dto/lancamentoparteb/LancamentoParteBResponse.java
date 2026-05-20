package br.com.lalurecf.infrastructure.dto.lancamentoparteb;

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
 * DTO de resposta para Lançamento da Parte B.
 *
 * <p>Retorna todos os dados do lançamento fiscal incluindo timestamps de auditoria.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteBResponse {

  private Long id;

  private Integer mesReferencia;

  private Integer anoReferencia;

  private TipoApuracao tipoApuracao;

  private TipoRelacionamento tipoRelacionamento;

  private Long contaContabilId;

  private String contaContabilCode;

  private Long contaParteBId;

  private String contaParteBCode;

  private Long parametroTributarioId;

  private String parametroTributarioCodigo;

  private TipoAjuste tipoAjuste;

  private String descricao;

  private BigDecimal valor;

  private Status status;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;
}
