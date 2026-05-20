package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.LancamentoContabilRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.LancamentoContabil;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.LancamentoContabilEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.LancamentoContabilMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.LancamentoContabilJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PlanoDeContasJpaRepository;
import br.com.lalurecf.infrastructure.security.SpringSecurityAuditorAware;
import jakarta.persistence.criteria.Predicate;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter que implementa LancamentoContabilRepositoryPort.
 *
 * <p>Responsável por:
 *
 * <ul>
 *   <li>Converter entre domain models e JPA entities
 *   <li>Resolver FKs (company, contaDebito, contaCredito)
 *   <li>Delegar operações ao JPA repository
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LancamentoContabilRepositoryAdapter implements LancamentoContabilRepositoryPort {

  private static final String BATCH_INSERT_SQL =
      "INSERT INTO tb_lancamento_contabil "
          + "(company_id, conta_debito_id, conta_credito_id, data, valor, historico, "
          + "numero_documento, fiscal_year, "
          + "status, criado_em, atualizado_em, criado_por, atualizado_por) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW(), ?, ?)";

  private final LancamentoContabilJpaRepository jpaRepository;
  private final CompanyJpaRepository companyRepository;
  private final PlanoDeContasJpaRepository planoDeContasRepository;
  private final LancamentoContabilMapper mapper;
  private final JdbcTemplate jdbcTemplate;
  private final SpringSecurityAuditorAware auditorAware;

  @Override
  public LancamentoContabil save(LancamentoContabil lancamento) {
    log.debug("Saving LancamentoContabil for company: {}", lancamento.getCompanyId());

    // Converter para entity
    LancamentoContabilEntity entity =
        (lancamento.getId() != null)
            ? jpaRepository
                .findById(lancamento.getId())
                .orElseGet(() -> mapper.toEntity(lancamento))
            : mapper.toEntity(lancamento);

    // Resolver FK: company
    CompanyEntity company =
        companyRepository
            .findById(lancamento.getCompanyId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Company not found with id: " + lancamento.getCompanyId()));
    entity.setCompany(company);

    // Resolver FK: contaDebito (opcional)
    if (lancamento.getContaDebitoId() != null) {
      PlanoDeContasEntity contaDebito =
          planoDeContasRepository
              .findById(lancamento.getContaDebitoId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Conta de débito not found with id: " + lancamento.getContaDebitoId()));
      entity.setContaDebito(contaDebito);
    } else {
      entity.setContaDebito(null);
    }

    // Resolver FK: contaCredito (opcional)
    if (lancamento.getContaCreditoId() != null) {
      PlanoDeContasEntity contaCredito =
          planoDeContasRepository
              .findById(lancamento.getContaCreditoId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Conta de crédito not found with id: "
                              + lancamento.getContaCreditoId()));
      entity.setContaCredito(contaCredito);
    } else {
      entity.setContaCredito(null);
    }

    // Copiar campos do domain para entity
    entity.setData(lancamento.getData());
    entity.setValor(lancamento.getValor());
    entity.setHistorico(lancamento.getHistorico());
    entity.setNumeroDocumento(lancamento.getNumeroDocumento());
    entity.setFiscalYear(lancamento.getFiscalYear());
    entity.setStatus(lancamento.getStatus() != null ? lancamento.getStatus() : Status.ACTIVE);

    // Salvar
    LancamentoContabilEntity saved = jpaRepository.save(entity);

    log.debug("LancamentoContabil saved with id: {}", saved.getId());
    return mapper.toDomain(saved);
  }

  @Override
  public void saveAll(List<LancamentoContabil> lancamentos) {
    final long auditorId = auditorAware.getCurrentAuditor().orElse(1L);
    jdbcTemplate.batchUpdate(
        BATCH_INSERT_SQL,
        new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            LancamentoContabil l = lancamentos.get(i);
            ps.setLong(1, l.getCompanyId());
            if (l.getContaDebitoId() != null) {
              ps.setLong(2, l.getContaDebitoId());
            } else {
              ps.setNull(2, Types.BIGINT);
            }
            if (l.getContaCreditoId() != null) {
              ps.setLong(3, l.getContaCreditoId());
            } else {
              ps.setNull(3, Types.BIGINT);
            }
            ps.setDate(4, Date.valueOf(l.getData()));
            ps.setBigDecimal(5, l.getValor());
            ps.setString(6, l.getHistorico());
            if (l.getNumeroDocumento() != null) {
              ps.setString(7, l.getNumeroDocumento());
            } else {
              ps.setNull(7, Types.VARCHAR);
            }
            ps.setInt(8, l.getFiscalYear());
            ps.setLong(9, auditorId);
            ps.setLong(10, auditorId);
          }

          @Override
          public int getBatchSize() {
            return lancamentos.size();
          }
        });
  }

  @Override
  public Optional<LancamentoContabil> findById(Long id) {
    log.debug("Finding LancamentoContabil by id: {}", id);
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<LancamentoContabil> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear) {
    log.debug(
        "Finding LancamentosContabeis by companyId: {} and fiscalYear: {}", companyId, fiscalYear);
    return jpaRepository.findByCompanyIdAndFiscalYear(companyId, fiscalYear).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Page<LancamentoContabil> findByCompanyId(Long companyId, Pageable pageable) {
    log.debug("Finding LancamentosContabeis by companyId: {} with pagination", companyId);
    return jpaRepository.findByCompanyId(companyId, pageable).map(mapper::toDomain);
  }

  @Override
  public List<LancamentoContabil> findForExport(
      Long companyId, Integer fiscalYear, LocalDate dataInicio, LocalDate dataFim) {
    log.debug(
        "Finding LancamentosContabeis for export: companyId={}, fiscalYear={}, range=[{}, {}]",
        companyId, fiscalYear, dataInicio, dataFim);
    return jpaRepository
        .findForExport(companyId, fiscalYear, Status.ACTIVE, dataInicio, dataFim)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Page<LancamentoContabil> findFiltered(
      Long companyId,
      Long contaDebitoId,
      Long contaCreditoId,
      LocalDate data,
      LocalDate dataInicio,
      LocalDate dataFim,
      Integer fiscalYear,
      boolean includeInactive,
      Pageable pageable) {
    log.debug("Finding filtered LancamentosContabeis by companyId: {} with pagination", companyId);

    Specification<LancamentoContabilEntity> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("company").get("id"), companyId));

          if (contaDebitoId != null) {
            predicates.add(cb.equal(root.get("contaDebito").get("id"), contaDebitoId));
          }
          if (contaCreditoId != null) {
            predicates.add(cb.equal(root.get("contaCredito").get("id"), contaCreditoId));
          }
          if (data != null) {
            predicates.add(cb.equal(root.get("data"), data));
          }
          if (dataInicio != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("data"), dataInicio));
          }
          if (dataFim != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("data"), dataFim));
          }
          if (fiscalYear != null) {
            predicates.add(cb.equal(root.get("fiscalYear"), fiscalYear));
          }
          if (!includeInactive) {
            predicates.add(cb.equal(root.get("status"), Status.ACTIVE));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    return jpaRepository.findAll(spec, pageable).map(mapper::toDomain);
  }

  @Override
  public int deleteByCompanyIdAndMesAndAno(Long companyId, Integer mes, Integer ano) {
    log.debug(
        "Deleting LancamentosContabeis for companyId: {}, mes: {}, ano: {}", companyId, mes, ano);
    return jpaRepository.deleteByCompanyIdAndMesAndAno(companyId, mes, ano);
  }

  @Override
  public void deleteById(Long id) {
    log.debug("Soft deleting LancamentoContabil with id: {}", id);
    LancamentoContabilEntity entity =
        jpaRepository
            .findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException("LancamentoContabil not found with id: " + id));
    entity.setStatus(Status.INACTIVE);
    jpaRepository.save(entity);
    log.debug("LancamentoContabil soft deleted with id: {}", id);
  }
}
