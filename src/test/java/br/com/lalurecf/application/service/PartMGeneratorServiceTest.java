package br.com.lalurecf.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.lalurecf.application.port.out.ContaParteBRepositoryPort;
import br.com.lalurecf.application.port.out.LancamentoParteBRepositoryPort;
import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.application.port.out.TaxParameterRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.enums.TipoRelacionamento;
import br.com.lalurecf.domain.enums.TipoTributo;
import br.com.lalurecf.domain.model.ContaParteB;
import br.com.lalurecf.domain.model.LancamentoParteB;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.domain.model.TaxParameter;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes unitários para PartMGeneratorService.
 *
 * <p>Cobre geração de registros M030/M300/M305/M310 (IRPJ), M350/M355/M360 (CSLL)
 * e M400/M410/M405 (Contas da Parte B). Todos os repositórios são mockados com Mockito.
 */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
@DisplayName("PartMGeneratorService - Testes Unitários")
class PartMGeneratorServiceTest {

  @Mock
  private LancamentoParteBRepositoryPort lancamentoRepo;

  @Mock
  private ContaParteBRepositoryPort contaParteBRepo;

  @Mock
  private PlanoDeContasRepositoryPort planoDeContasRepo;

  @Mock
  private TaxParameterRepositoryPort taxParameterRepo;

  @InjectMocks
  private PartMGeneratorService service;

  private static final Long COMPANY_ID = 1L;
  private static final Integer FISCAL_YEAR = 2024;
  private static final Long PARAMETRO_ID = 10L;
  private static final Long CONTA_PARTE_B_ID = 20L;
  private static final Long CONTA_CONTABIL_ID = 30L;

  private TaxParameter parametro;
  private ContaParteB contaParteB;
  private PlanoDeContas planoDeContas;

  @BeforeEach
  void setUp() {
    parametro = TaxParameter.builder()
        .id(PARAMETRO_ID)
        .code("001")
        .description("Multas não dedutíveis")
        .build();

    contaParteB = ContaParteB.builder()
        .id(CONTA_PARTE_B_ID)
        .codigoConta("4.01.01")
        .descricao("Prejuízo Fiscal")
        .tipoTributo(TipoTributo.IRPJ)
        .saldoInicial(BigDecimal.valueOf(1000))
        .build();

    planoDeContas = PlanoDeContas.builder()
        .id(CONTA_CONTABIL_ID)
        .code("1.1.01.001")
        .build();
  }

  @Test
  @DisplayName("M030 gerado com datas corretas no formato DDMMYYYY para mês com lançamentos IRPJ")
  void generateGrupoIrpj_deveGerarM030ComDatasCorretas() {
    LancamentoParteB lanc = lancamentoIrpj(1, TipoRelacionamento.CONTA_CONTABIL, TipoAjuste.ADICAO,
        BigDecimal.valueOf(500));
    when(taxParameterRepo.findById(PARAMETRO_ID)).thenReturn(Optional.of(parametro));
    when(planoDeContasRepo.findById(CONTA_CONTABIL_ID)).thenReturn(Optional.of(planoDeContas));

    List<String> lines = service.generateGrupoIrpj(List.of(lanc), FISCAL_YEAR);

    assertThat(lines).isNotEmpty();
    assertThat(lines.get(0)).isEqualTo("|M030|01012024|31012024|A01|");
  }

  @Test
  @DisplayName("ADICAO gera A no M300 e D no M310; EXCLUSAO gera E e C")
  void generateGrupoIrpj_adicaoEExclusaoMapeadosCorretamente() {
    LancamentoParteB adicao = lancamentoIrpj(1, TipoRelacionamento.CONTA_CONTABIL,
        TipoAjuste.ADICAO, BigDecimal.valueOf(300));
    LancamentoParteB exclusao = lancamentoIrpj(2, TipoRelacionamento.CONTA_CONTABIL,
        TipoAjuste.EXCLUSAO, BigDecimal.valueOf(200));
    when(taxParameterRepo.findById(PARAMETRO_ID)).thenReturn(Optional.of(parametro));
    when(planoDeContasRepo.findById(CONTA_CONTABIL_ID)).thenReturn(Optional.of(planoDeContas));

    List<String> adicaoLines = service.generateGrupoIrpj(List.of(adicao), FISCAL_YEAR);
    List<String> exclusaoLines = service.generateGrupoIrpj(List.of(exclusao), FISCAL_YEAR);

    // Mes 1 ADICAO: M030, M300 com A, M310 com D
    String m300Adicao = adicaoLines.get(1);
    assertThat(m300Adicao).contains("|A|");
    String m310Adicao = adicaoLines.get(2);
    assertThat(m310Adicao).endsWith("|300,00|D|");

    // Mes 2 EXCLUSAO: M030, M300 com E, M310 com C
    String m300Exclusao = exclusaoLines.get(1);
    assertThat(m300Exclusao).contains("|E|");
    String m310Exclusao = exclusaoLines.get(2);
    assertThat(m310Exclusao).endsWith("|200,00|C|");
  }

