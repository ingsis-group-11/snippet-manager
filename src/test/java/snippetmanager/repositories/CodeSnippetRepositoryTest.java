package snippetmanager.repositories;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import snippetmanager.model.entities.CodeSnippet;
import snippetmanager.util.CodeLanguage;

@DataJpaTest
public class CodeSnippetRepositoryTest {

  @Autowired private CodeSnippetRepository codeSnippetRepository;

  @Test
  void createAndSaveNewSnippet() {
    String assetId = "snippet-test";
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(assetId);
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);
    codeSnippet.setVersion("1.1");
    codeSnippetRepository.save(codeSnippet);

    Optional<CodeSnippet> findSnippet = codeSnippetRepository.findById(codeSnippet.getId());
    assert findSnippet.isPresent();
    assert findSnippet.get().getAssetId().equals(assetId);
    assert findSnippet.get().getLanguage().equals(CodeLanguage.PRINTSCRIPT);
    assert findSnippet.get().getVersion().equals("1.1");
  }

  @Test
  void updateExistingSnippet() {
    String assetId = "snippet-test";
    // Create new snippet
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(assetId);
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);
    codeSnippet.setVersion("1.1");
    codeSnippetRepository.save(codeSnippet);

    // Search for the snippet
    Optional<CodeSnippet> findSnippet = codeSnippetRepository.findById(codeSnippet.getId());
    assert findSnippet.isPresent();

    // Update the snippet
    findSnippet.get().setVersion("1.2");

    codeSnippetRepository.save(findSnippet.get());

    // Search for the updated snippet
    Optional<CodeSnippet> updatedSnippet = codeSnippetRepository.findById(codeSnippet.getId());
    assert updatedSnippet.isPresent();
    assert updatedSnippet.get().getAssetId().equals(assetId);
    assert updatedSnippet.get().getLanguage().equals(CodeLanguage.PRINTSCRIPT);
    assert updatedSnippet.get().getVersion().equals("1.2");
  }

  @Test
  void deleteExistingSnippet() {
    String assetId = "snippet-test";
    // Create new snippet
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setAssetId(assetId);
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);
    codeSnippet.setVersion("1.1");
    codeSnippetRepository.save(codeSnippet);

    // Search for the snippet
    Optional<CodeSnippet> findSnippet = codeSnippetRepository.findById(codeSnippet.getId());
    assert findSnippet.isPresent();

    // Delete the snippet
    codeSnippetRepository.delete(findSnippet.get());

    // Search for the deleted snippet
    Optional<CodeSnippet> deletedSnippet = codeSnippetRepository.findById(codeSnippet.getId());
    assert deletedSnippet.isEmpty();
  }
}
