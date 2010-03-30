package uk.co.colinhowe.glimpse.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.co.colinhowe.glimpse.Node;

public class HtmlCreator {
  private interface NodeHandler {
    public void handle(Node node, StringBuilder builder);
  }
  
  private Map<String, NodeHandler> handlers = new HashMap<String, NodeHandler>();
  
  public Node findNode(List<Node> nodes, String name) {
    for (Node node : nodes) {
      if (node.getId().equals(name)) {
        return node;
      }
    }
    
    return null;
  }
  
  public HtmlCreator() {
    
    handlers.put("page", new NodeHandler() {
      public void handle(Node node, StringBuilder builder) {
        builder.append("<html>");
        builder.append("<head>");
        builder.append("<title>" + node.getAttribute("title") + "</title>");
        builder.append("</head>");
        builder.append("<body>");
        builder.append("<h1>" + node.getAttribute("title") + "</h1>");
        generate(node.getNodes(), builder);
        builder.append("</body>");
        builder.append("</html>");
      }
    });
    
    handlers.put("text", new NodeHandler() {
      public void handle(Node node, StringBuilder builder) {
        builder.append("<span>" + node.getValue() + "</span>");
      }
    });
  }
  
  public void generate(Node node, StringBuilder builder) {
    if (handlers.containsKey(node.getId())) {
      handlers.get(node.getId()).handle(node, builder);
    } else {
      throw new IllegalStateException("Unrecognised node type [" + node.getId() + "]");
    }
  }

  public void generate(List<Node> nodes, StringBuilder builder) {
    for (Node node : nodes) {
      generate(node, builder);
    }
  }

  public String generate(List<Node> nodes) {
    StringBuilder builder = new StringBuilder();
    
    generate(nodes, builder);
    
    return builder.toString();
  }
}
