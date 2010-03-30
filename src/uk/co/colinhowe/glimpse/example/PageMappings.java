package uk.co.colinhowe.glimpse.example;

import java.util.Map;
import java.util.HashMap;

import uk.co.colinhowe.glimpse.quickstart.RequestHandler;

public class PageMappings {
  public final static Map<String, Class<? extends RequestHandler>> MAPPINGS = new HashMap<String, Class<? extends RequestHandler>>();
  
  static {
    MAPPINGS.put("index", Index.class);
  }
}
