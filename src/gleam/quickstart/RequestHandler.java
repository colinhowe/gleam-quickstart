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
