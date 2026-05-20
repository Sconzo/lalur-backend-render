package br.com.lalurecf.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de cache da aplicação usando Caffeine.
 *
 * <p>Configura caches com TTL e tamanho máximo para otimizar performance
 * e reduzir chamadas a APIs externas.
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /**
   * Configura o CacheManager com Caffeine.
   *
   * <p>Cache "cnpj-data":
   * <ul>
   *   <li>TTL: 24 horas após escrita
   *   <li>Tamanho máximo: 1000 entradas
   *   <li>Usado para cachear consultas de CNPJ na BrasilAPI
   * </ul>
   *
   * @return CacheManager configurado
   */
  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager("cnpj-data");
    cacheManager.setCaffeine(caffeineCacheBuilder());
    return cacheManager;
  }

  /**
   * Constrói configuração do Caffeine para o cache.
   *
   * @return Caffeine builder configurado
   */
  private Caffeine<Object, Object> caffeineCacheBuilder() {
    return Caffeine.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .maximumSize(1000)
        .recordStats();
  }
}
