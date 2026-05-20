package br.com.lalurecf.infrastructure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation para marcar métodos que devem validar o Período Contábil antes de executar.
 *
 * <p>Quando aplicada a um método de service que edita ou exclui uma entidade temporal
 * (que implementa {@link br.com.lalurecf.domain.model.TemporalEntity}), o
 * {@link br.com.lalurecf.infrastructure.aspect.PeriodoContabilAspect} interceptará
 * a chamada e validará se a competência do registro é posterior ao Período Contábil
 * da empresa.
 *
 * <p>Se a competência for anterior ao Período Contábil, uma
 * {@link br.com.lalurecf.infrastructure.exception.PeriodoContabilViolationException}
 * será lançada.
 *
 * <p>Exemplo de uso:
 * <pre>
 * &#64;EnforcePeriodoContabil
 * public void updateAccountingEntry(Long id, UpdateRequest request) {
 *   // Aspect validará antes de executar este método
 *   // ...
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnforcePeriodoContabil {
  // Annotation marker - sem propriedades
}
