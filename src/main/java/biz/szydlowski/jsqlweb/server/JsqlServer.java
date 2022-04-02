/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.jsqlweb.server;

import biz.szydlowski.jsqlweb.api.JdbcDriverApi;
import biz.szydlowski.jsqlweb.api.QueryApi;
import biz.szydlowski.jsqlweb.server.configuration.JdbcDriver;
import biz.szydlowski.jsqlweb.server.configuration.ReadQueries;
import biz.szydlowski.jsqlweb.server.configuration.WebParams;
import biz.szydlowski.utils.OSValidator;
import biz.szydlowski.utils.api.RestartApi;
import java.io.File;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 *
 * @author szydlowskidom
 */
public class JsqlServer implements Daemon { 
    
    static {
        try {
            System.setProperty("log4j.configurationFile", getJarContainingFolder(JsqlServer.class)+"/setting/log4j2.xml");
        } catch (Exception ex) {
        }
    }
    public static long startTime;  
    public static List<String> allowedConn = new ArrayList<>();    
    public static List<QueryApi> queryApi = new ArrayList<>();
    public static List<JdbcDriverApi> driverApi = new ArrayList<>();
    public static List<String> modules = new ArrayList<>();
    public static RestartApi restartApi=null;
    
    private static boolean stop = false;
    static final Logger logger = LogManager.getLogger(JsqlServer.class);
    public static SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss"); 
    public static String absolutePath ="";
 
    private Timer Maintenance = null;
    public static HashMap<UUID, String> hmap_cmd = new HashMap<UUID, String>();
    public static HashMap<UUID, Long> hmap_time = new HashMap<UUID, Long>();
    
    private JsqlWebServer WebServer = null;
    public static int webport=8080;
  
    public JsqlServer (){		
    } 
    
     public JsqlServer (boolean test, boolean win){
          if (test || win){
            if (!win) System.out.println("****** TESTING MODE  ********"); 
            else System.out.println("****** WINDOWS MODE  ********"); 
            try {
               initialize();
               start();       
             } catch (Exception ex) {
                logger.error(ex);
            }
        }
     }
    
        
            
     public static void main(String[] args) {
       
         if (args.length>0){
             if (args[0].equalsIgnoreCase("testing")){
                 JsqlServer  jobber  = new JsqlServer (true, false);
             }

         }
         
		
     }
   
     
     public void initialize() {
      
            if (OSValidator.isUnix()){
                 absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                       absolutePath = "/" + absolutePath.substring(0, absolutePath.lastIndexOf("/"))+"/";
            }
             System.setProperty("log4j.configurationFile", absolutePath+"setting/log4j2.xml");
             startTime=System.currentTimeMillis();
            
            Maintenance = new Timer("Maintenance", true);
            Maintenance.schedule(new MaintenanceTask(), 60000, 30000);
            
            logger.info(new Version().getAllInfo()); 
            
            ReadQueries readCommands = new ReadQueries();
            
            for (int i=0; i<queryApi.size(); i++){
                boolean exist=false;
                for (int j=0; j<modules.size(); j++){
                    if (modules.get(j).equals(queryApi.get(i).getModule())) exist=true;
                }
                if (!exist) modules.add(queryApi.get(i).getModule());
            }     
           
                  
            WebParams _WebParams = new WebParams();
            webport = _WebParams.getWebConsolePort();
          
            
            restartApi = new RestartApi(_WebParams.getRestartScriptPath());
            allowedConn = _WebParams.getAllowedConn();
            WebServer = new JsqlWebServer(webport);
            driverApi = new JdbcDriver().getJdbcDriverApiList();
     }


    /**
     *
     * @param dc
     * @throws DaemonInitException
     * @throws Exception
     */
    @Override
    public void init(DaemonContext dc) throws DaemonInitException, Exception {
          //String[] args = dc.getArguments();
          initialize();
         
    }

  
    @Override
    public void start() throws Exception {
          logger.info("Starting server");
          WebServer.start();  
          logger.info("Started server");
    }

   
    @Override
    public void stop() throws Exception {
        logger.info("Stopping daemon");
  
        
        WebServer.stopSever();   
        Maintenance.cancel();
             
        logger.info("Stopped daemon");
    }
    
    //for windows
    public static void start(String[] args) {
        System.out.println("start");
        JsqlServer jobber = new JsqlServer(false, true);
              
        while (!stop) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }
  
    public static void stop(String[] args) {
        System.out.println("stop");
        stop = true;
       
        logger.info("Stoppping daemon");
     
           
        int active = Thread.activeCount();
        Thread all[] = new Thread[active];
        Thread.enumerate(all);

        for (int i = 0; i < active; i++) {
            if (!all[i].getName().contains("RESTART")){
               logger.info("Thread to interrupt " + i + ": " + all[i].getName() + " " + all[i].getState());
               all[i].interrupt();
            } else {
                logger.info("Thread alive " + i + ": " + all[i].getName() + " " + all[i].getState());
            }
        }
    
        logger.info("Stopped daemon");  
        
        System.exit(0);
                
    }
 

   
    @Override
    public void destroy() { 
        logger.info("Destroy daemon");
        
        Maintenance = null;
     
        logger.info("*********** Destroyed daemon  ****************");
    }
      
   
          
    public class MaintenanceTask extends TimerTask {
           
            int tick=0;
            int mb = 1024 * 1024; 
            Runtime runtime = Runtime.getRuntime();
                 
            @Override
            public void run() {
                        
                     
                   long maxMemory = runtime.maxMemory();
                   long allocatedMemory = runtime.totalMemory();
                   long freeMemory = runtime.freeMemory();
                   long usedMem = allocatedMemory - freeMemory;
                   long totalMem = runtime.totalMemory();
                   
                    if (tick==50){
                        logger.info("***** Heap utilization statistics [MB] *****");
                        // available memory
                        logger.info("Total Memory: " + totalMem / mb);
                        // free memory
                        logger.info("Free Memory: " + freeMemory / mb);
                        // used memory
                        logger.info("Used Memory: " + usedMem / mb);
                        // Maximum available memory
                        logger.info("Max Memory: " + maxMemory / mb);
                       
                        tick=0;
                        
                        System.gc();
                    }
  
                 
            }
    }
    
  
    public static String getJarContainingFolder(Class aclass) throws Exception {
          CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();

          File jarFile;

          if (codeSource.getLocation() != null) {
            jarFile = new File(codeSource.getLocation().toURI());
          }
          else {
            String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();
            String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
            jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
            jarFile = new File(jarFilePath);
          }
          return jarFile.getParentFile().getAbsolutePath();
        }
     
          
}
