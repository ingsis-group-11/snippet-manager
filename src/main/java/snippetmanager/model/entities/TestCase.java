package snippetmanager.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Entity
@Getter
@Setter
public class TestCase {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String assetId;

  private String name;

  @ElementCollection
  private List<String> inputs;

  @ElementCollection
  private List<String> outputs;
}
