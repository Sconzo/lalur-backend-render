package br.com.lalurecf.domain.exception;

/**
 * Exception lançada quando credenciais de autenticação são inválidas.
 *
 * <p>Usada quando email não existe ou senha está incorreta.
 */
public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException(String message) {
    super(message);
  }
}
