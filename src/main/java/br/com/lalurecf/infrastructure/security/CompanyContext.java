package br.com.lalurecf.infrastructure.security;

/**
 * Thread-safe context holder for current company ID.
 * <p>
 * This class uses ThreadLocal to store the company ID for the current request thread.
 * It is primarily used to maintain company context when processing requests with
 * the X-Company-Id header.
 * </p>
 * <p>
 * <strong>CRITICAL:</strong> Always call {@link #clear()} in a finally block to prevent
 * memory leaks in thread pool environments.
 * </p>
 *
 * @see CompanyContextFilter
 */
public final class CompanyContext {

  private static final ThreadLocal<Long> currentCompanyId = new ThreadLocal<>();

  private CompanyContext() {
    // Utility class - prevent instantiation
  }

  /**
   * Sets the current company ID for this thread.
   *
   * @param companyId the company ID to set (must not be null)
   */
  public static void setCurrentCompanyId(Long companyId) {
    currentCompanyId.set(companyId);
  }

  /**
   * Gets the current company ID for this thread.
   *
   * @return the company ID, or null if not set
   */
  public static Long getCurrentCompanyId() {
    return currentCompanyId.get();
  }

  /**
   * Clears the current company ID from this thread.
   * <p>
   * <strong>MUST</strong> be called in a finally block after request processing
   * to prevent memory leaks.
   * </p>
   */
  public static void clear() {
    currentCompanyId.remove();
  }
}
