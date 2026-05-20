package br.com.lalurecf.application.port.out;

import br.com.lalurecf.domain.enums.EcfFileType;
import br.com.lalurecf.domain.model.EcfFile;
import java.util.List;
import java.util.Optional;

/**
 * Port OUT para persistência de EcfFile.
 *
 * <p>Define as operações de persistência necessárias para o domínio ECF.
 * Implementado pela camada de infraestrutura (EcfFileRepositoryAdapter).
 */
public interface EcfFileRepositoryPort {

  /**
   * Upsert por (fileType, companyId, fiscalYear).
   *
   * <p>Se já existir um arquivo do mesmo tipo para a mesma empresa e ano fiscal,
   * atualiza o registro existente. Caso contrário, insere um novo.
   *
   * @param ecfFile arquivo a salvar ou substituir
   * @return arquivo persistido com ID preenchido
   */
  EcfFile saveOrReplace(EcfFile ecfFile);

  /**
   * Busca arquivo ECF por ID.
   *
   * @param id identificador do arquivo
   * @return Optional com o arquivo, ou vazio se não encontrado
   */
  Optional<EcfFile> findById(Long id);

  /**
   * Busca todos os arquivos ECF de uma empresa para um ano fiscal.
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @return lista de arquivos (pode ser vazia)
   */
  List<EcfFile> findByCompanyAndFiscalYear(Long companyId, Integer fiscalYear);

  /**
   * Busca arquivo ECF por empresa, ano fiscal e tipo.
   *
   * <p>Retorna no máximo um resultado dado o unique constraint (fileType, companyId, fiscalYear).
   *
   * @param companyId ID da empresa
   * @param fiscalYear ano fiscal
   * @param type tipo do arquivo
   * @return Optional com o arquivo, ou vazio se não existir
   */
  Optional<EcfFile> findByCompanyAndFiscalYearAndType(
      Long companyId, Integer fiscalYear, EcfFileType type);
}
