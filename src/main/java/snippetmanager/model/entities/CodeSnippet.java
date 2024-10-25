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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import snippetmanager.util.CodeLanguage;

@Entity
@Getter
@Setter
public class CodeSnippet {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String assetId = UUID.randomUUID().toString();

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

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
