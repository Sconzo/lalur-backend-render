package br.com.lalurecf.infrastructure.security;

/**
 * Thread-safe context holder for current fiscal year.
 *
 * <p>This class uses ThreadLocal to store the fiscal year for the current request thread.
 * It is primarily used to maintain fiscal year context when processing requests with
 * the X-Fiscal-Year header.
 *
 * <p><strong>CRITICAL:</strong> Always call {@link #clear()} in a finally block to prevent
 * memory leaks in thread pool environments.
 *
 * @see FiscalYearContextFilter
 */
public final class FiscalYearContext {

  private static final ThreadLocal<Integer> currentFiscalYear = new ThreadLocal<>();

  private FiscalYearContext() {
    // Utility class - prevent instantiation
  }

  /**
   * Sets the current fiscal year for this thread.
   *
   * @param fiscalYear the fiscal year to set (must not be null)
   */
  public static void setCurrentFiscalYear(Integer fiscalYear) {
    currentFiscalYear.set(fiscalYear);
  }

  /**
   * Gets the current fiscal year for this thread.
   *
   * @return the fiscal year, or null if not set
   */
  public static Integer getCurrentFiscalYear() {
    return currentFiscalYear.get();
  }

  /**
   * Clears the current fiscal year from this thread.
   *
   * <p><strong>MUST</strong> be called in a finally block after request processing
   * to prevent memory leaks.
   */
  public static void clear() {
    currentFiscalYear.remove();
  }
}
