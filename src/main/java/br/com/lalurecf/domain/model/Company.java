package br.com.lalurecf.domain.model;

import br.com.lalurecf.domain.enums.Status;
import br.com.lalurecf.domain.model.valueobject.CNPJ;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a Company (Empresa).
 * Pure POJO without framework dependencies (no Spring/JPA annotations).
 */
public class Company {

  private Long id;
  private CNPJ cnpj;
  private String razaoSocial;
  private LocalDate periodoContabil;
  private String mascaraNiveis;
  private Status status;
  // CNAE, Qualificação PJ e Natureza Jurídica são gerenciados como
  // parâmetros tributários (ADR-001 v2.0)
  private Long createdBy;
  private LocalDateTime createdAt;
  private Long updatedBy;
  private LocalDateTime updatedAt;

  /**
   * Default constructor.
   */
  public Company() {
  }

  /**
   * Full constructor with all fields.
   */
  public Company(Long id, CNPJ cnpj, String razaoSocial,
                 LocalDate periodoContabil, String mascaraNiveis, Status status,
                 Long createdBy, LocalDateTime createdAt,
                 Long updatedBy, LocalDateTime updatedAt) {
    this.id = id;
    this.cnpj = cnpj;
    this.razaoSocial = razaoSocial;
    this.periodoContabil = periodoContabil;
    this.mascaraNiveis = mascaraNiveis;
    this.status = status;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.updatedBy = updatedBy;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public CNPJ getCnpj() {
    return cnpj;
  }

  public void setCnpj(CNPJ cnpj) {
    this.cnpj = cnpj;
  }

  public String getRazaoSocial() {
    return razaoSocial;
  }

  public void setRazaoSocial(String razaoSocial) {
    this.razaoSocial = razaoSocial;
  }

  public LocalDate getPeriodoContabil() {
    return periodoContabil;
  }

  public void setPeriodoContabil(LocalDate periodoContabil) {
    this.periodoContabil = periodoContabil;
  }

  public String getMascaraNiveis() {
    return mascaraNiveis;
  }

  public void setMascaraNiveis(String mascaraNiveis) {
    this.mascaraNiveis = mascaraNiveis;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public Long getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(Long createdBy) {
    this.createdBy = createdBy;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Long getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(Long updatedBy) {
    this.updatedBy = updatedBy;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Company company = (Company) o;
    return Objects.equals(id, company.id) && Objects.equals(cnpj, company.cnpj);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, cnpj);
  }

  @Override
  public String toString() {
    return "Company{"
        + "id=" + id
        + ", cnpj=" + cnpj
        + ", razaoSocial='" + razaoSocial + '\''
        + ", status=" + status
        + '}';
  }
}
