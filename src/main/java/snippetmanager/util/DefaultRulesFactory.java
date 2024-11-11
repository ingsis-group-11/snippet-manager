package snippetmanager.util;

import java.util.List;
import snippetmanager.model.dtos.RuleDto;

public class DefaultRulesFactory {

  public static List<RuleDto> getDefaultFormatterRules() {
    return List.of(
        RuleDto.builder().name("line-breaks-after-println").value("0").isActive(false).build(),
        RuleDto.builder()
            .name("enforce-spacing-around-equals")
            .value("false")
            .isActive(false)
            .build(),
        RuleDto.builder()
            .name("enforce-spacing-before-colon-in-declaration")
            .isActive(false)
            .value("false")
            .build(),
        RuleDto.builder()
            .name("enforce-spacing-after-colon-in-declaration")
            .isActive(false)
            .value("false")
            .build(),
        RuleDto.builder().name("if-brace-same-line").value("true").isActive(false).build(),
        RuleDto.builder().name("indent-inside-if").value("2").isActive(false).build());
  }

  public static List<RuleDto> getDefaultLinterRules() {
    return List.of(
        RuleDto.builder()
            .name("mandatory-variable-or-literal-in-println")
            .value("false")
            .isActive(false)
            .build(),
        RuleDto.builder()
            .name("mandatory-variable-or-literal-in-readInput")
            .value("false")
            .isActive(false)
            .build(),
        RuleDto.builder().name("identifier_format").value("camel case").isActive(false).build());
  }
}
