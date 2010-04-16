package gleam.quickstart;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import gleam.CompilationError;
import gleam.CompilationResult;
import gleam.Node;
import gleam.PropertyReference;
import gleam.View;
import gleam.compiler.CompilationUnit;
import gleam.compiler.FileCompilationUnit;
import gleam.compiler.GleamCompiler;
import gleam.example.HtmlCreator;
import gleam.example.PageMappings;
import Acme.Serve.Serve;

@SuppressWarnings("serial")
public class RequestProcessor extends HttpServlet {
  private Map<String, PropertyReference<?>> bindings = new HashMap<String, PropertyReference<?>>();
  private boolean developerMode = true;
  
  private List<CompilationUnit> getCompilationUnits(File folder) {
    final List<CompilationUnit> units = new LinkedList<CompilationUnit>();
    for (final File file : folder.listFiles()) {
      if (file.getAbsolutePath().endsWith(".glimpse")) {
        final String viewName = file.getName().substring(0, file.getName().indexOf(".glimpse"));
        final String sourceName = file.getAbsolutePath();
        final CompilationUnit unit = 
          new FileCompilationUnit(viewName, sourceName, file.getAbsolutePath());
        units.add(unit);
      } else if (file.isDirectory()) {
        units.addAll(getCompilationUnits(file));
      }
    }
    
    return units;
  }
  
  /**
   * Compiles the views.
   * 
   * @return A string containing the errors ready to be output.
   */
  private String compile() {
    String result = "";
    final File viewsFolder = new File("bin");
    final List<CompilationUnit> units = getCompilationUnits(viewsFolder);
  
    // Compile all the units
    List<String> classPaths = new LinkedList<String>();
//    classPaths.add("../glimpse/bin");
    
    List<CompilationResult> compilationResults = new GleamCompiler().compile(units, classPaths);
    
    boolean hasErrors = false;
    for (CompilationResult compilationResult : compilationResults) {
      for (CompilationError error : compilationResult.getErrors()) {
        hasErrors = true;
        System.out.println(error.toString());
        result += compilationResult.getFilename() + ": " + error.toString() + "<br />";
      }
    }
    
    if (hasErrors) {
      result = "<pre>" + result + "</pre>";
    }
    
    return result;
  }
  
  public static void main(String[] args) {
    // setting properties for the server, and exchangable Acceptors
    java.util.Properties properties = new java.util.Properties();
    properties.put("port", 8080);
    properties.setProperty(Acme.Serve.Serve.ARG_NOHUP, "nohup");

    final Serve srv = new Serve();
    srv.arguments = properties;
    srv.addDefaultServlets(null); // optional file servlet
    srv.addServlet("/", new RequestProcessor()); // optional
    
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        try {
          srv.notifyStop();
        } catch (java.io.IOException ioe) {

        }
        srv.destroyAllServlets();
      }
    }));
    srv.serve();
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
  @SuppressWarnings("deprecation")
  private View getView(String viewName) {
    if (!developerMode && viewCache.containsKey(viewName)) {
      return viewCache.get(viewName);
    }
    
    try {
      final ClassLoader classLoader = new URLClassLoader(new URL[] { new File("temp/").toURL() });
      final Class<?> viewClazz;
      viewClazz = classLoader.loadClass(viewName);
      View view = (View)viewClazz.newInstance();
      viewCache.put(viewName, view);
      return view;
    } catch (Exception e) {
      throw new RuntimeException("Failed to instantiate view", e);
    }
  }
  
  private void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    long startTime = System.currentTimeMillis();
    
    response.addHeader("Expires", "Fri, 30 Oct 1998 14:19:41 GMT");
    
    // Locate all the glimpse files
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
        Class<? extends RequestHandler> handlerClazz = PageMappings.MAPPINGS.get(viewName);
        
        final RequestHandler controller = handlerClazz.newInstance();
        
        // Get any parameters from the request that are bound
        // and set them on the controller
        String queryString = request.getQueryString();
        if (queryString != null) {
          String[] parameters = queryString.split("&");
          for (String parameter : parameters) {
            String[] pair = parameter.split("=");
            if (bindings.containsKey(pair[0])) {
              bindings.get(pair[0]).set(controller, pair[1]);
            }
          }
        }
        
        controller.doGet(request);

        final View view = getView(viewName);
        final List<Node> nodes = view.view(controller);
        
        // Get bindings from the node list
        bindings = new HashMap<String, PropertyReference<?>>();
        getBindings(nodes, bindings);
        
        result = new HtmlCreator().generate(nodes);
      }
    } catch (Throwable e) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      result = sw.toString();
    }
    
    final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
    writer.write(result);
    writer.flush();
    writer.close();
    
    long endTime = System.currentTimeMillis();
    long totalTime = endTime - startTime;
    System.out.println("Execution performed in " + totalTime + "ms");
  }
  
  
  private void getBindings(List<Node> nodes, Map<String, PropertyReference<?>> bindings) {
    if (nodes == null) {
      return;
    }
    for (Node node : nodes) {
      if (node.getTagName().equals("field")) {
        PropertyReference<?> reference = (PropertyReference<?>)node.getAttribute("property");
        bindings.put(reference.getPath(), reference);
      }
      
      getBindings(node.getNodes(), bindings);
    }
    
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    handleRequest(request, response);
  }
  
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    handleRequest(request, response);
  }
}