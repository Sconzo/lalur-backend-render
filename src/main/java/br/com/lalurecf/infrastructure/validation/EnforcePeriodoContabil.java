package br.com.lalurecf.infrastructure.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation para marcar métodos que devem validar Período Contábil.
 * Métodos anotados devem garantir que operações só ocorram em lançamentos
 * com data >= company.periodoContabil.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnforcePeriodoContabil {
}
