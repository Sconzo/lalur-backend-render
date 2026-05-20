package br.com.lalurecf.infrastructure.exception;

/**
 * Exception lançada quando um recurso solicitado não é encontrado.
 *
 * <p>Usada para retornar HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(String resource, Long id) {
    super(String.format("%s com ID %d não encontrado", resource, id));
  }
}
