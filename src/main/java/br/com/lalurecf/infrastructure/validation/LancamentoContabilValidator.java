package br.com.lalurecf.infrastructure.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation para validação customizada de Lançamento Contábil.
 *
 * <p>Valida regras de negócio de partidas dobradas:
 *
 * <ul>
 *   <li>Conta de débito deve ser diferente da conta de crédito
 *   <li>Valor deve ser maior que zero
 * </ul>
 *
 * <p>Uso: Anotar DTO request classes que representam lançamentos contábeis.
 *
 * <pre>
 * &#64;LancamentoContabilValidator
 * public class CreateLancamentoContabilRequest {
 *   private Long contaDebitoId;
 *   private Long contaCreditoId;
 *   private BigDecimal valor;
 *   // ...
 * }
 * </pre>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LancamentoContabilValidatorImpl.class)
@Documented
public @interface LancamentoContabilValidator {

  /**
   * Mensagem de erro padrão.
   *
   * @return mensagem de erro
   */
  String message() default "Lançamento contábil inválido";

  /**
   * Grupos de validação.
   *
   * @return grupos
   */
  Class<?>[] groups() default {};

  /**
   * Payload adicional.
   *
   * @return payload
   */
  Class<? extends Payload>[] payload() default {};
}
