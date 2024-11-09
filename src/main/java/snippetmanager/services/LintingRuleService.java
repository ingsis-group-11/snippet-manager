package snippetmanager.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import snippetmanager.model.dtos.RuleDto;
import snippetmanager.model.dtos.SnippetSendDto;
import snippetmanager.model.entities.CodeSnippet;
import snippetmanager.model.entities.LintingRule;
import snippetmanager.redis.linter.LintProducer;
import snippetmanager.repositories.CodeSnippetRepository;
import snippetmanager.repositories.LintingRuleRepository;
import snippetmanager.webservice.asset.AssetManager;

@Service
public class LintingRuleService {
  @Autowired private AssetManager assetManager;

  @Autowired private LintingRuleRepository lintingRuleRepository;

  @Autowired private CodeSnippetService codeSnippetService;

  @Autowired private CodeSnippetRepository codeSnippetRepository;

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
          throw new HttpServerErrorException(HttpStatusCode.valueOf(500), "Error updating rules");
        }
      } else {
        createAndSaveRule(userId, ruleDto);
      }
    }
    createOrUpdateRulesInAssetService(rules, userId);
    publishAllSnippetsToRedis(userId);
    return "Success updating rules";
  }

  @Transactional
  public void saveLintResult(String assetId, String result) {
    String resultEnum = result.toUpperCase();
    Optional<CodeSnippet> codeSnippetOp = codeSnippetRepository.findById(assetId);
    if (codeSnippetOp.isPresent()) {
      CodeSnippet codeSnippet = codeSnippetOp.get();
      codeSnippet.setResultAsString(resultEnum);
      try {
        codeSnippetRepository.save(codeSnippet);
      } catch (Exception e) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        throw new HttpServerErrorException(HttpStatusCode.valueOf(500), "Error updating rules");
      }

    } else {
      CodeSnippet newCodeSnippet = new CodeSnippet();
      newCodeSnippet.setAssetId(assetId);
      newCodeSnippet.setResultAsString(resultEnum);
      try {
        codeSnippetRepository.save(newCodeSnippet);
      } catch (Exception e) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        throw new HttpServerErrorException(HttpStatusCode.valueOf(500), "Error updating rules");
      }
    }
  }

  public List<RuleDto> getRules(String userId) {
    List<LintingRule> rules = lintingRuleRepository.findAllByUserId(userId);
    return rules.stream()
        .map(
            rule ->
                RuleDto.builder()
                    .name(rule.getName())
                    .value(rule.getValue())
                    .id(rule.getId())
                    .build())
        .collect(Collectors.toList());
  }

  private void publishAllSnippetsToRedis(String userId) {
    codeSnippetService
        .getAllWriteSnippets(userId)
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
    try {
      lintingRuleRepository.save(newRule);
    } catch (Exception e) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      throw new HttpServerErrorException(HttpStatusCode.valueOf(500), "Error creating rule");
    }
  }

  private Optional<LintingRule> searchRule(String ruleName, String userId) {
    return lintingRuleRepository.findByNameAndUserId(ruleName, userId);
  }

  private void createOrUpdateRulesInAssetService(List<RuleDto> rules, String userId) {
    try {
      Map<String, String> rulesMap =
          rules.stream()
              .collect(Collectors.toMap(RuleDto::getName, rule -> String.valueOf(rule.getValue())));

      ObjectMapper objectMapper = new ObjectMapper();
      String jsonString = objectMapper.writeValueAsString(rulesMap);

      MultipartFile rulesToJson =
          new MockMultipartFile(
              "linting-rules-" + userId, "rules.json", "application/json", jsonString.getBytes());

      ResponseEntity<String> createAssetResponse =
          assetManager.createAsset("lint-rules", userId, rulesToJson);
      if (createAssetResponse.getStatusCode().isError()) {
        throw new HttpServerErrorException(
            createAssetResponse.getStatusCode(), "Error creating asset with linting rules");
      }
    } catch (Exception e) {
      throw new RuntimeException("Error creating asset with linting rules", e);
    }
  }
}
