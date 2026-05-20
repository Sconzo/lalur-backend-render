package br.com.lalurecf.infrastructure.dto.ecf;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado de validação de arquivo ECF.
 *
 * <p>Acumula erros (que invalidam o arquivo) e avisos (que não impedem o uso mas indicam
 * possíveis inconsistências). {@code valid = false} assim que qualquer erro é adicionado.
 */
public class ValidationResult {

  private boolean valid = true;
  private final List<String> errors = new ArrayList<>();
  private final List<String> warnings = new ArrayList<>();

  /**
   * Adiciona um erro de validação e marca o resultado como inválido.
   *
   * @param message descrição do erro encontrado
   */
  public void addError(String message) {
    this.valid = false;
    this.errors.add(message);
  }

  /**
   * Adiciona um aviso de validação (não invalida o arquivo).
   *
   * @param message descrição do aviso
   */
  public void addWarning(String message) {
    this.warnings.add(message);
  }

  public boolean isValid() {
    return valid;
  }

  public List<String> getErrors() {
    return errors;
  }

  public List<String> getWarnings() {
    return warnings;
  }
}
