/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.jsqlweb.server;

import biz.szydlowski.jsqlweb.api.JdbcDriverApi;
import static biz.szydlowski.jsqlweb.server.JsqlServer.driverApi;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Properties;

/**
 *
 * @author Dominik
 */
public class Connection2Db {
  
     public static Connection getConnection(JdbcDriverApi jdbc, PrintWriter out) {
           Properties prop = new Properties();

           prop.setProperty("user", jdbc.getUser());
           prop.setProperty("password", jdbc.getPassword());
           prop.setProperty("applicationname", "jsql.web");
           
         
           //System.out.println(className);
           //System.out.println(url);
            
           try {
               Class.forName(jdbc.getClassName());
               DriverManager.setLoginTimeout(jdbc.getLoginTimeout());
               Connection connection = DriverManager.getConnection(jdbc.getUrl(), prop);
                        
           return connection;
          } catch (ClassNotFoundException e) {
                 System.err.println("ClassNotFoundException:");
                 System.err.println("error message=" + e.getMessage());
                 return null;

          } catch (SQLException e) {
              while(e != null) {
                            out.append("[ERROR] SQL Exception/Error:");
                            System.err.println("[ERROR] error message=" + e.getMessage());
                            System.err.println("[ERROR] SQL State= " + e.getSQLState());
                            System.err.println("[ERROR] Vendor Error Code= " + e.getErrorCode());

                            // it is possible to chain the errors and find the most
                            // detailed errors about the exception
                            e = e.getNextException( );
               }
               return null;
          } catch (Exception e2) {
            // handle non-SQL exception …
              System.err.println("[ERROR] SQL Exception/Error:");
              System.err.println("[ERROR] error message=" + e2.getMessage());

               return null;
          }

   }   
     
    public static void doQueries(String driver_interface, String[] queries, PrintWriter out){ 
        Connection conn = null;
         boolean found=false;
           for (JdbcDriverApi jdbcTypeApi : driverApi){
               if (jdbcTypeApi.getInterfaceName().equalsIgnoreCase(driver_interface)){
                   found=true;
                   conn = getConnection(jdbcTypeApi, out);
                   
               }
           }
           
           
         if (!found){
             out.println("</br><div class=\"alert\"><strong>Error! </strong>Driver not found " + driver_interface + "</div></br>");
             out.println("</br>");
             return;
         } 
         
       if (conn == null){
          out.println("</br><div class=\"alert\"><strong>Error! </strong>Connection error</div></br>");
          return;
        }

        out.println("</br><div class=\"ok\"><strong>OK.</strong> Connected to " + driver_interface + " !</div>"); 
            
        for (String query : queries){
            out.println("</br><div class=\"info\"> RUN QUERY: " + query +"</div></br>"); 
            doQuery( conn,  query, out);
            out.println("</br></br>");
        } 
         
        CloseJDBCConn(conn);
        out.println("<div class=\"info\"> Close connection to database </div></br>"); 
    }
     
     
    private static void doQuery(Connection conn, String query, PrintWriter out){
           String newline = System.getProperty("line.separator");        
           
         
        // System.out.println("[INFO] Connecting to database "+  cmdProps.getProperty("host") + ".....");
 
              
             ResultSet rs = null;
             ResultSetMetaData rsmd = null;
             boolean isResult = false;
             boolean hasMoreResults = false;
             boolean hadResult = false; 
            
             try {  
                                
               Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
             
               int timeout=30;
              
               stmt.setQueryTimeout(timeout);  // set timeout to 30 sec.
              
               isResult = stmt.execute(query);
          
               int updates = 0;
               SQLWarning sqlW = null;  
               sqlW = stmt.getWarnings();
                 
               do {
                     sqlW = stmt.getWarnings();
                                       
                     if (sqlW != null) { 
                          out.append(printWarnings(sqlW)).append(newline);
                          stmt.clearWarnings();
                     } 
                 
                  if (isResult) {
                      
                      out.println("<table style=\"width:100%\">");
                 
                      rs = stmt.getResultSet();
                      rsmd = rs.getMetaData();
                      int numCol = rsmd.getColumnCount();
                        out.append("<tr>");
                        for (int i=1; i<=numCol; i++) {
                            out.append("<th>").append(rsmd.getColumnLabel(i)).append("</th>");
                        }
                        out.append("</tr>");
                        out.append(newline);
                     
                      out.append(newline);
                      
                       boolean b = rs.last();
                       int rowsSelected = rs.getRow();
                       rs.beforeFirst();
                                      
                      while (rs.next()) {
                           out.append("<tr>");
                           for (int i=1; i<=numCol; i++) {
                                out.append("<td>").append(rs.getString(i)).append("</td>");
                           }
                           out.append("</tr>");
                           out.append(newline);
                      }
                              
                    out.println("</table>");   
                    
                    if (rowsSelected >= 0){
                         out.println(Integer.toString(rowsSelected));
                         out.println("  row(s) affected </br>");
                         out.println(newline);
                    }
                     
                        
                  } else {
                      
                       updates = stmt.getUpdateCount();
                       sqlW = stmt.getWarnings();
                       if (sqlW != null)
                       {
                         stmt.clearWarnings();
                       }
                       if ((updates >= 0)) {
                           out.append(Integer.toString(updates));
                           out.append("  row(s) updated </br>").append(newline);
                           out.append(newline);
                        }
                   }
                  
                    hasMoreResults = stmt.getMoreResults();
                    isResult = hasMoreResults;
                    if (hasMoreResults) out.append(newline).append(newline);
                }  while ((hasMoreResults) || (updates != -1));
               
                if ( out.toString().length()==0){
                    out.println("[INFO] Query: "  + query );
                    out.println("  was done successfully.</br>");
                }
          

               try {
                        rs.close();
               }  catch(Exception ignore) { }
               try {
                         stmt.close();
               } catch(Exception ignore) {  }

            } 
             catch (SQLException e) {   
                   out.println("</br><div class=\"alert\">\n" +
                            "  <strong>Error!</strong> SQLException " + e.getMessage() + " </div></br>");
                    //e.printStackTrace();
                  
             } 
             
              
               
        }
    
       protected static String printWarnings(SQLException ex){
             String retString="";
             String newline = System.getProperty("line.separator");

             while (ex != null)
             {
               if (checkForTSQLPrint(ex))
               {
                   retString = retString + ex.getMessage() + newline;
                   ex = ex.getNextException();
               } else {
                   retString = retString + ex.getMessage() +  newline;
                   ex = ex.getNextException();
               }
             }
            
             return retString;
             
       }
      
      protected static boolean checkForTSQLPrint(SQLException ex)  {
             boolean returnVal = false;
             if ((ex.getErrorCode() == 0) && (ex.getSQLState() == null))
             {
               returnVal = true;
                             
             }
             return returnVal;
      }
      
       
   public static void CloseJDBCConn(Connection conn){
        if (conn != null){
            try  {
                 
                 conn.close();
             }  catch (SQLException e){
                 while(e != null) {
		
			e = e.getNextException( );
	         }
            } catch (Exception e2) {
            }
        } else {
       }
   }
                              
      
     
}
