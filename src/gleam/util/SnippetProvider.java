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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SnippetProvider {
  public static class Snippet {
    private long lastModified;
    private String contents;
    
    public String getContents() {
      return contents;
    }
  }
  
  static final int UPDATE_INTERVAL = 5000;
  static Thread cacheUpdater;
  
  static Map<String, Snippet> snippets = new HashMap<String, Snippet>();
  
  static {
    // Initialise the thread for caching
    cacheUpdater = new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            Thread.sleep(UPDATE_INTERVAL);
            
            for (String snippetName : snippets.keySet()) {
              try {
                // Check the modified date
                long lastModified = new File("snippets/" + snippetName + ".html").lastModified();
                boolean refresh = false;
                synchronized (snippets) {
                  if (lastModified != snippets.get(snippetName).lastModified) {
                    refresh = true;
                  }
                }
                
                if (refresh) {
                  loadSnippet(snippetName, true);
                }
                
              } catch (Exception e) {
                synchronized (snippets) {
                  snippets.remove(snippetName);
                }
              }
            }
            
          } catch (Exception e) {
            // Ignore
          }
        }
      }
    };
    cacheUpdater.setDaemon(true);
    
    cacheUpdater.start();
  }
  
  public static Snippet loadSnippet(String name) {
    return loadSnippet(name, false);
  }
  
  static Snippet loadSnippet(String name, boolean refresh) {
  
    if (!refresh) {
      // Check the cache
      synchronized (snippets) {
        if (snippets.containsKey(name)) {
          return snippets.get(name);
        }
      }
    }
    
    FileInputStream fis = null;
    try {
      fis = new FileInputStream("snippets/" + name + ".html");
      int numberBytes = fis.available();
      byte bytearray[] = new byte[numberBytes];
  
      fis.read(bytearray);
      Snippet snippet = new Snippet();
      snippet.contents = new String(bytearray);
      snippet.lastModified = new File("snippets/" + name + ".html").lastModified();

      // Put the snippet in the cache
      synchronized (snippets) {
        snippets.put(name, snippet);
      }

      return snippet;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}