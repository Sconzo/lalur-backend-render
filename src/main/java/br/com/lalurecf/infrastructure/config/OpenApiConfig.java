package br.com.lalurecf.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do OpenAPI/Swagger para documentação da API.
 *
 * <p>Define informações gerais da API, esquema de segurança JWT e header X-Company-Id global.
 */
@Configuration
public class OpenApiConfig {

  /** Configuração personalizada do OpenAPI com segurança JWT e info da API. */
  @Bean
  public OpenAPI customOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("LALUR V2 ECF API")
                .version("1.0.0")
                .description(
                    "API para escrituração contábil fiscal - cálculos de IRPJ e CSLL")
                .contact(
                    new Contact()
                        .name("LALUR V2 Team")
                        .email("support@lalurecf.com.br")
                        .url("https://lalurecf.com.br")))
        .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
        .schemaRequirement(
            "bearer-jwt",
            new SecurityScheme()
                .name("bearer-jwt")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description(
                    "JWT token de autenticação. Obtenha via POST /api/v1/auth/login"));
  }

  /** Adiciona header X-Company-Id como parâmetro global em todos os endpoints. */
  @Bean
  public OperationCustomizer globalHeaderCustomizer() {
    return (operation, handlerMethod) -> {
      operation.addParametersItem(
          new Parameter()
              .in("header")
              .name("X-Company-Id")
              .description("ID da empresa (obrigatório para endpoints protegidos)")
              .required(false)
              .schema(new io.swagger.v3.oas.models.media.IntegerSchema()));
      return operation;
    };
  }
}
