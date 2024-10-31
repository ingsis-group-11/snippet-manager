package snippetmanager.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import snippetmanager.util.CodeLanguage;
import snippetmanager.util.LintResult;

@Entity
@Getter
@Setter
public class CodeSnippet {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String assetId;

  @Enumerated(EnumType.STRING)
  private CodeLanguage language;

  private String version;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = createdAt;
  }

  private LintResult lintResult;

  public void setResultAsString(String result) {
    String resultEnum = result.toUpperCase();
    this.lintResult = LintResult.valueOf(resultEnum);
  }

  public String getResultAsString() {
    return lintResult.toString();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}