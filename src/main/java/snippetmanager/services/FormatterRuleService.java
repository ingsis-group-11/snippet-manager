package snippetmanager.services;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import snippetmanager.model.dtos.RuleDto;
import snippetmanager.model.entities.FormatterRule;
import snippetmanager.repositories.FormatterRuleRepository;

@Service
public class FormatterRuleService {
  @Autowired private FormatterRuleRepository formatterRuleRepository;

  public String createOrUpdateRules(List<RuleDto> rules, String userId) {
    String response = "";
    for (RuleDto ruleDto : rules) {
      Optional<FormatterRule> rule = searchRule(ruleDto.getName(), userId);
      if (rule.isPresent()) {
        rule.get().setValue(ruleDto.getValue());
        formatterRuleRepository.save(rule.get());
        response = "Rules updated successfully";
      } else {
        createAndSaveRule(userId, ruleDto);
        response = "Rules created successfully";
      }
    }
    return response;
  }

  private void createAndSaveRule(String userId, RuleDto ruleDto) {
    FormatterRule newRule = new FormatterRule();
    newRule.setName(ruleDto.getName());
    newRule.setValue(ruleDto.getValue());
    newRule.setUserId(userId);
    formatterRuleRepository.save(newRule);
  }

  private Optional<FormatterRule> searchRule(String ruleName, String userId) {
    return formatterRuleRepository.findByNameAndUserId(ruleName, userId);
  }

  public List<RuleDto> getRules(String userId) {
    return formatterRuleRepository.findAllByUserId(userId);
  }
}