  @Test
  @DisplayName("tipoRelacionamento = AMBOS gera tanto M305 quanto M310")
  void generateGrupoIrpj_ambosGeraM305eM310() {
    LancamentoParteB lanc = lancamentoIrpjAmbos(1, TipoAjuste.ADICAO, BigDecimal.valueOf(100));
    when(taxParameterRepo.findById(PARAMETRO_ID)).thenReturn(Optional.of(parametro));
    when(contaParteBRepo.findById(CONTA_PARTE_B_ID)).thenReturn(Optional.of(contaParteB));
    when(planoDeContasRepo.findById(CONTA_CONTABIL_ID)).thenReturn(Optional.of(planoDeContas));

    List<String> lines = service.generateGrupoIrpj(List.of(lanc), FISCAL_YEAR);

    // M030, M300, M305, M310 = 4 linhas
    assertThat(lines).hasSize(4);
    assertThat(lines.get(2)).startsWith("|M305|");
    assertThat(lines.get(3)).startsWith("|M310|");
  }

  @Test
  @DisplayName("tipoRelacionamento = CONTA_CONTABIL não gera M305")
  void generateGrupoIrpj_contaContabilNaoGeraM305() {
    LancamentoParteB lanc = lancamentoIrpj(1, TipoRelacionamento.CONTA_CONTABIL,
        TipoAjuste.ADICAO, BigDecimal.valueOf(100));
    when(taxParameterRepo.findById(PARAMETRO_ID)).thenReturn(Optional.of(parametro));
    when(planoDeContasRepo.findById(CONTA_CONTABIL_ID)).thenReturn(Optional.of(planoDeContas));

    List<String> lines = service.generateGrupoIrpj(List.of(lanc), FISCAL_YEAR);

    assertThat(lines).noneMatch(l -> l.startsWith("|M305|"));
    assertThat(lines).anyMatch(l -> l.startsWith("|M310|"));
  }

  @Test
  @DisplayName("tipoRelacionamento = CONTA_PARTE_B não gera M310")
  void generateGrupoIrpj_contaParteBNaoGeraM310() {
    LancamentoParteB lanc = lancamentoIrpj(1, TipoRelacionamento.CONTA_PARTE_B,
        TipoAjuste.ADICAO, BigDecimal.valueOf(100));
    when(taxParameterRepo.findById(PARAMETRO_ID)).thenReturn(Optional.of(parametro));
    when(contaParteBRepo.findById(CONTA_PARTE_B_ID)).thenReturn(Optional.of(contaParteB));

    List<String> lines = service.generateGrupoIrpj(List.of(lanc), FISCAL_YEAR);

    assertThat(lines).noneMatch(l -> l.startsWith("|M310|"));
    assertThat(lines).anyMatch(l -> l.startsWith("|M305|"));
  }

  @Test
  @DisplayName("Lançamentos IRPJ geram M300/M305/M310; CSLL geram M350/M355/M360")
  void generateGrupo_irpjUsaM300csllUsaM350() {
    LancamentoParteB irpj = lancamentoIrpj(1, TipoRelacionamento.CONTA_CONTABIL,
        TipoAjuste.ADICAO, BigDecimal.valueOf(100));
    LancamentoParteB csll = LancamentoParteB.builder()
        .mesReferencia(1)
        .anoReferencia(FISCAL_YEAR)
        .tipoApuracao(TipoApuracao.CSLL)
        .tipoRelacionamento(TipoRelacionamento.CONTA_CONTABIL)
        .tipoAjuste(TipoAjuste.ADICAO)
        .contaContabilId(CONTA_CONTABIL_ID)
        .parametroTributarioId(PARAMETRO_ID)
        .descricao("CSLL desc")
        .valor(BigDecimal.valueOf(200))
        .status(Status.ACTIVE)
        .build();
    when(taxParameterRepo.findById(PARAMETRO_ID)).thenReturn(Optional.of(parametro));
    when(planoDeContasRepo.findById(CONTA_CONTABIL_ID)).thenReturn(Optional.of(planoDeContas));

    List<String> irpjLines = service.generateGrupoIrpj(List.of(irpj), FISCAL_YEAR);
    List<String> csllLines = service.generateGrupoCsll(List.of(csll), FISCAL_YEAR);

    assertThat(irpjLines).anyMatch(l -> l.startsWith("|M300|"));
    assertThat(irpjLines).anyMatch(l -> l.startsWith("|M310|"));
    assertThat(irpjLines).noneMatch(l -> l.startsWith("|M350|"));

    assertThat(csllLines).anyMatch(l -> l.startsWith("|M350|"));
    assertThat(csllLines).anyMatch(l -> l.startsWith("|M360|"));
    assertThat(csllLines).noneMatch(l -> l.startsWith("|M300|"));
  }

