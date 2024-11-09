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
import snippetmanager.model.entities.FormatterRule;
import snippetmanager.redis.formatter.FormatterProducer;
import snippetmanager.repositories.FormatterRuleRepository;
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

  @Transactional
  public String createOrUpdateRules(List<RuleDto> rules, String userId) {
    for (RuleDto ruleDto : rules) {
      Optional<FormatterRule> rule = searchRule(ruleDto.getName(), userId);
      if (rule.isPresent()) {
        rule.get().setValue(ruleDto.getValue());
        try {
          formatterRuleRepository.save(rule.get());
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

  public List<RuleDto> getRules(String userId) {
    List<FormatterRule> rules = formatterRuleRepository.findAllByUserId(userId);
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
              formatterProducer.publishEvent(
                  SnippetSendDto.builder()
                      .assetId(snippet.getAssetId())
                      .language(snippet.getLanguage())
                      .version(snippet.getVersion())
                      .userId(snippet.getUserId())
                      .build());
            });
  }

  private void createAndSaveRule(String userId, RuleDto ruleDto) {
    FormatterRule newRule = new FormatterRule();
    newRule.setName(ruleDto.getName());
    newRule.setValue(ruleDto.getValue());
    newRule.setUserId(userId);
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
      Map<String, String> rulesMap =
              rules.stream()
                      .collect(Collectors.toMap(RuleDto::getName, rule -> String.valueOf(rule.getValue())));

      ObjectMapper objectMapper = new ObjectMapper();
      String jsonString = objectMapper.writeValueAsString(rulesMap);

      MultipartFile rulesToJson =
              new MockMultipartFile(
                      "format-rules-" + userId, "rules.json", "application/json", jsonString.getBytes());

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
}
