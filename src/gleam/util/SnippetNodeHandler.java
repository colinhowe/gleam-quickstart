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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import gleam.Node;

public class SnippetNodeHandler implements NodeHandler {
  private final SnippetProcessor snippetProcessor = new SnippetProcessor();
  private final HtmlCreator htmlCreator;
  private final String snippetName;
  
  public SnippetNodeHandler(HtmlCreator htmlCreator, String snippetName) {
    this.htmlCreator = htmlCreator;
    this.snippetName = snippetName;
  }
  
  public void handle(Node node, StringBuilder builder) {
    final Map<String, String> attributes = new HashMap<String, String>();
    
    // Copy all the attributes from the current node onto the attributes map
    for (Entry<String, Object> entry : node.getAttributes().entrySet()) {
      attributes.put(entry.getKey(), entry.getValue().toString());
    }
      
    // Set the inner value for this snippet as either the inner nodes or the
    // value of the current node
    if (node.getNodes() != null && node.getNodes().size() != 0) {
      final StringBuilder innerBuilder = new StringBuilder();
      for (final Node innerNode : node.getNodes()) {
        htmlCreator.generate(innerNode, innerBuilder);
      }
      attributes.put("_inner", innerBuilder.toString());
    } else if (node.getValue() != null) {
      attributes.put("_inner", node.getValue().toString());
    }
    
    // Process the snippet into HTML and put on the string builder
    builder.append(snippetProcessor.process(
      SnippetProvider.loadSnippet(snippetName).getContents(), attributes));
  }
}