package snippetmanager.model.dtos;

import jakarta.persistence.ElementCollection;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class TestCaseDto {

  private String testId;

  private String name;

  @ElementCollection private List<String> input;

  @ElementCollection private List<String> output;
}
