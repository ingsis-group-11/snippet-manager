package snippetmanager.model.dtos.webservice;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Builder
@Setter
@Getter
public class PsTestCaseDto {
  private String language;
  private String version;
  private String content;
  private List<String> input;
  private List<String> output;
}
