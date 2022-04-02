/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.jsqlweb.server;

import static biz.szydlowski.jsqlweb.server.HtmlScripts.EXEC;
import static biz.szydlowski.jsqlweb.server.HtmlScripts.SEARCH_IN_TABLE;
import static biz.szydlowski.jsqlweb.server.HtmlStyle.ALERTSTYLE;
import static biz.szydlowski.jsqlweb.server.HtmlStyle.CELL_COMMENT_STYLE;
import static biz.szydlowski.jsqlweb.server.HtmlStyle.ERROR404;
import static biz.szydlowski.jsqlweb.server.HtmlStyle.FIND_INPUT_STYLE;
import static biz.szydlowski.jsqlweb.server.HtmlStyle.INFOSTYLE;
import static biz.szydlowski.jsqlweb.server.HtmlStyle.LINKSTYLE;
import static biz.szydlowski.jsqlweb.server.HtmlStyle.OKSTYLE;
import static biz.szydlowski.jsqlweb.server.HtmlStyle.TABLE_STYLE;
import static biz.szydlowski.jsqlweb.server.HtmlStyle.TOPNAV_STYLE;
import static biz.szydlowski.jsqlweb.server.JsqlServer.absolutePath;
import static biz.szydlowski.jsqlweb.server.JsqlServer.hmap_cmd;
import static biz.szydlowski.jsqlweb.server.JsqlServer.hmap_time;
import static biz.szydlowski.jsqlweb.server.JsqlServer.modules;
import static biz.szydlowski.jsqlweb.server.JsqlServer.queryApi;
import static biz.szydlowski.jsqlweb.server.JsqlServer.restartApi;
import static biz.szydlowski.jsqlweb.server.JsqlServer.sdf;
import static biz.szydlowski.jsqlweb.server.JsqlServer.startTime;
import static biz.szydlowski.jsqlweb.server.JsqlServer.webport;
import biz.szydlowski.utils.OSValidator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 *
 * @author Dominik
 */
public class ServerWorkerRunnable implements Runnable {
   
     static final Logger logger = LogManager.getLogger(ServerWorkerRunnable.class);
      protected Socket clientSocket = null;
      protected SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String userInput = "default";
      boolean isGet=true;
      boolean isPost=false;
      private boolean allowedconn=false;
      BufferedReader br = null;
      private static final ExecutorService THREAD_POOL  = Executors.newCachedThreadPool();
                         
      public ServerWorkerRunnable(Socket clientSocket, boolean allowedconn) {
            this.clientSocket = clientSocket;
            this.allowedconn=allowedconn;
      }

