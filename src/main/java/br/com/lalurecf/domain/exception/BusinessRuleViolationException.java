package br.com.lalurecf.domain.exception;

/**
 * Exception lançada quando uma regra de negócio é violada.
 *
 * <p>Usada para validações de domínio que não têm exception específica.
 */
public class BusinessRuleViolationException extends RuntimeException {

  public BusinessRuleViolationException(String message) {
    super(message);
  }
}
