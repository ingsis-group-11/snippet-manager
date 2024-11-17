package snippetmanager.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;
import snippetmanager.model.dtos.RuleDto;
import snippetmanager.model.dtos.SnippetSendDto;
import snippetmanager.model.entities.LintingRule;
import snippetmanager.redis.linter.LintProducer;
import snippetmanager.repositories.CodeSnippetRepository;
import snippetmanager.repositories.LintingRuleRepository;
import snippetmanager.webservice.asset.AssetManager;

@ActiveProfiles("test")
@SpringBootTest
public class LinterRulesServiceTest {

  @MockBean private AssetManager assetManager;

  @MockBean private LintingRuleRepository lintingRuleRepository;

  @MockBean private CodeSnippetService codeSnippetService;

  @MockBean private CodeSnippetRepository codeSnippetRepository;

  @MockBean private LintProducer lintProducer;

  @Autowired private LintingRuleService lintingRuleService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = mock(Authentication.class);
    Jwt jwt = mock(Jwt.class);

    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getPrincipal()).thenReturn(jwt);
    when(jwt.getClaimAsString("sub")).thenReturn("1");

    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void createNewLintingRulesSuccess() {
    String ruleId = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();

    when(lintingRuleRepository.save(any(LintingRule.class)))
        .thenAnswer(
            invocation -> {
              LintingRule rule = invocation.getArgument(0);
              rule.setId(ruleId);
              return rule;
            });
    when(assetManager.createAsset(anyString(), anyString(), any(MultipartFile.class)))
        .thenReturn(ResponseEntity.ok("Success"));
    when(codeSnippetService.getAllOwnSnippets(eq(userId)))
        .thenReturn(List.of(createSnippetSendDto(), createSnippetSendDto()));
    when(lintingRuleRepository.findByNameAndUserId(anyString(), eq(userId)))
        .thenReturn(Optional.empty());

    List<RuleDto> rules =
        List.of(createRuleDto("rule1", "value1"), createRuleDto("rule2", "value2"));
    ResponseEntity<String> response =
        ResponseEntity.ok(lintingRuleService.createOrUpdateRules(rules, userId));

    assertTrue(response.getStatusCode().is2xxSuccessful());
  }

  @Test
  void createNewLintingRulesFailure() {
    String ruleId = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();

    when(lintingRuleRepository.save(any(LintingRule.class)))
        .thenAnswer(
            invocation -> {
              LintingRule rule = invocation.getArgument(0);
              rule.setId(ruleId);
              return rule;
            });
    when(assetManager.createAsset(anyString(), anyString(), any(MultipartFile.class)))
        .thenReturn(ResponseEntity.ok("Success"));
    when(codeSnippetService.getAllOwnSnippets(eq(userId)))
        .thenReturn(List.of(createSnippetSendDto(), createSnippetSendDto()));
    when(lintingRuleRepository.findByNameAndUserId(anyString(), eq(userId)))
        .thenReturn(Optional.empty());

    List<RuleDto> rules =
        List.of(createRuleDto("rule1", "value1"), createRuleDto("rule1", "value1"));

    assertThrows(
        RuntimeException.class, () -> lintingRuleService.createOrUpdateRules(rules, userId));
  }

  @Test
  void updateLintingRulesSuccess() {
    String ruleId = UUID.randomUUID().toString();
    String userId = UUID.randomUUID().toString();

    when(lintingRuleRepository.save(any(LintingRule.class)))
        .thenAnswer(
            invocation -> {
              LintingRule rule = invocation.getArgument(0);
              rule.setId(ruleId);
              return rule;
            });
    when(assetManager.createAsset(anyString(), anyString(), any(MultipartFile.class)))
        .thenReturn(ResponseEntity.ok("Success"));
    when(codeSnippetService.getAllOwnSnippets(eq(userId)))
        .thenReturn(List.of(createSnippetSendDto(), createSnippetSendDto()));
    when(lintingRuleRepository.findByNameAndUserId(anyString(), eq(userId)))
        .thenReturn(Optional.of(new LintingRule()));

    List<RuleDto> rules =
        List.of(createRuleDto("rule1", "value1"), createRuleDto("rule2", "value2"));
    ResponseEntity<String> response =
        ResponseEntity.ok(lintingRuleService.createOrUpdateRules(rules, userId));

    assertTrue(response.getStatusCode().is2xxSuccessful());
  }

  @Test
  void getAllRules() {
    String userId = UUID.randomUUID().toString();
    List<LintingRule> rules =
        List.of(
            createLintingRule("rule1", "value1", userId),
            createLintingRule("rule2", "value2", userId));

    when(lintingRuleRepository.findAllByUserId(userId)).thenReturn(rules);

    List<RuleDto> response = lintingRuleService.getRules(userId);

    assertEquals(2, response.size());
  }

  private RuleDto createRuleDto(String ruleName, String ruleValue) {
    return RuleDto.builder().name(ruleName).value(ruleValue).isActive(true).build();
  }

  private SnippetSendDto createSnippetSendDto() {
    return SnippetSendDto.builder()
        .assetId(UUID.randomUUID().toString())
        .language("printscript")
        .version("1.1")
        .content("example content")
        .build();
  }

  private LintingRule createLintingRule(String ruleName, String ruleValue, String userId) {
    LintingRule lintingRule = new LintingRule();
    lintingRule.setName(ruleName);
    lintingRule.setValue(ruleValue);
    lintingRule.setIsActive(true);
    lintingRule.setId(UUID.randomUUID().toString());
    lintingRule.setUserId(userId);
    return lintingRule;
  }
}