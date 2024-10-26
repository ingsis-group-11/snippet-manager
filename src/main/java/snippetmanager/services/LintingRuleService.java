package snippetmanager.services;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import snippetmanager.model.dtos.RuleDto;
import snippetmanager.model.entities.LintingRule;
import snippetmanager.repositories.LintingRuleRepository;

@Service
public class LintingRuleService {
  @Autowired private LintingRuleRepository lintingRuleRepository;

  public String createOrUpdateRules(List<RuleDto> rules, String userId) {
    String response = "";
    for (RuleDto ruleDto : rules) {
      Optional<LintingRule> rule = searchRule(ruleDto.getName(), userId);
      if (rule.isPresent()) {
        rule.get().setValue(ruleDto.getValue());
        lintingRuleRepository.save(rule.get());
        response = "Rules updated successfully";
      } else {
        createAndSaveRule(userId, ruleDto);
        response = "Rules created successfully";
      }
    }
    return response;
  }

  private void createAndSaveRule(String userId, RuleDto ruleDto) {
    LintingRule newRule = new LintingRule();
    newRule.setName(ruleDto.getName());
    newRule.setValue(ruleDto.getValue());
    newRule.setUserId(userId);
    lintingRuleRepository.save(newRule);
  }

  private Optional<LintingRule> searchRule(String ruleName, String userId) {
    return lintingRuleRepository.findByNameAndUserId(ruleName, userId);
  }

  public List<RuleDto> getRules(String userId) {
    return lintingRuleRepository.findAllByUserId(userId);
  }
}
