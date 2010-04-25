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

import gleam.CompilationError;
import gleam.CompilationResult;
import gleam.Node;
import gleam.View;
import gleam.compiler.CompilationUnit;
import gleam.compiler.FileCompilationUnit;
import gleam.compiler.GleamCompiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import pages.PageMappings;

public class RequestProcessor extends AbstractHandler {
  private static boolean developerMode = false;
  
  /**
   * Compiles the views.
   * 
   * @return A string containing the errors ready to be output.
   */
  private String compile() {
    String result = "";
    final File viewsFolder = new File("./views/");
    final List<CompilationUnit> units = new LinkedList<CompilationUnit>();
    for (final File viewFile : viewsFolder.listFiles()) {
      if (viewFile.getAbsolutePath().endsWith(".gleam")) {
        final String viewName = viewFile.getName().substring(0, viewFile.getName().indexOf(".gleam"));
        final String sourceName = viewFile.toString().substring(0, viewsFolder.toString().length());
        final CompilationUnit unit = 
          new FileCompilationUnit(viewName, sourceName, viewFile.getAbsolutePath());
        units.add(unit);
      }
    }
  
    // Compile all the units
    List<String> classPaths = new LinkedList<String>();
    classPaths.add("../gleam/bin");
    
    List<CompilationResult> compilationResults = new GleamCompiler().compile(units, classPaths);
    
    boolean hasErrors = false;
    for (CompilationResult compilationResult : compilationResults) {
      for (CompilationError error : compilationResult.getErrors()) {
        hasErrors = true;
        System.out.println(error.toString());
        result += compilationResult.getFilename() + ": " + error.toString() + "\n";
      }
    }
    
    if (hasErrors) {
      result = "<pre>" + result + "</pre>";
    }
    
    return result;
  }
  
  public static Properties loadConfiguration() {
    Properties propertiesFile = new Properties();
    try {
      propertiesFile.load(new FileInputStream("config.properties"));
    } catch (Exception e) {
      throw new RuntimeException("Failed to load config.properties", e);
    }
    return propertiesFile;
  }
  
  public static void main(String[] args) throws Exception {
    Properties config = loadConfiguration();
    developerMode = Boolean.parseBoolean(config.getProperty("developermode"));
    
    Server server = new Server(Integer.parseInt(config.getProperty("port")));
    server.setHandler(new RequestProcessor());
    server.start();
  }
  
  RequestProcessor() {
    compile();
  }
  
  private byte[] readFile(String filename) throws FileNotFoundException {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(filename);
      int numberBytes = fis.available();
      byte bytearray[] = new byte[numberBytes];
  
      fis.read(bytearray);
      return bytearray;
    } catch (IOException e) {
      if (e instanceof FileNotFoundException) {
        throw (FileNotFoundException)e;
      } else {
        throw new RuntimeException(e);
      }
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
  
  final Map<String, View> viewCache = new HashMap<String, View>();
  private View getView(String viewName) {
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
  
  public class ClassReloader extends ClassLoader {
    final String viewFolder;
    
    public ClassReloader(String viewFolder, ClassLoader parent) {
      super(parent);
      this.viewFolder = viewFolder;
    }
    
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      try {
        byte[] bytes = readFile(viewFolder + "/" + name + ".class");
        return defineClass(name, bytes, 0, bytes.length);
      } catch (IOException e) {
        return super.loadClass(name);
      }
    }
  }
//  public void handle(String arg0, Request arg1, HttpServletRequest arg2,
//      HttpServletResponse arg3) throws IOException, ServletException {
  public void handle(String target, Request r1, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    long startTime = System.currentTimeMillis();
    
    response.addHeader("Expires", "Fri, 30 Oct 1998 14:19:41 GMT");
    response.addHeader("Content-Type", "text/html");
    
    // Locate all the gleam files
    String requestUri = request.getRequestURI().substring(1);
    if (requestUri.indexOf(".") != -1) {
      // Get from the resources folder
      try {
        final byte[] contents = readFile("resource/" + requestUri);
        response.getOutputStream().write(contents);
      } catch (FileNotFoundException e) {
        System.err.println("Requested missing file [" + e.getMessage() + "]");
      }
      return;
    }
    
    String result = "";
    
    try {
      if (developerMode) {
        result = compile();
      }
      
      if (result.length() == 0) {
        String viewName = request.getRequestURI().substring(1);
        if (viewName.equals("")) {
          viewName = "index";
        }
        Class<? extends RequestHandler> handlerClazz = PageMappings.MAPPINGS.get(viewName);
        
        final RequestHandler controller = handlerClazz.newInstance();
        controller.doGet(request);

        final View view = getView(viewName);
        final List<Node> nodes = view.view(controller);
        
        result = new gleam.util.HtmlCreator().generate(nodes);
      }
    } catch (Throwable e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      result = "<pre>" + sw.toString() + "</pre>";
    }
    
    final PrintWriter writer = response.getWriter();
    writer.println(result);
    response.setStatus(HttpServletResponse.SC_OK);

    ((Request)request).setHandled(true);
    
    long totalTime = System.currentTimeMillis() - startTime;
    System.out.println("Execution performed in " + totalTime + "ms");
  }
}