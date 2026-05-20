package br.com.lalurecf.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CompanyContext Unit Tests")
class CompanyContextTest {

    @AfterEach
    void tearDown() {
        // Always clear context after each test
        CompanyContext.clear();
    }

    @Test
    @DisplayName("Should set and get company ID")
    void shouldSetAndGetCompanyId() {
        // Arrange
        Long companyId = 123L;

        // Act
        CompanyContext.setCurrentCompanyId(companyId);
        Long result = CompanyContext.getCurrentCompanyId();

        // Assert
        assertEquals(companyId, result);
    }

    @Test
    @DisplayName("Should return null when no company ID is set")
    void shouldReturnNullWhenNoCompanyIdSet() {
        // Act
        Long result = CompanyContext.getCurrentCompanyId();

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Should clear company ID")
    void shouldClearCompanyId() {
        // Arrange
        CompanyContext.setCurrentCompanyId(123L);

        // Act
        CompanyContext.clear();
        Long result = CompanyContext.getCurrentCompanyId();

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Should overwrite previous company ID")
    void shouldOverwritePreviousCompanyId() {
        // Arrange
        CompanyContext.setCurrentCompanyId(100L);

        // Act
        CompanyContext.setCurrentCompanyId(200L);
        Long result = CompanyContext.getCurrentCompanyId();

        // Assert
        assertEquals(200L, result);
    }

    @Test
    @DisplayName("Should isolate company ID per thread (ThreadLocal behavior)")
    void shouldIsolateCompanyIdPerThread() throws InterruptedException {
        // Arrange
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<AssertionError> errors = new ArrayList<>();

        // Act - Each thread sets its own company ID
        for (int i = 0; i < threadCount; i++) {
            final Long companyId = Long.valueOf(i + 1);
            executor.submit(() -> {
                try {
                    CompanyContext.setCurrentCompanyId(companyId);

                    // Small delay to ensure threads overlap
                    Thread.sleep(50);

                    // Each thread should see its own company ID
                    Long result = CompanyContext.getCurrentCompanyId();
                    if (!companyId.equals(result)) {
                        errors.add(new AssertionError(
                            String.format("Expected %d but got %d", companyId, result)
                        ));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    CompanyContext.clear();
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Assert
        assertTrue(errors.isEmpty(), "ThreadLocal isolation failed: " + errors);
    }

    @Test
    @DisplayName("Should not cause memory leak after clear (ThreadLocal cleanup)")
    void shouldNotCauseMemoryLeakAfterClear() {
        // Arrange
        CompanyContext.setCurrentCompanyId(999L);

        // Act
        CompanyContext.clear();

        // Assert - After clear, context should be null
        assertNull(CompanyContext.getCurrentCompanyId());

        // Verify we can set a new value after clearing
        CompanyContext.setCurrentCompanyId(111L);
        assertEquals(111L, CompanyContext.getCurrentCompanyId());
    }
}
