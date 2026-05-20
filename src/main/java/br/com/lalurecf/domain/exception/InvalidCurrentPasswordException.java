package br.com.lalurecf.domain.exception;

/**
 * Exception lançada quando senha atual fornecida está incorreta.
 *
 * <p>Usada na troca de senha quando usuário fornece senha atual inválida.
 */
public class InvalidCurrentPasswordException extends RuntimeException {

  public InvalidCurrentPasswordException(String message) {
    super(message);
  }
}