  @Test
  @DisplayName("Formatação de valor com vírgula decimal e 2 casas")
  void generateGrupoIrpj_formatacaoDeValorComVirgula() {
    LancamentoParteB lanc = lancamentoIrpj(1, TipoRelacionamento.CONTA_CONTABIL,
        TipoAjuste.ADICAO, new BigDecimal("1234.56"));
    when(taxParameterRepo.findById(PARAMETRO_ID)).thenReturn(Optional.of(parametro));
    when(planoDeContasRepo.findById(CONTA_CONTABIL_ID)).thenReturn(Optional.of(planoDeContas));

    List<String> lines = service.generateGrupoIrpj(List.of(lanc), FISCAL_YEAR);

    assertThat(lines).anyMatch(l -> l.contains("1234,56"));
    assertThat(lines).noneMatch(l -> l.contains("1234.56"));
  }

  @Test
  @DisplayName("somaValores no M300 é a soma dos valores do grupo")
  void generateGrupoIrpj_somaValoresNoM300() {
    LancamentoParteB lanc1 = lancamentoIrpj(1, TipoRelacionamento.CONTA_CONTABIL,
        TipoAjuste.ADICAO, BigDecimal.valueOf(300));
    LancamentoParteB lanc2 = lancamentoIrpj(1, TipoRelacionamento.CONTA_CONTABIL,
        TipoAjuste.ADICAO, BigDecimal.valueOf(200));
    when(taxParameterRepo.findById(PARAMETRO_ID)).thenReturn(Optional.of(parametro));
    when(planoDeContasRepo.findById(CONTA_CONTABIL_ID)).thenReturn(Optional.of(planoDeContas));

    List<String> lines = service.generateGrupoIrpj(List.of(lanc1, lanc2), FISCAL_YEAR);

    String m300 = lines.stream().filter(l -> l.startsWith("|M300|")).findFirst().orElseThrow();
    assertThat(m300).contains("500,00");
  }

  @Test
  @DisplayName("Lançamentos INACTIVE são ignorados")
  void generateArquivoParcial_lancamentosInativosIgnorados() {
    LancamentoParteB active = lancamentoIrpj(1, TipoRelacionamento.CONTA_CONTABIL,
        TipoAjuste.ADICAO, BigDecimal.valueOf(100));
    LancamentoParteB inactive = LancamentoParteB.builder()
        .mesReferencia(1)
        .anoReferencia(FISCAL_YEAR)
        .tipoApuracao(TipoApuracao.IRPJ)
        .tipoRelacionamento(TipoRelacionamento.CONTA_CONTABIL)
        .tipoAjuste(TipoAjuste.ADICAO)
        .contaContabilId(CONTA_CONTABIL_ID)
        .parametroTributarioId(PARAMETRO_ID)
        .descricao("inactive")
        .valor(BigDecimal.valueOf(999))
        .status(Status.INACTIVE)
        .build();
    when(lancamentoRepo.findByCompanyIdAndAnoReferencia(COMPANY_ID, FISCAL_YEAR))
        .thenReturn(List.of(active, inactive));
    when(taxParameterRepo.findById(PARAMETRO_ID)).thenReturn(Optional.of(parametro));
    when(planoDeContasRepo.findById(CONTA_CONTABIL_ID)).thenReturn(Optional.of(planoDeContas));

    String result = service.generateArquivoParcial(COMPANY_ID, FISCAL_YEAR);

    assertThat(result).doesNotContain("999,00");
    assertThat(result).contains("100,00");
  }

