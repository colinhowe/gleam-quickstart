package pages;

import java.util.Map;
import java.util.HashMap;

import gleam.quickstart.RequestHandler;

public class PageMappings {
  public final static Map<String, Class<? extends RequestHandler>> MAPPINGS = new HashMap<String, Class<? extends RequestHandler>>();
  
  static {
    MAPPINGS.put("index", Index.class);
  }
}
