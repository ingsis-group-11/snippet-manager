package snippet_manager.snippet.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import snippet_manager.snippet.util.CodeLanguage;

import java.time.LocalDateTime;
import java.util.UUID;

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
