package snippetmanager.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import snippetmanager.model.dtos.RuleDto;
import snippetmanager.model.entities.FormatterRule;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormatterRuleRepository extends JpaRepository<FormatterRule, String> {
  Optional<FormatterRule> findByNameAndUserId(String ruleName, String userId);

  List<RuleDto> findAllByUserId(String userId);
}
