package br.com.lalurecf.infrastructure.aspect;

import br.com.lalurecf.domain.model.TemporalEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.exception.PeriodoContabilViolationException;
import br.com.lalurecf.infrastructure.security.CompanyContext;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Aspect que intercepta métodos anotados com @EnforcePeriodoContabil para validar
 * se operações de edição/exclusão estão sendo feitas em registros com competência
 * posterior ao Período Contábil da empresa.
 *
 * <p>Funcionalidade:
 * <ul>
 *   <li>Intercepta métodos anotados com @EnforcePeriodoContabil
 *   <li>Busca Período Contábil da empresa do contexto (CompanyContext)
 *   <li>Verifica se algum argumento do método implementa TemporalEntity
 *   <li>Compara competência do registro com Período Contábil
 *   <li>Lança PeriodoContabilViolationException se competência < Período Contábil
 * </ul>
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PeriodoContabilAspect {

  private final CompanyJpaRepository companyRepository;

  /**
   * Intercepta métodos anotados com @EnforcePeriodoContabil.
   *
   * <p>Executa ANTES do método alvo (before advice).
   *
   * @param joinPoint informações sobre o método interceptado
   * @throws PeriodoContabilViolationException se competência < Período Contábil
   */
  @Before("@annotation(br.com.lalurecf.infrastructure.annotation.EnforcePeriodoContabil)")
  public void enforcePeriodoContabil(JoinPoint joinPoint) {
    log.debug("PeriodoContabilAspect: Interceptando método {}", joinPoint.getSignature());

    // 1. Obter companyId do contexto (ThreadLocal via header X-Company-Id)
    Long companyId = CompanyContext.getCurrentCompanyId();
    if (companyId == null) {
      log.warn("PeriodoContabilAspect: X-Company-Id não está presente no contexto. "
          + "Validação de Período Contábil será ignorada.");
      return;
    }

    // 2. Buscar Período Contábil da empresa
    CompanyEntity company = companyRepository.findById(companyId)
        .orElseThrow(() -> new IllegalStateException(
            "Empresa não encontrada com ID: " + companyId));

    LocalDate periodoContabil = company.getPeriodoContabil();
    log.debug("PeriodoContabilAspect: Período Contábil da empresa {}: {}",
        companyId, periodoContabil);

    // 3. Verificar se algum argumento implementa TemporalEntity
    Object[] args = joinPoint.getArgs();
    for (Object arg : args) {
      if (arg instanceof TemporalEntity temporalEntity) {
        LocalDate competencia = temporalEntity.getCompetencia();
        log.debug("PeriodoContabilAspect: Encontrado TemporalEntity com competência: {}",
            competencia);

        // 4. Validar: competência deve ser >= periodoContabil
        if (competencia.isBefore(periodoContabil)) {
          log.warn("PeriodoContabilAspect: VIOLAÇÃO - Tentativa de editar registro com "
                  + "competência {} anterior ao Período Contábil {}",
              competencia, periodoContabil);
          throw new PeriodoContabilViolationException(competencia, periodoContabil);
        }

        log.debug("PeriodoContabilAspect: Validação OK - competência {} >= período {}",
            competencia, periodoContabil);
      }
    }
  }
}
