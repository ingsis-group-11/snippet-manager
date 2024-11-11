package snippetmanager.controllers;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import snippetmanager.model.dtos.RuleDto;
import snippetmanager.services.FormatterRuleService;

@RestController
@RequestMapping("/api/formatter-rule")
public class FormatterRuleController {
  @Autowired private FormatterRuleService formatterRuleService;

  private String getUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Jwt jwt = (Jwt) authentication.getPrincipal();
    String userId = jwt.getClaimAsString("sub");
    int position = userId.indexOf("|");

    if (position != -1) {
      userId = userId.substring(position + 1);
    }

    return userId;
  }

  @PutMapping
  public ResponseEntity<String> createOrUpdateRules(@RequestBody List<RuleDto> rules) {
    return ResponseEntity.ok(formatterRuleService.createOrUpdateRules(rules, getUserId()));
  }

  @GetMapping
  public ResponseEntity<List<RuleDto>> getRules() {
    return ResponseEntity.ok(formatterRuleService.getRules(getUserId()));
  }
}
