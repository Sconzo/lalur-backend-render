package br.com.lalurecf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for LALUR V2 ECF System.
 *
 * <p>Sistema de escrituração contábil fiscal (ECF) para cálculos de IRPJ e CSLL. Arquitetura
 * hexagonal (Ports & Adapters) com separação de camadas.
 */
@SpringBootApplication
public class EcfApplication {

  public static void main(String[] args) {
    SpringApplication.run(EcfApplication.class, args);
  }
}
