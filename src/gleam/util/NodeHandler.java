package gleam.util;

import gleam.Node;

public interface NodeHandler {
  public void handle(Node node, StringBuilder builder);
}