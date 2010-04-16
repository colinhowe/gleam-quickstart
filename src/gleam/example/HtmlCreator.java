package gleam.example;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gleam.Node;
import gleam.PropertyReference;

public class HtmlCreator {
  private interface NodeHandler {
    public void handle(Node node, StringBuilder builder);
  }
  
  private Map<String, NodeHandler> handlers = new HashMap<String, NodeHandler>();
    
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
    
    handlers.put("paragraph", new NodeHandler() {
      public void handle(Node node, StringBuilder builder) {
        builder.append("<p>");
        generate(node.getNodes(), builder);
        builder.append("</p>");
      }
    });
    
    handlers.put("form", new NodeHandler() {
      public void handle(Node node, StringBuilder builder) {
        builder.append("<form>");
        generate(node.getNodes(), builder);
        builder.append("<input type=\"submit\" value=\"Go\" />");
        builder.append("</form>");
      }
    });
    
    handlers.put("field", new NodeHandler() {
      public void handle(Node node, StringBuilder builder) {
        PropertyReference<?> ref = (PropertyReference<?>)node.getAttribute("property");
        String inputName = ref.getPath();
        String value = (String)ref.getValue();
        String label = (String)node.getAttribute("label");
        builder.append("<div>");
        builder.append("<label for=\"" + inputName + "\">" + label + "</label>");
        builder.append("<input type=\"text\" name=\"" + inputName + "\" value=\"");
        if (value != null) {
          builder.append(value);
        }
        builder.append("\" />");
        builder.append("</div>");
      }
    });
  }
  
  private void generate(Node node, StringBuilder builder) {
    if (handlers.containsKey(node.getTagName())) {
      handlers.get(node.getTagName()).handle(node, builder);
    } else {
      throw new IllegalStateException("Unrecognised node type [" + node.getTagName() + "]");
    }
  }

  private void generate(List<Node> nodes, StringBuilder builder) {
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
