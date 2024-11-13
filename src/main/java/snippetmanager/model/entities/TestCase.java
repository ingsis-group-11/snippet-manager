package snippetmanager.model.entities;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TestCase {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String assetId;

  private String name;

  @ElementCollection private List<String> inputs;

  @ElementCollection private List<String> outputs;
}