    /**
     *
     */
    @Override
    public void run() {
        try {
           
            br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            userInput = br.readLine();
            
            /*while ((userInput = stdIn.readLine()) != null) {
                System.out.println(userInput);
            }*/

          if (userInput == null) userInput = "DEFAULT";

          //System.out.println("user input " + userInput);
          logger.debug("user input " + userInput);
          
          if (userInput.length() == 0){
              return;
           }
         
          isGet = userInput.contains("GET");
          
         if (!allowedconn) {
             logger.debug("Rejected Client : Address - " + clientSocket.getInetAddress().getHostAddress());
           
             try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                  out.print(ERROR404);
                  out.flush();
             }
             br.close();
             return;
         } 
         
          logger.debug("Accepted Client : Address - " + clientSocket.getInetAddress().getHostName());
          
          String postData = "";
          if ( userInput.contains("POST")){
               isPost=true;
             
               String line;
               int postDataI=0;
                while ((line = br.readLine()) != null && (line.length() != 0)) {
                    logger.debug("HTTP-HEADER: " + line);
                    if (line.contains("Content-Length:")) {
                        postDataI = Integer.parseInt(line.substring(line.indexOf("Content-Length:") + 16, line.length()));
                    }
                }
                postData = "";
                // read the post data
                if (postDataI > 0) {
                    char[] charArray = new char[postDataI];
                    br.read(charArray, 0, postDataI);
                    postData = new String(charArray);
                }
                               
                postData =  replaceURL(postData );                                
                 
                logger.debug("post DATA after replace " + postData); 
             
          } else {
              isPost=false;
          } 
          
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 
                    if ( isGet || isPost){
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/html");
                        out.println("<html>\n");
                        out.println("<head>");
                        out.println("<title>");
                        out.println(Version.getVersion());
                        out.println("</title>");
                        out.println("<style>\n");
                        out.println(TABLE_STYLE);        
                        out.println(CELL_COMMENT_STYLE);
                        out.println(TOPNAV_STYLE );
                        out.println(FIND_INPUT_STYLE);
                        out.println("</style>");
                        out.println("</head>\n");
                    }

                   if (userInput.length()>0) userInput = userInput.replace("HTTP/1.1", "");
                   if (userInput.length()>0) userInput = userInput.replace("%20", " ");
                   if (userInput.length()>0) userInput = userInput.replaceFirst("GET", "");      
                   if (userInput.length()>0) userInput = userInput.replaceFirst("/", "");  
                   if (userInput.length()>0) userInput = userInput.replaceAll("\\s+", " ");
                   if (userInput.length()>0) userInput = userInput.replaceAll("favicon.ico ", "");

                   if (userInput.length() == 0){
                        return;
                   }   

             if (isPost){

                if ( userInput.contains("exec.api") ){
                        //System.out.println(postData);
                        String[] parm = postData.split("&");

                        if (parm.length==2){

                            String[] lockidstr  = parm[1].split("=");
                            int lockid=0;

                            if (lockidstr.length==2){
                                lockid=Integer.parseInt(lockidstr[1]);            
                            }

                            if (lockid==-1){
                                out.append("");
                                out.flush();
                            } else {                       

                                if (!queryApi.get(lockid).isLock()){
                                    
                                    if (parm[0].startsWith("command=")){

                                        String query =  parm[0].replaceFirst("command=", "");
                                        queryApi.get(lockid).setLock(true);
                                        logger.info("Execution query start " + query);
                                        
                                         out.println(OKSTYLE);
                                         out.println(INFOSTYLE);
                                         out.println(ALERTSTYLE);
                                         
                                        //out.append("<meta http-equiv=\"refresh\" content=\"10;url=/defined_querys\" />");
                                        out.append("</br>"); 
                                        out.flush();
                                        String [] cmds = query.split("#NEXTQUERY#");
                                       
                                        out.append("Execution start: ");
                                        out.append(sdf.format(new Date()));
                                        out.append("</br>");
                                        out.flush();
                                        
                                        UUID uuid = UUID.randomUUID();
                                   
                                        hmap_cmd.put(uuid, query);
                                        hmap_time.put(uuid, System.currentTimeMillis());  
                                        
                                        Connection2Db.doQueries(queryApi.get(lockid).getJdbcInterface(), cmds, out);
                                        
                                        queryApi.get(lockid).setLock(false);
                                        hmap_cmd.remove(uuid);
                                        hmap_time.remove(uuid);
                                       
                                        out.append("</br>");
                                        out.append("Execution finished: ");
                                        out.append(sdf.format(new Date()));
                                        out.append("</br>");
                                        out.append("</br>");
                                        out.flush();
                                        
                                        System.gc();

                                  } else {  
                                     out.append("command error x0001</br>");
                                     out.flush();
                                     logger.error("command error x0001");
                                     logger.error(parm[0]);
                                  }
                            }
                            else {
                                  out.append("</br></br>LOCKED!!!</br></br>");
                                  out.flush();
                            }
                        }


                    } else {
                        out.append("</br></br>INTERNAL ERROR</br></br>");
                        out.flush();

                   }
                }  else if ( userInput.contains("restart.api") ){ 
                            logger.info("RESTART API");
                            String[] _sp = postData.split("=");
                             if (_sp.length==2){
                                switch (_sp[1]) {
                                    case "restart":
                                        out.write(new StringBuilder().append("<font color=\"red\">RESTART NucleusWeb</font>").toString());
                                        out.flush();
                                        restartApi.restart();
                                        break;
                                    case "kill":  
                                        out.write(new StringBuilder().append("<font color=\"red\">KILL NucleusWeb</font>").toString());
                                        out.flush();
                                        restartApi.kill();
                                        break;
                                    default:
                                        out.write(new StringBuilder().append("<font color=\"red\">DO NOTHING</font>").toString());
                                        break;
                                }

                            } else {
                                 logger.error("_sp.length!=2");
                            }

               } 
            }   else if (userInput.contains("defined_query")  ) {  
                    int Imodule = 0;
                    try {
                        Imodule = Integer.parseInt(userInput.replace("defined_query_", "").replaceAll("\\s+", ""));
                    } catch (Exception ee){}

                    printDefinedCommands(out, Imodule);



            }  else if (userInput.contains("tasks_count")  ) {  
                out.println(hmap_cmd.size());
            } else if (userInput.contains("tasks_time_max")  ) {  
                long dif=System.currentTimeMillis();
                for (Map.Entry<UUID, Long> entry : hmap_time.entrySet()) {
                    if (entry.getValue()< dif) dif=entry.getValue();                
                }
                dif = System.currentTimeMillis()-dif;
                out.println(dif);
            } else if (userInput.contains("tasks")  ) {  
                if (isGet)  out.println("CURRENT TASKS </br> ==================================== </br>");
                if (hmap_cmd.isEmpty()) out.println("NO TASKS </br>");
                for (Map.Entry<UUID, String> entry : hmap_cmd.entrySet()) {
                  long dif= System.currentTimeMillis() - hmap_time.getOrDefault(entry.getKey(), 0L);
                  if (isGet) {                  
                      out.println(new StringBuilder().append(entry.getKey()).append(" ----> ").append(entry.getValue()).append(" | ").append(dif).append("</br>").toString());
                  }   else {
                      out.println(new StringBuilder().append(entry.getKey()).append(" ----> ").append(entry.getValue()).append(" | ").append(dif).toString());
                  }
                }   

                printHomeAndBack(out,true,false,false); 

            } else if (userInput.replaceAll("\\s+", "").equals("ping")){ 
                 if (isGet) out.println(new StringBuilder().append("PONG").append("<br/>").toString());
                 else out.println(new StringBuilder().append("PONG").append("<br/>").toString());
            } else if (!userInput.replaceAll("\\s+", "").startsWith("defined_query")){
                //System.out.println(userInput.replaceAll("\\s+", ""));
                printDefinedCommands(out,0);
            } 

            if  ( (isGet || isPost) ) {
                 out.println("</html>");
            }

             out.flush();
             out.close();
           
           
          logger.debug("Request processed/completed...");
        }
        catch (IOException e) {
          logger.error(e);
        } finally {
       
            try { 
                if(br != null) { 
                    br.close(); 
               } 
            } catch(Exception e) {
                logger.error(e);
            }
            try { 
                clientSocket.close(); 
            } catch(Exception e) { 
                logger.error(e);
            }
                  
            
        
        }
    }
      
    private boolean checkLocking(String cmd){
           boolean ret=false;
           for (Map.Entry<UUID, String> entry : hmap_cmd.entrySet()) {
                if (entry.getValue().equals(cmd)) ret=true;
            }   
           return ret;
    }
   
   
    
    public void printDefinedModules(PrintWriter out, int k) {
  
        out.println("<div class=\"topnav\">");
        for (int i=0; i<modules.size(); i++){
             if (i==k) out.println("<a class=\"active\" href=\"defined_query_"+i+"\">"+modules.get(i)+"</a>");
             else out.println("<a href=\"defined_query_"+i+"\">"+modules.get(i)+"</a>");
        }
        
        out.println("</div>");
       
    }
    
    public List<String> getGroupInModule(String module){
             List<String> group = new ArrayList<>();
        
             for (int i=0; i<queryApi.size(); i++){
                if (queryApi.get(i).getModule().equals(module)){
                    boolean exist=false;
                    for (int j=0; j<group.size(); j++){
                        if (group.get(j).equals(queryApi.get(i).getGroup())) exist=true;
                    }
                    if (!exist) group.add(queryApi.get(i).getGroup());
                }
            } 
            return group;
    }
    
     public List<String> getJDBCInGroupAndModule(String module, String group){
             List<String> jdbc = new ArrayList<>();
        
             for (int i=0; i<queryApi.size(); i++){
                if (queryApi.get(i).getModule().equals(module) && queryApi.get(i).getGroup().equals(group)){
                    boolean exist=false;
                    for (int j=0; j<jdbc.size(); j++){
                        if (jdbc.get(j).equals(queryApi.get(i).getJdbcInterface())) exist=true;
                    }
                    if (!exist) jdbc.add(queryApi.get(i).getJdbcInterface());
                }
            } 
            return jdbc;
    }
     
     public int getItemsCountIndModuleGroupHost(String module, String group, String host){
             int licz=0;
        
             for (int i=0; i<queryApi.size(); i++){
                if (queryApi.get(i).getModule().equals(module) && queryApi.get(i).getGroup().equals(group) && queryApi.get(i).getJdbcInterface().equals(host)){
                   licz++;  
                }
            } 
            return licz;
    }
    
    public void printDefinedCommands(PrintWriter out, int mdl) {
        
     
               
        printDefinedModules(out, mdl);
        
        
        out.println("<div class=\"tab\">");
        
        if (mdl>=modules.size() || mdl<0) mdl=0;
        
        List<String> group = getGroupInModule(modules.get(mdl));
               
        if (group.isEmpty())  out.println("----");
                  
        for (int i = 0; i <  group.size(); i++) {
             out.println("<button class=\"tablinks\" onclick=\"exec(event, '"+group.get(i)+"')\" id=\"defaultOpen\">"+group.get(i)+"</button>");        
        }               
        out.println("</div>");  
        
        for (int i = 0; i <  group.size(); i++) {
            
           List<String> jdbc = getJDBCInGroupAndModule(modules.get(mdl), group.get(i));
                       
           out.println("<div id=\""+group.get(i)+"\" class=\"tabcontent\">"); 
           out.println("<h3>"+group.get(i)+"</h3>"); 
           
           out.println("<input class=\"searchInput\" type=\"text\" id=\"searchInput."+i+"\" onkeyup=\"search("+i+")\" placeholder=\"Search for alias..\" title=\"Type in an alias\">");
            
           out.println("<table id=\"commandTab."+i+"\" class=\"scroll\">");
           out.println("<thead><tr>");
           out.println("<th style=\"width:120px\">JDBC");
           out.println("<th style=\"width:400px\">Alias</th>");
           out.println("<th style=\"width:715px\">Query</th><th style=\"width:96px\"> RUN </th>");
           out.println("</tr></thead>");
           
               
           for (int k=0; k<jdbc.size();k++){ 
                for (int j = 0; j <  queryApi.size(); j++) {
               
                  if (queryApi.get(j).getGroup().equals(group.get(i)) && queryApi.get(j).getModule().equals(modules.get(mdl))
                          && queryApi.get(j).getJdbcInterface().equals(jdbc.get(k)))  {
                     
                      out.println("<tr>");
                      if (jdbc.get(k).equals("ERROR")){
                         out.println("<td style=\"width:120px\"></td>");
                      } else {
                         out.println("<td style=\"width:120px\">"+jdbc.get(k)+"</td>"); 
                      }

                      if (queryApi.get(j).getDescription().equals("ERROR")){
                           out.println("<td style=\"width:400px\">" +  queryApi.get(j).getAlias() + "</td>");
                      } else {
                            out.println("<td style=\"width:400px\" class=\"CellWithComment\">" +  queryApi.get(j).getAlias() + "<span class=\"CellComment\">"+queryApi.get(j).getDescription()+"</span></td>");
                      }

                      out.println("<td style=\"width:715px\">");
                      for (String s : queryApi.get(j).getQuery().split("#NEXTQUERY#")){
                           out.println(s+"</br>");
                      }
                      out.println("</td>");
                      if (queryApi.get(j).isRequestConfirmation()){
                          out.println("<td style=\"width:80px\"><button class=\"button-exc\"  onclick=\"executeAsk('"+j+"','"+queryApi.get(j).getQuery().replaceAll("'", "\\\\'")+"')\">GO</button></td>");
                      } else {
                           out.println("<td style=\"width:80px\"><button class=\"button-exc\"  onclick=\"execute('"+j+"','"+queryApi.get(j).getQuery().replaceAll("'", "\\\\'")+"')\">GO</button></td>");
                      }
                      out.println("</tr>");
                  }
                }
           }

      
            out.println("</table>");            
            out.println("</div>");  
          
        }  
        
        out.println(EXEC); 
        out.println(SEARCH_IN_TABLE);
        
        printApiFunction(out);
        printHomeAndBack(out,true,true,false);
        
    }
   
   
      private void printHomeAndBack(PrintWriter out, boolean printHome, boolean printClear, boolean printRestart){
          
          if (printHome){ 
              out.println("<br/><button onclick=\"goHome()\">HOME</button>\n");
              out.println("  "); 
              out.println("<script>\n");
              out.println("function goHome() {\n");
              out.println("   window.location = '/';\n");
              out.println("}\n" );
              out.println( "</script>");
          }
          
          if (printClear) out.println("<button onclick=\"goBack()\">Go Back</button>\n");
          else out.println("<button onclick=\"goBack()\">Go Back</button><br/>\n");
          out.println("\n");
          out.println("<script>\n");
          out.println("function goBack() {\n");
          out.println("    window.history.back();\n");
          out.println("}\n" );
          out.println( "</script>"); 
          
          if (printClear){
              out.println("<button onclick=\"executeClean(-1, 'clear')\">Clear</button><br/>\n");          
          }
          
          if (printRestart){
               printRestartAndKill(out);          
          }
         // out.println("</br> &copy; 2021 Landevant Research Center <b> <a href=\"mailto:support@szydlowski.biz?subject="+ Version.getVersion()+"\">support@szydlowski.biz</a></b>");
         
          out.println("</br> &copy; 2021 DoSS Research Center <b> <a href=\"mailto:support@szydlowski.biz?subject="+ Version.getVersion()+"\">support@szydlowski.biz</a></b>");
          out.println("</br> " +Version.getVersion());
     }
      
     private void printApiFunction(PrintWriter out){
          out.println("<h3>SQL viewer</h3><p style=\"color:black;background-color:white;width:1400px;\" id=\"info\"></p>");
          
           out.println(new StringBuilder().append("<script>\n").append("function execute(id, command) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"exec.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"command=\"+command+\"&id=\"+id);\n ")
                              .append(  " }\n")
                              .append( " </script>").toString());
      
          out.println(new StringBuilder().append("<script>\n").append("function executeAsk(id, command) {\n")
                              .append( "    var txt = \'Action\';\n")
                              .append( "        txt = \"Are you sure you want to execute: \\n\" \n")
                              .append( "        var cmds = \"\";\n")
                              .append( "        cmdTab = command.split('#NEXTQUERY#');\n")
                              .append( "        for (i=0; i<cmdTab.length; i++){\n")
                              .append( "         if (i<cmdTab.length-1) cmds = cmds + cmdTab[i] + \"\\n\";\n")
                              .append( "         else cmds = cmds + cmdTab[i]; \n")
                              .append( "        }\n")
                              .append( "        txt = txt + cmds;\n")
                              .append( "        var r = confirm(txt);\n")
                              .append( "        if (r == true) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"exec.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"command=\"+command+\"&id=\"+id);\n ")
                              .append( "    } else {\n")
                              .append( "    }\n")
                              .append(  " }\n")
                              .append( " </script>").toString());
          
        out.println(new StringBuilder().append("<script>\n").append("function executeClean(id, command) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"exec.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"command=\"+command+\"&id=\"+id);\n ")
                              .append(  " }\n")
                              .append( " </script>").toString());
            
    } 
     
     private void printRestartAndKill(PrintWriter out){         
          
          out.println("");
          out.println("<br/><button onclick=\"Restart('restart')\">Restart NucleusWeb</button>");
          out.println("");
          out.println("<button onclick=\"Restart('kill')\">Kill NucleusWeb</button><br/>");
          
          printRestartApiFunction(out);
    }
      
     
    private void printRestartApiFunction(PrintWriter out){
          out.println("<font color=\"red\"><p id=\"info\"></p></font>");
      
          out.println(new StringBuilder().append("<script>\n").append("function Restart(type) {\n")
                              .append( "    var txt = \'Action\';\n")
                              .append( "    if (type === 'restart') {\n")
                              .append( "        txt = \"Are you sure you want to restart daemon?\";\n")
                              .append( "    } else if (type === 'kill') {\n")
                              .append( "        txt = \"Are you sure you want to kill daemon?\";\n")
                              .append( "    } \n")
                              .append( "    var r = confirm(txt);\n")
                              .append( "    if (r == true) {\n")
                              .append( "        var xhttp = new XMLHttpRequest();\n")
                              .append( "        xhttp.onreadystatechange = function() {\n")
                              .append( "           if (this.status == 200) {")
                              .append( "               document.getElementById(\"info\").innerHTML = this.responseText;\n")
                              .append( "           }\n")
                              .append( "       };\n")
                              .append( "       xhttp.open(\"POST\", \"restart.api\", true);")
                              .append( "       xhttp.setRequestHeader(\"Content-type\", \"application/x-www-form-urlencoded\");\n")
                              .append( "       xhttp.send(\"action=\"+type);\n ")
                              .append( "    } else {\n")
                              .append( "    }\n")
                              .append(  " }\n")
                              .append( " </script>").toString());
            
      }
    private static <T> T timedCall(Callable<T> c, long timeout, TimeUnit timeUnit)
            throws InterruptedException, ExecutionException, TimeoutException {
            FutureTask<T> task = new FutureTask<T>(c);
            THREAD_POOL.execute(task);
            return task.get(timeout, timeUnit);
    }
   
   private String replaceURL(String postData){
          
         try {
              postData = URLDecoder.decode(postData, "UTF-8");
          } catch (UnsupportedEncodingException ex) {
              logger.error("replaceURL decoder");
          }
              
          postData = postData.replaceAll("%C4%84", "A");
          postData = postData.replaceAll("%C4%85", "a");
          postData = postData.replaceAll("%C4%87", "C");
          postData = postData.replaceAll("%C4%88", "c");
          postData = postData.replaceAll("%C4%98", "E");
          postData = postData.replaceAll("%C4%99", "e");
          postData = postData.replaceAll("%C5%81", "L");
          postData = postData.replaceAll("%C5%82", "l");

          postData = postData.replaceAll("%C5%83", "N");
          postData = postData.replaceAll("%C5%84", "n");

          postData = postData.replaceAll("%C3%B3", "o");
          postData = postData.replaceAll("%C3%93", "O");

          postData = postData.replaceAll("%C5%9A", "S");
          postData = postData.replaceAll("%C5%9B", "s");
          postData = postData.replaceAll("%C5%B9", "Z");
          postData = postData.replaceAll("%C5%BA", "z");
          postData = postData.replaceAll("%C5%BB", "Z");
          postData = postData.replaceAll("%C5%BC", "z");
          postData = postData.replaceAll("%20", " ");

         
          postData = postData.replaceAll("%5B", "[");
          postData = postData.replaceAll("%5C", "\\");
          postData = postData.replaceAll("%5D", "]");
          postData = postData.replaceAll("%21", "!");
          postData = postData.replaceAll("%22", "\"");
          postData = postData.replaceAll("%23", "#");
          postData = postData.replaceAll("%24", "$");
          postData = postData.replaceAll("%28", "(");
          postData = postData.replaceAll("%29", ")");
          postData = postData.replaceAll("%40", "@");
          postData = postData.replaceAll("%3F", "?");
          postData = postData.replaceAll("%25", "%");

          return postData;          
                
    }
   
    
}