  @Test
  @DisplayName("Sem lançamentos ACTIVE lança IllegalArgumentException")
  void generateArquivoParcial_semLancamentosAtivosLancaExcecao() {
    when(lancamentoRepo.findByCompanyIdAndAnoReferencia(COMPANY_ID, FISCAL_YEAR))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.generateArquivoParcial(COMPANY_ID, FISCAL_YEAR))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(String.valueOf(FISCAL_YEAR));
  }

  @Test
  @DisplayName("Lançamentos de meses diferentes geram M030 separados")
  void generateGrupoIrpj_mesesDiferentesGeramM030Separados() {
    LancamentoParteB lancMes1 = lancamentoIrpj(1, TipoRelacionamento.CONTA_CONTABIL,
        TipoAjuste.ADICAO, BigDecimal.valueOf(100));
    LancamentoParteB lancMes3 = lancamentoIrpj(3, TipoRelacionamento.CONTA_CONTABIL,
        TipoAjuste.ADICAO, BigDecimal.valueOf(200));
    when(taxParameterRepo.findById(PARAMETRO_ID)).thenReturn(Optional.of(parametro));
    when(planoDeContasRepo.findById(CONTA_CONTABIL_ID)).thenReturn(Optional.of(planoDeContas));

    List<String> lines = service.generateGrupoIrpj(List.of(lancMes1, lancMes3), FISCAL_YEAR);

    long m030Count = lines.stream().filter(l -> l.startsWith("|M030|")).count();
    assertThat(m030Count).isEqualTo(2);
    assertThat(lines).anyMatch(l -> l.equals("|M030|01012024|31012024|A01|"));
    assertThat(lines).anyMatch(l -> l.equals("|M030|01032024|31032024|A03|"));
  }

  @Test
  @DisplayName("M405.saldoAtual = saldoInicial + totalAdic - totalExcl da ContaParteB")
  void generateGrupo3ParteB_saldoAtualCalculadoCorretamente() {
    // saldoInicial = 1000, adicao = 500, exclusao = 200 → saldoAtual = 1300
    ContaParteB conta = ContaParteB.builder()
        .id(CONTA_PARTE_B_ID)
        .codigoConta("4.01.01")
        .descricao("Prejuízo Fiscal")
        .tipoTributo(TipoTributo.IRPJ)
        .saldoInicial(BigDecimal.valueOf(1000))
        .build();

    LancamentoParteB adicao = LancamentoParteB.builder()
        .mesReferencia(1)
        .anoReferencia(FISCAL_YEAR)
        .tipoApuracao(TipoApuracao.IRPJ)
        .tipoRelacionamento(TipoRelacionamento.CONTA_PARTE_B)
        .tipoAjuste(TipoAjuste.ADICAO)
        .contaParteBId(CONTA_PARTE_B_ID)
        .parametroTributarioId(PARAMETRO_ID)
        .descricao("adicao")
        .valor(BigDecimal.valueOf(500))
        .status(Status.ACTIVE)
        .build();

    LancamentoParteB exclusao = LancamentoParteB.builder()
        .mesReferencia(2)
        .anoReferencia(FISCAL_YEAR)
        .tipoApuracao(TipoApuracao.IRPJ)
        .tipoRelacionamento(TipoRelacionamento.CONTA_PARTE_B)
        .tipoAjuste(TipoAjuste.EXCLUSAO)
        .contaParteBId(CONTA_PARTE_B_ID)
        .parametroTributarioId(PARAMETRO_ID)
        .descricao("exclusao")
        .valor(BigDecimal.valueOf(200))
        .status(Status.ACTIVE)
        .build();

    when(contaParteBRepo.findById(CONTA_PARTE_B_ID)).thenReturn(Optional.of(conta));

    List<String> lines = service.generateGrupo3ParteB(List.of(adicao, exclusao));

    String m405 = lines.stream().filter(l -> l.startsWith("|M405|")).findFirst().orElseThrow();
    // |M405|{codigo}|{saldoAnterior}|{totalAdic}|{totalExcl}|{saldoAtual}|
    assertThat(m405).contains("|1000,00|");
    assertThat(m405).contains("|500,00|");
    assertThat(m405).contains("|200,00|");
    assertThat(m405).endsWith("|1300,00|");
  }

  // --- helpers ---

  private LancamentoParteB lancamentoIrpj(int mes, TipoRelacionamento rel,
      TipoAjuste ajuste, BigDecimal valor) {
    return LancamentoParteB.builder()
        .mesReferencia(mes)
        .anoReferencia(FISCAL_YEAR)
        .tipoApuracao(TipoApuracao.IRPJ)
        .tipoRelacionamento(rel)
        .tipoAjuste(ajuste)
        .contaParteBId(rel == TipoRelacionamento.CONTA_PARTE_B ? CONTA_PARTE_B_ID : null)
        .contaContabilId(rel == TipoRelacionamento.CONTA_CONTABIL ? CONTA_CONTABIL_ID : null)
        .parametroTributarioId(PARAMETRO_ID)
        .descricao("Teste lancamento mes " + mes)
        .valor(valor)
        .status(Status.ACTIVE)
        .build();
  }

  private LancamentoParteB lancamentoIrpjAmbos(int mes, TipoAjuste ajuste, BigDecimal valor) {
    return LancamentoParteB.builder()
        .mesReferencia(mes)
        .anoReferencia(FISCAL_YEAR)
        .tipoApuracao(TipoApuracao.IRPJ)
        .tipoRelacionamento(TipoRelacionamento.AMBOS)
        .tipoAjuste(ajuste)
        .contaParteBId(CONTA_PARTE_B_ID)
        .contaContabilId(CONTA_CONTABIL_ID)
        .parametroTributarioId(PARAMETRO_ID)
        .descricao("Teste ambos mes " + mes)
        .valor(valor)
        .status(Status.ACTIVE)
        .build();
  }
}
