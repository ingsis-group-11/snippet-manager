package snippetmanager.model.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserDto {
  private String userId;
  private String name;
}
