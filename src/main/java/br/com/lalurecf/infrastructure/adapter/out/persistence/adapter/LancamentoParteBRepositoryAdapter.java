package br.com.lalurecf.infrastructure.adapter.out.persistence.adapter;

import br.com.lalurecf.application.port.out.LancamentoParteBRepositoryPort;
import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.enums.TipoAjuste;
import br.com.lalurecf.domain.enums.TipoApuracao;
import br.com.lalurecf.domain.model.LancamentoParteB;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.CompanyEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.ContaParteBEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.LancamentoParteBEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.PlanoDeContasEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.TaxParameterEntity;
import br.com.lalurecf.infrastructure.adapter.out.persistence.mapper.LancamentoParteBMapper;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.CompanyJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.ContaParteBJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.LancamentoParteBJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.PlanoDeContasJpaRepository;
import br.com.lalurecf.infrastructure.adapter.out.persistence.repository.TaxParameterJpaRepository;
import br.com.lalurecf.infrastructure.security.SpringSecurityAuditorAware;
import jakarta.persistence.criteria.Predicate;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
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
 * Adapter de persistência para LancamentoParteB.
 *
 * <p>Implementa LancamentoParteBRepositoryPort (hexagonal port OUT) usando Spring Data JPA como
 * tecnologia de persistência.
 *
 * <p>Responsabilidades:
 *
 * <ul>
 *   <li>Converter entre domain model (LancamentoParteB) e JPA entity (LancamentoParteBEntity)
 *   <li>Delegar operações de persistência ao LancamentoParteBJpaRepository
 *   <li>Resolver relacionamentos com CompanyEntity, PlanoDeContasEntity, ContaParteBEntity e
 *       TaxParameterEntity
 * </ul>
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class LancamentoParteBRepositoryAdapter implements LancamentoParteBRepositoryPort {

  private static final String BATCH_INSERT_SQL =
      "INSERT INTO tb_lancamento_parte_b "
          + "(company_id, mes_referencia, ano_referencia, tipo_apuracao, tipo_relacionamento, "
          + "conta_contabil_id, conta_parte_b_id, parametro_tributario_id, "
          + "tipo_ajuste, descricao, valor, "
          + "status, criado_em, atualizado_em, criado_por, atualizado_por) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', NOW(), NOW(), ?, ?)";

  private final LancamentoParteBJpaRepository jpaRepository;
  private final CompanyJpaRepository companyJpaRepository;
  private final PlanoDeContasJpaRepository planoDeContasJpaRepository;
  private final ContaParteBJpaRepository contaParteBJpaRepository;
  private final TaxParameterJpaRepository taxParameterJpaRepository;
  private final LancamentoParteBMapper mapper;
  private final JdbcTemplate jdbcTemplate;
  private final SpringSecurityAuditorAware auditorAware;

  @Override
  public LancamentoParteB save(LancamentoParteB lancamento) {
    LancamentoParteBEntity entity;

    if (lancamento.getId() != null) {
      // Update: busca entity existente e atualiza seus campos
      entity =
          jpaRepository
              .findById(lancamento.getId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "LancamentoParteB not found with id: " + lancamento.getId()));
      mapper.updateEntity(lancamento, entity);
    } else {
      // Create: converte domain para nova entity
      entity = mapper.toEntity(lancamento);

      // Resolver relacionamento com Company
      CompanyEntity company =
          companyJpaRepository
              .findById(lancamento.getCompanyId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Company not found with id: " + lancamento.getCompanyId()));
      entity.setCompany(company);

      // Resolver relacionamento com ContaContabil (se informado)
      if (lancamento.getContaContabilId() != null) {
        PlanoDeContasEntity contaContabil =
            planoDeContasJpaRepository
                .findById(lancamento.getContaContabilId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "ContaContabil not found with id: "
                                + lancamento.getContaContabilId()));
        entity.setContaContabil(contaContabil);
      }

      // Resolver relacionamento com ContaParteB (se informado)
      if (lancamento.getContaParteBId() != null) {
        ContaParteBEntity contaParteB =
            contaParteBJpaRepository
                .findById(lancamento.getContaParteBId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "ContaParteB not found with id: " + lancamento.getContaParteBId()));
        entity.setContaParteB(contaParteB);
      }

      // Resolver relacionamento com ParametroTributario (obrigatório)
      TaxParameterEntity parametroTributario =
          taxParameterJpaRepository
              .findById(lancamento.getParametroTributarioId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "ParametroTributario not found with id: "
                              + lancamento.getParametroTributarioId()));
      entity.setParametroTributario(parametroTributario);
    }

    LancamentoParteBEntity savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  @Override
  public void saveAll(List<LancamentoParteB> lancamentos) {
    final long auditorId = auditorAware.getCurrentAuditor().orElse(1L);
    jdbcTemplate.batchUpdate(
        BATCH_INSERT_SQL,
        new BatchPreparedStatementSetter() {
          @Override
          public void setValues(PreparedStatement ps, int i) throws SQLException {
            LancamentoParteB l = lancamentos.get(i);
            ps.setLong(1, l.getCompanyId());
            ps.setInt(2, l.getMesReferencia());
            ps.setInt(3, l.getAnoReferencia());
            ps.setString(4, l.getTipoApuracao().name());
            ps.setString(5, l.getTipoRelacionamento().name());
            if (l.getContaContabilId() != null) {
              ps.setLong(6, l.getContaContabilId());
            } else {
              ps.setNull(6, Types.BIGINT);
            }
            if (l.getContaParteBId() != null) {
              ps.setLong(7, l.getContaParteBId());
            } else {
              ps.setNull(7, Types.BIGINT);
            }
            ps.setLong(8, l.getParametroTributarioId());
            ps.setString(9, l.getTipoAjuste().name());
            ps.setString(10, l.getDescricao());
            ps.setBigDecimal(11, l.getValor());
            ps.setLong(12, auditorId);
            ps.setLong(13, auditorId);
          }

          @Override
          public int getBatchSize() {
            return lancamentos.size();
          }
        });
  }

  @Override
  public Optional<LancamentoParteB> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<LancamentoParteB> findByCompanyIdAndAnoReferencia(
      Long companyId, Integer anoReferencia) {
    return jpaRepository.findByCompanyIdAndAnoReferencia(companyId, anoReferencia).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<LancamentoParteB> findByCompanyIdAndAnoReferenciaAndStatus(
      Long companyId, Integer anoReferencia, Status status) {
    return jpaRepository
        .findByCompanyIdAndAnoReferenciaAndStatus(companyId, anoReferencia, status)
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Page<LancamentoParteB> findByCompanyId(Long companyId, Pageable pageable) {
    return jpaRepository.findByCompanyId(companyId, pageable).map(mapper::toDomain);
  }

  @Override
  public Page<LancamentoParteB> findFiltered(
      Long companyId,
      Integer anoReferencia,
      Integer mesReferencia,
      TipoApuracao tipoApuracao,
      TipoAjuste tipoAjuste,
      boolean includeInactive,
      Pageable pageable) {

    Specification<LancamentoParteBEntity> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(cb.equal(root.get("company").get("id"), companyId));

          if (anoReferencia != null) {
            predicates.add(cb.equal(root.get("anoReferencia"), anoReferencia));
          }
          if (mesReferencia != null) {
            predicates.add(cb.equal(root.get("mesReferencia"), mesReferencia));
          }
          if (tipoApuracao != null) {
            predicates.add(cb.equal(root.get("tipoApuracao"), tipoApuracao));
          }
          if (tipoAjuste != null) {
            predicates.add(cb.equal(root.get("tipoAjuste"), tipoAjuste));
          }
          if (!includeInactive) {
            predicates.add(cb.equal(root.get("status"), Status.ACTIVE));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };

    return jpaRepository.findAll(spec, pageable).map(mapper::toDomain);
  }

  @Override
  public List<LancamentoParteB> findByCompanyIdAndAnoReferenciaAndMesReferencia(
      Long companyId, Integer anoReferencia, Integer mesReferencia) {
    return jpaRepository
        .findByCompanyIdAndAnoReferenciaAndMesReferencia(companyId, anoReferencia, mesReferencia)
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public void deleteById(Long id) {
    jpaRepository.deleteById(id);
  }
}
