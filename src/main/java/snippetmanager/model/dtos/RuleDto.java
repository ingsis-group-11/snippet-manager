package snippetmanager.model.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class RuleDto {
  private String name;
  private String value;
  private String id;
}
