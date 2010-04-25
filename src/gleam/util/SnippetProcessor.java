/*
 * Copyright 2010 Colin Howe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gleam.util;

import java.util.Map;

public class SnippetProcessor {
  public String process(final String snippet, final Map<String, String> substitutions) {
    
    // Replace all the % tokens with the appropriate value from the substitutions map
    // Any double % signs (%%) will be replaced with a % sign in the created HTML
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