package snippetmanager.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import snippetmanager.model.dtos.RuleDto;
import snippetmanager.services.FormatterRuleService;
import snippetmanager.services.LintingRuleService;

import java.util.List;

@RestController
@RequestMapping("/api/formatter-rule")
public class FormatterRuleController {
  @Autowired private FormatterRuleService formatterRuleService;

  private String getUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Jwt jwt = (Jwt) authentication.getPrincipal();

    return jwt.getClaimAsString("sub");
  }

  @PutMapping
  public ResponseEntity<String> createOrUpdateRules(
          @RequestBody List<RuleDto> rules) {
    return ResponseEntity.ok(formatterRuleService.createOrUpdateRules(rules, getUserId()));
  }

  @GetMapping
  public ResponseEntity<List<RuleDto>> getRules() {
    return ResponseEntity.ok(formatterRuleService.getRules(getUserId()));
  }
}
