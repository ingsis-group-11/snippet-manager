package snippetmanager.repositories;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import snippetmanager.model.entities.TestCase;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, String> {
  List<TestCase> findByAssetId(String assetId);

  void deleteById(@NotNull String testId);
}
