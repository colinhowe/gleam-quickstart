package gleam.util;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gleam.Node;
import gleam.PropertyReference;

public class HtmlCreator {
  private Map<String, NodeHandler> handlers = new HashMap<String, NodeHandler>();
  
  public HtmlCreator() {
    
    handlers.put("page", new SnippetNodeHandler(this, "page"));
    handlers.put("text", new SnippetNodeHandler(this, "text"));
    handlers.put("form", new SnippetNodeHandler(this, "form"));
    handlers.put("paragraph", new SnippetNodeHandler(this, "paragraph"));
    handlers.put("field", new SnippetNodeHandler(this, "field") {
      @Override
      public void handle(Node node, StringBuilder builder) {
        PropertyReference<?> reference = (PropertyReference<?>)node.getAttribute("property");
        node.setAttribute("value", reference.getValue());
        node.setAttribute("name", reference.getPath());
        super.handle(node, builder);
      }
    });
  }
  
  public void generate(Node node, StringBuilder builder) {
    if (handlers.containsKey(node.getTagName())) {
      handlers.get(node.getTagName()).handle(node, builder);
    } else {
      throw new IllegalStateException("Unrecognised node type [" + node.getTagName() + "]");
    }
  }

  public String generate(List<Node> nodes) {
    StringBuilder builder = new StringBuilder();
    
    for (Node node : nodes) {
      generate(node, builder);
    }
    
    return builder.toString();
  }
}