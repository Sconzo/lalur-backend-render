package br.com.lalurecf.infrastructure.config;

import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * Configuração do DataSource para produção.
 *
 * <p>Converte automaticamente URLs do Render (postgres://) para formato JDBC (jdbc:postgresql://).
 * Isso permite usar DATABASE_URL diretamente sem conversão manual.
 */
@Configuration
@Profile("prod")
public class DataSourceConfig {

  /**
   * Cria DataSource convertendo URL do Render se necessário.
   *
   * <p>Render fornece DATABASE_URL no formato: postgresql://user:pass@host/db
   * Spring precisa: jdbc:postgresql://host:5432/db
   *
   * @param env environment com as propriedades
   * @return datasource configurado
   */
  @Bean
  public DataSource dataSource(Environment env) {
    String url = env.getProperty("spring.datasource.url");
    String username = env.getProperty("spring.datasource.username");
    String password = env.getProperty("spring.datasource.password");

    // Converter URL do Render para JDBC
    // Aceita: postgres://, postgresql://, ou jdbc:postgresql://
    if (url != null && (url.startsWith("postgres://") || url.startsWith("postgresql://"))) {
      // Extrair credenciais e host da URL se presentes
      String originalUrl = url;

      // Remover prefixo postgres:// ou postgresql://
      if (url.startsWith("postgres://")) {
        url = url.replace("postgres://", "");
      } else if (url.startsWith("postgresql://")) {
        url = url.replace("postgresql://", "");
      }

      // Verificar se há credenciais na URL (user:pass@host)
      if (url.contains("@")) {
        String[] parts = url.split("@", 2);
        if (parts.length == 2) {
          // Extrair credenciais
          String[] credentials = parts[0].split(":", 2);
          if (credentials.length == 2 && (username == null || username.isBlank())) {
            username = credentials[0];
            password = credentials[1];
          }
          // Host e database
          url = parts[1];
        }
      }

      // Adicionar porta padrão se não especificada
      if (!url.contains(":5432") && !url.contains(":")) {
        String[] hostDb = url.split("/", 2);
        if (hostDb.length == 2) {
          url = hostDb[0] + ":5432/" + hostDb[1];
        }
      }

      // Montar URL JDBC final
      url = "jdbc:postgresql://" + url;
    }

    return DataSourceBuilder
        .create()
        .url(url)
        .username(username)
        .password(password)
        .build();
  }
}
