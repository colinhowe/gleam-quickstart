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
package gleam.quickstart;

import gleam.View;

import java.util.HashMap;
import java.util.Map;

public class ViewCache {
  private final Map<String, View> viewCache = new HashMap<String, View>();
  private final boolean developerMode;
  
  public ViewCache(boolean developerMode) {
    this.developerMode = developerMode;
  }
  
  public View getView(String viewName) {
    if (!developerMode && viewCache.containsKey(viewName)) {
      return viewCache.get(viewName);
    }
    
    try {
      final ClassReloader classLoader = new ClassReloader("temp", this.getClass().getClassLoader());
      final Class<?> viewClazz = classLoader.loadClass(viewName);
      View view = (View)viewClazz.newInstance();
      viewCache.put(viewName, view);
      return view;
    } catch (Exception e) {
      throw new RuntimeException("Failed to instantiate view", e);
    }
  }
}
