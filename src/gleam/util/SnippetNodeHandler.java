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
    
    for (Entry<String, Object> entry : node.getAttributes().entrySet()) {
      attributes.put(entry.getKey(), entry.getValue().toString());
    }
      
    if (node.getNodes() != null && node.getNodes().size() != 0) {
      final StringBuilder innerBuilder = new StringBuilder();
      for (final Node innerNode : node.getNodes()) {
        htmlCreator.generate(innerNode, innerBuilder);
      }
      attributes.put("_inner", innerBuilder.toString());
    } else if (node.getValue() != null) {
      attributes.put("_inner", node.getValue().toString());
    }
    
    builder.append(snippetProcessor.process(
      SnippetProvider.loadSnippet(snippetName).getContents(), attributes));
  }
}