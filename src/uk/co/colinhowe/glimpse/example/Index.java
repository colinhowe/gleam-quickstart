package uk.co.colinhowe.glimpse.example;

import java.text.SimpleDateFormat;
import java.util.Date;

import uk.co.colinhowe.glimpse.quickstart.RequestHandler;

public class Index extends RequestHandler {
  
  /**
   * Gets the current time
   * @return
   */
  public String getTime() {
    return new SimpleDateFormat("EEEE HH:mm:ss").format(new Date());
  }
}
