package snippetmanager.model.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class LanguagesDto {
  private String language;
  private String extension;
}
