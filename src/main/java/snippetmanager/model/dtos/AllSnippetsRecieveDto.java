package snippetmanager.model.dtos;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
@Getter
@Setter
public class AllSnippetsRecieveDto {
  private List<String> snippetsIds;
  private int maxSnippets;
}
