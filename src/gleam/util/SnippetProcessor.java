package gleam.util;

import java.util.Map;

public class SnippetProcessor {
  public String process(final String snippet, final Map<String, String> substitutions) {
    final StringBuilder buffer = new StringBuilder();
    int currentIndex = 0;
    while (currentIndex < snippet.length()) {
      int firstSepIndex = snippet.indexOf("%", currentIndex);
      if (firstSepIndex == -1) {
        buffer.append(snippet.substring(currentIndex));
        currentIndex = snippet.length();
      } else {
        buffer.append(snippet.substring(currentIndex, firstSepIndex));
        currentIndex = snippet.indexOf("%", firstSepIndex + 1);
        if (firstSepIndex + 1 == currentIndex) {
          buffer.append("%");
        } else {
          final String subName = snippet.substring(firstSepIndex + 1, currentIndex);
          buffer.append(substitutions.get(subName));
        }
        currentIndex = currentIndex + 1;
      }
    }
    
    return buffer.toString();
  }
}