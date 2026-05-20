package br.com.lalurecf.infrastructure.adapter.out.persistence.repository;

import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.infrastructure.adapter.out.persistence.entity.EcfFileEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA Repository para EcfFileEntity.
 *
 * <p>Query methods para busca de arquivos ECF por empresa, ano e tipo.
 */
@Repository
public interface EcfFileJpaRepository extends JpaRepository<EcfFileEntity, Long> {

  /**
   * Busca arquivo ECF por empresa, ano fiscal e tipo.
   *
   * <p>Usado pelo saveOrReplace para verificar existência antes de insert/update.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @param fileType tipo do arquivo
   * @return Optional com entity se encontrada
   */
  Optional<EcfFileEntity> findByCompanyIdAndFiscalYearAndFileType(
      Long companyId, Integer fiscalYear, EcfFileType fileType);

  /**
   * Busca todos os arquivos ECF de uma empresa para um ano fiscal.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @return lista de entities
   */
  List<EcfFileEntity> findByCompanyIdAndFiscalYear(Long companyId, Integer fiscalYear);
}
