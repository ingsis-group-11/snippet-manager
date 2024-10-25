package snippetmanager.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import snippetmanager.model.entities.CodeSnippet;

@Repository
public interface CodeSnippetRepository extends JpaRepository<CodeSnippet, String> {
  Optional<CodeSnippet> findCodeSnippetByAssetId(String assetId);

  Optional<CodeSnippet> deleteCodeSnippetByAssetId(String assetId);
}
