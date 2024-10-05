package snippet_manager.snippet.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;
import snippet_manager.snippet.util.CodeLanguage;
import snippet_manager.snippet.util.StringToMultipartFile;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class CodeSnippet {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;

  @Enumerated(EnumType.STRING)
  private CodeLanguage language;

  @Lob
  @Column(columnDefinition = "TEXT")
  private String content;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public MultipartFile getContentInMultipartFile() {
    return new StringToMultipartFile(content, title, title, "text/plain");
  }

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
