package snippetmanager.services;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import snippetmanager.model.dtos.RuleDto;
import snippetmanager.model.dtos.SnippetSendDto;
import snippetmanager.model.entities.LintingResult;
import snippetmanager.model.entities.LintingRule;
import snippetmanager.redis.linter.LintProducer;
import snippetmanager.repositories.LintingResultRepository;
import snippetmanager.repositories.LintingRuleRepository;

@Service
public class LintingRuleService {
  @Autowired private LintingRuleRepository lintingRuleRepository;

  @Autowired private CodeSnippetService codeSnippetService;

  @Autowired private LintingResultRepository lintingResultRepository;

  private final LintProducer lintProducer;

  @Autowired
  public LintingRuleService(LintProducer lintProducer) {
    this.lintProducer = lintProducer;
  }

  @Transactional
  public String createOrUpdateRules(List<RuleDto> rules, String userId) {
    for (RuleDto ruleDto : rules) {
      Optional<LintingRule> rule = searchRule(ruleDto.getName(), userId);
      if (rule.isPresent()) {
        rule.get().setValue(ruleDto.getValue());
        try {
          lintingRuleRepository.save(rule.get());
        } catch (Exception e) {
          TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
          return "Error updating rules";
        }
      } else {
        createAndSaveRule(userId, ruleDto);
      }
    }
    publishAllSnippetsToRedis(userId);
    return "Success updating rules";
  }

  public void saveLintResult(String assetId, String result) {
    LintingResult lintingResult = lintingResultRepository.findById(assetId)
            .orElse(LintingResult.builder().assetId(assetId).build());
    lintingResult.setResultAsString(result);
    lintingResultRepository.save(lintingResult);
  }

  public String getLintResult(String assetId) {
    LintingResult lintingResult = lintingResultRepository.findById(assetId)
            .orElseThrow(() -> new EntityNotFoundException("No lint result found for assetId: " + assetId));
    return lintingResult.getResultAsString();
  }

  private void publishAllSnippetsToRedis(String userId) {
    codeSnippetService
        .getAllSnippets(userId)
        .forEach(
            snippet -> {
              lintProducer.publishEvent(
                  SnippetSendDto.builder()
                      .assetId(snippet.getAssetId())
                      .language(snippet.getLanguage())
                      .version(snippet.getVersion())
                      .userId(snippet.getUserId())
                      .build());
            });
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
