package snippetmanager.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import snippetmanager.model.entities.FormatterRule;

@Repository
public interface FormatterRuleRepository extends JpaRepository<FormatterRule, String> {
  Optional<FormatterRule> findByNameAndUserId(String ruleName, String userId);

  List<FormatterRule> findAllByUserId(String userId);
}
