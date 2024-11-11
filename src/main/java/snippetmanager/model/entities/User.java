package snippetmanager.model.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Table(name = "app_user")
@Entity
@Getter
@Setter
public class User {
  @Id private String userId;

  private String name;
}
