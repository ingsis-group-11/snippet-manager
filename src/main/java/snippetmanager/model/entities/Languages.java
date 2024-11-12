package snippetmanager.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Getter
@Entity
public class Languages {
  @Id private String language;

  private String extension;
}
