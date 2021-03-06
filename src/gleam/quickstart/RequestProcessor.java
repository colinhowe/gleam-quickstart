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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import pages.PageMappings;

public class RequestProcessor extends AbstractHandler {
  private final boolean developerMode;
  private final ViewCache viewCache;
  
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
  
  public static void main(String[] args) throws Exception {
    Properties config = new Properties();
    config.load(new FileInputStream("config.properties"));
    final boolean developerMode = Boolean.parseBoolean(config.getProperty("developermode"));
    
    Server server = new Server(Integer.parseInt(config.getProperty("port")));
    server.setHandler(new RequestProcessor(developerMode));
    server.start();
  }
  
  RequestProcessor(final boolean developerMode) {
    this.developerMode = developerMode;
    viewCache = new ViewCache(developerMode);
    compile();
  }
  
  public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    long startTime = System.currentTimeMillis();
    
    response.addHeader("Expires", "Fri, 30 Oct 1998 14:19:41 GMT");
    response.addHeader("Content-Type", "text/html");
    
    // Locate all the gleam files
    String requestUri = request.getRequestURI().substring(1);
    if (requestUri.indexOf(".") != -1) {
      // Get from the resources folder
      try {
        final byte[] contents = FileUtils.readFile("resource/" + requestUri);
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

        final View view = viewCache.getView(viewName);
        final List<Node> nodes = view.view(controller);
        
        result = new gleam.util.HtmlCreator().generate(nodes);
      }
    } catch (Throwable e) {
      StringWriter sw = new StringWriter();
      e.printStackTrace(new PrintWriter(sw));
      result = "<pre>" + sw.toString() + "</pre>";
    }
    
    response.getWriter().println(result);
    response.setStatus(HttpServletResponse.SC_OK);

    baseRequest.setHandled(true);
    
    long totalTime = System.currentTimeMillis() - startTime;
    System.out.println("Execution performed in " + totalTime + "ms");
  }
}