package br.com.lalurecf.domain.exception;

/**
 * Exception lançada quando usuário tenta fazer login mas deve trocar senha primeiro.
 *
 * <p>Usada quando mustChangePassword = true, impedindo login até troca de senha.
 */
public class MustChangePasswordException extends RuntimeException {

  public MustChangePasswordException(String message) {
    super(message);
  }
}
