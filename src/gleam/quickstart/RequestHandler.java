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

import java.lang.reflect.Method;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

public abstract class RequestHandler {
  @SuppressWarnings("unchecked")
  public void doGet(HttpServletRequest request) {
    // Process all the properties and set any that are valid
    for (Object entry : request.getParameterMap().entrySet()) {
      Entry<String, String[]> property = (Entry<String, String[]>)entry;
      String propertyName = property.getKey();
      String setterName = 
        "set" + 
        propertyName.substring(0, 1).toUpperCase() +
        propertyName.substring(1);
      
      try {
        Method[] methods = this.getClass().getDeclaredMethods();
        for (Method method : methods) {
          if (method.getName().equals(setterName)) {
            method.invoke(this, property.getValue()[0]);
            break;
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  public void doPost(HttpServletRequest request) {
  }
}
