package snippet_manager.snippet.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import snippet_manager.snippet.model.entities.CodeSnippet;
import snippet_manager.snippet.util.CodeLanguage;

import java.util.Optional;

@DataJpaTest
public class CodeSnippetRepositoryTest {
  @Autowired
  private CodeSnippetRepository codeSnippetRepository;

  @Test
  void createAndSaveNewSnippet() {
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setTitle("Test Snippet");
    codeSnippet.setContent("This is a test snippet");
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);
    codeSnippet.setVersion("1.1");
    codeSnippetRepository.save(codeSnippet);

    Optional<CodeSnippet> findSnippet = codeSnippetRepository.findById(codeSnippet.getId());
    assert findSnippet.isPresent();
    assert findSnippet.get().getTitle().equals("Test Snippet");
    assert findSnippet.get().getContent().equals("This is a test snippet");
    assert findSnippet.get().getLanguage().equals(CodeLanguage.PRINTSCRIPT);
    assert findSnippet.get().getVersion().equals("1.1");
  }

  @Test
  void updateExistingSnippet() {
    //Create new snippet
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setTitle("Test Snippet");
    codeSnippet.setContent("This is a test snippet");
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);
    codeSnippet.setVersion("1.1");
    codeSnippetRepository.save(codeSnippet);

    //Search for the snippet
    Optional<CodeSnippet> findSnippet = codeSnippetRepository.findById(codeSnippet.getId());
    assert findSnippet.isPresent();

    //Update the snippet
    findSnippet.get().setTitle("Updated Snippet");
    findSnippet.get().setContent("This is an updated snippet");

    codeSnippetRepository.save(findSnippet.get());

    //Search for the updated snippet
    Optional<CodeSnippet> updatedSnippet = codeSnippetRepository.findById(codeSnippet.getId());
    assert updatedSnippet.isPresent();
    assert updatedSnippet.get().getTitle().equals("Updated Snippet");
    assert updatedSnippet.get().getContent().equals("This is an updated snippet");
    assert updatedSnippet.get().getLanguage().equals(CodeLanguage.PRINTSCRIPT);
    assert updatedSnippet.get().getVersion().equals("1.1");
  }

  @Test
  void deleteExistingSnippet() {
    //Create new snippet
    CodeSnippet codeSnippet = new CodeSnippet();
    codeSnippet.setTitle("Test Snippet");
    codeSnippet.setContent("This is a test snippet");
    codeSnippet.setLanguage(CodeLanguage.PRINTSCRIPT);
    codeSnippet.setVersion("1.1");
    codeSnippetRepository.save(codeSnippet);

    //Search for the snippet
    Optional<CodeSnippet> findSnippet = codeSnippetRepository.findById(codeSnippet.getId());
    assert findSnippet.isPresent();

    //Delete the snippet
    codeSnippetRepository.delete(findSnippet.get());

    //Search for the deleted snippet
    Optional<CodeSnippet> deletedSnippet = codeSnippetRepository.findById(codeSnippet.getId());
    assert deletedSnippet.isEmpty();
  }
}
