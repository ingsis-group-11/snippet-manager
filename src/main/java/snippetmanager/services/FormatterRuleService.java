package snippetmanager.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.multipart.MultipartFile;
import snippetmanager.model.dtos.RuleDto;
import snippetmanager.model.entities.FormatterRule;
import snippetmanager.redis.formatter.FormatterProducer;
import snippetmanager.repositories.FormatterRuleRepository;
import snippetmanager.util.DefaultRulesFactory;
import snippetmanager.webservice.asset.AssetManager;

@Service
public class FormatterRuleService {
  @Autowired private AssetManager assetManager;

  @Autowired private FormatterRuleRepository formatterRuleRepository;

  @Autowired private CodeSnippetService codeSnippetService;

  private final FormatterProducer formatterProducer;

  @Autowired
  public FormatterRuleService(FormatterProducer formatterProducer) {
    this.formatterProducer = formatterProducer;
  }

  public String createOrUpdateRules(List<RuleDto> rules, String userId) {
    for (RuleDto ruleDto : rules) {
      Optional<FormatterRule> rule = searchRule(ruleDto.getName(), userId);
      if (rule.isPresent()) {
        rule.get().setValue(ruleDto.getValue());
        rule.get().setIsActive(ruleDto.getIsActive());
        try {
          formatterRuleRepository.save(rule.get());
        } catch (Exception e) {
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

  public List<RuleDto> getRules(String userId) {
    createDefaultRulesIfNeeded(userId);
    List<FormatterRule> rules = formatterRuleRepository.findAllByUserId(userId);
    return rules.stream()
        .map(
            rule ->
                RuleDto.builder()
                    .name(rule.getName())
                    .value(rule.getValue())
                    .isActive(rule.getIsActive())
                    .id(rule.getId())
                    .build())
        .collect(Collectors.toList());
  }

  // ** Internal methods

  private void createDefaultRulesIfNeeded(String userId) {
    List<FormatterRule> formatterRules = formatterRuleRepository.findAllByUserId(userId);

    if (formatterRules.isEmpty()) {
      this.createOrUpdateRules(DefaultRulesFactory.getDefaultFormatterRules(), userId);
    }
  }

  private void publishAllSnippetsToRedis(String userId) {
    codeSnippetService.getAllOwnSnippets(userId).forEach(formatterProducer::publishEvent);
  }

  private void createAndSaveRule(String userId, RuleDto ruleDto) {
    FormatterRule newRule = new FormatterRule();
    newRule.setName(ruleDto.getName());
    newRule.setValue(ruleDto.getValue());
    newRule.setUserId(userId);
    newRule.setIsActive(ruleDto.getIsActive());
    try {
      formatterRuleRepository.save(newRule);
    } catch (Exception e) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      throw new HttpServerErrorException(HttpStatusCode.valueOf(500), "Error creating rule");
    }
  }

  private Optional<FormatterRule> searchRule(String ruleName, String userId) {
    return formatterRuleRepository.findByNameAndUserId(ruleName, userId);
  }

  private void createOrUpdateRulesInAssetService(List<RuleDto> rules, String userId) {
    try {
      MultipartFile rulesToJson = getRulesInMultipartFile(rules, userId);

      ResponseEntity<String> createAssetResponse =
          assetManager.createAsset("format-rules", userId, rulesToJson);
      if (createAssetResponse.getStatusCode().isError()) {
        throw new HttpServerErrorException(
            createAssetResponse.getStatusCode(), "Error creating asset with formatter rules");
      }
    } catch (Exception e) {
      throw new RuntimeException("Error creating asset with formatter rules", e);
    }
  }

  // TODO: Move this method to a utility class
  @NotNull
  private static MultipartFile getRulesInMultipartFile(List<RuleDto> rules, String userId)
      throws JsonProcessingException {
    Map<String, String> rulesMap =
        rules.stream()
            .filter(RuleDto::getIsActive)
            .collect(Collectors.toMap(RuleDto::getName, rule -> String.valueOf(rule.getValue())));

    ObjectMapper objectMapper = new ObjectMapper();
    String jsonString = objectMapper.writeValueAsString(rulesMap);

    MultipartFile rulesToJson =
        new MockMultipartFile(
            "format-rules-" + userId, "rules.json", "application/json", jsonString.getBytes());
    return rulesToJson;
  }
}
