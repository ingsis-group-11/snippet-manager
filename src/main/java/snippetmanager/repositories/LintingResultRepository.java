package snippetmanager.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import snippetmanager.model.entities.LintingResult;

import java.util.Optional;

public interface LintingResultRepository extends JpaRepository<LintingResult, String> {
  @Override
  Optional<LintingResult> findById(String assetId);
}
