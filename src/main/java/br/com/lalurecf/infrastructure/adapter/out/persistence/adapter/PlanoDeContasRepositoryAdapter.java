package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.PlanoDeContasRepositoryPort;
import br.com.lalurecf.domain.enums.AccountType;
import br.com.lalurecf.domain.enums.ClasseContabil;
import br.com.lalurecf.domain.enums.NaturezaConta;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.PlanoDeContas;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.PlanoDeContasMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaReferencialJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PlanoDeContasJpaRepository;
import br.com.lalurecf.infrastructure.security.SpringSecurityAuditorAware;
import jakarta.persistence.criteria.Predicate;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter de persistência para PlanoDeContas.
 *
 * <p>Implementa PlanoDeContasRepositoryPort (hexagonal port OUT) usando Spring Data JPA como
 * tecnologia de persistência.
 *
 * <p>Responsabilidades:
 *
 * <ul>
 *   <li>Converter entre domain model (PlanoDeContas) e JPA entity (PlanoDeContasEntity)
 *   <li>Delegar operações de persistência ao PlanoDeContasJpaRepository
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PlanoDeContasRepositoryAdapter implements PlanoDeContasRepositoryPort {

  private static final String BATCH_INSERT_SQL =
      "INSERT INTO tb_plano_de_contas "
          + "(company_id, conta_referencial_id, code, name, fiscal_year, "
          + "account_type, classe, nivel, natureza, afeta_resultado, dedutivel, "
          + "status, criado_em, atualizado_em, criado_por, atualizado_por) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW(), ?, ?)";

  private final PlanoDeContasJpaRepository jpaRepository;
  private final PlanoDeContasMapper mapper;
  private final CompanyJpaRepository companyJpaRepository;
  private final ContaReferencialJpaRepository contaReferencialJpaRepository;
  private final JdbcTemplate jdbcTemplate;
  private final SpringSecurityAuditorAware auditorAware;

  @Override
  public PlanoDeContas save(PlanoDeContas account) {
    PlanoDeContasEntity entity;

    if (account.getId() != null) {
      // Update: busca entity existente e atualiza seus campos
      entity =
          jpaRepository
              .findById(account.getId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "PlanoDeContas not found with id: " + account.getId()));
      mapper.updateEntity(account, entity);
    } else {
      // Create: converte domain para nova entity
      entity = mapper.toEntity(account);
    }

    // Definir referências gerenciadas via getReferenceById para evitar instâncias transientes
    entity.setCompany(companyJpaRepository.getReferenceById(account.getCompanyId()));
    if (account.getContaReferencialId() != null) {
      entity.setContaReferencial(
          contaReferencialJpaRepository.getReferenceById(account.getContaReferencialId()));
    } else {
      entity.setContaReferencial(null);
    }

    PlanoDeContasEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public void saveAll(List<PlanoDeContas> accounts) {
    final long auditorId = auditorAware.getCurrentAuditor().orElse(1L);
    jdbcTemplate.batchUpdate(
        BATCH_INSERT_SQL,
        new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            PlanoDeContas a = accounts.get(i);
            ps.setLong(1, a.getCompanyId());
            if (a.getContaReferencialId() != null) {
              ps.setLong(2, a.getContaReferencialId());
            } else {
              ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, a.getCode());
            ps.setString(4, a.getName());
            ps.setInt(5, a.getFiscalYear());
            ps.setString(6, a.getAccountType().name());
            ps.setString(7, a.getClasse().name());
            ps.setInt(8, a.getNivel());
            ps.setString(9, a.getNatureza().name());
            ps.setBoolean(10, a.getAfetaResultado());
            ps.setBoolean(11, a.getDedutivel());
            ps.setLong(12, auditorId);
            ps.setLong(13, auditorId);
          }

          @Override
          public int getBatchSize() {
            return accounts.size();
          }
        });
  }

  @Override
  public Optional<PlanoDeContas> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<PlanoDeContas> findAllById(Collection<Long> ids) {
    return jpaRepository.findAllById(ids).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<PlanoDeContas> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear) {
    return jpaRepository.findByCompanyIdAndFiscalYear(companyId, fiscalYear).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<PlanoDeContas> findByCompanyIdAndCodeAndFiscalYear(
      Long companyId, String code, Integer fiscalYear) {
    return jpaRepository
        .findByCompanyIdAndCodeAndFiscalYear(companyId, code, fiscalYear)
        .map(mapper::toDomain);
  }

  @Override
  public void deleteById(Long id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public Page<PlanoDeContas> findByCompanyId(Long companyId, Pageable pageable) {
    return jpaRepository.findByCompanyId(companyId, pageable).map(mapper::toDomain);
  }

  @Override
  public boolean existsActiveByCompanyId(Long companyId) {
    return jpaRepository.existsByCompanyIdAndStatus(companyId, Status.ACTIVE);
  }

  @Override
  public Page<PlanoDeContas> findFiltered(
      Long companyId,
      Integer fiscalYear,
      AccountType accountType,
      ClasseContabil classe,
      NaturezaConta natureza,
      String search,
      Integer nivel,
      boolean includeInactive,
      Pageable pageable) {

    Specification<PlanoDeContasEntity> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("company").get("id"), companyId));

          if (fiscalYear != null) {
            predicates.add(cb.equal(root.get("fiscalYear"), fiscalYear));
          }
          if (accountType != null) {
            predicates.add(cb.equal(root.get("accountType"), accountType));
          }
          if (classe != null) {
            predicates.add(cb.equal(root.get("classe"), classe));
          }
          if (natureza != null) {
            predicates.add(cb.equal(root.get("natureza"), natureza));
          }
          if (search != null && !search.trim().isEmpty()) {
            String pattern = "%" + search.toLowerCase() + "%";
            predicates.add(
                cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern)));
          }
          if (nivel != null) {
            predicates.add(cb.equal(root.get("nivel"), nivel));
          }
          if (!includeInactive) {
            predicates.add(cb.equal(root.get("status"), Status.ACTIVE));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    return jpaRepository.findAll(spec, pageable).map(mapper::toDomain);
  }
}
