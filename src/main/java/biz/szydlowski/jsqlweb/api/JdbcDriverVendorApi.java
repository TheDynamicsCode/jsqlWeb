/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.jsqlweb.api;

/**
 *
 * @author Dominik
 */
public class JdbcDriverVendorApi {
    
    private String driver_vendor="ifn";
    private String url_template="url";
    private String _class="class";
     
    public void setDriverVendor(String set){
        driver_vendor=set;
    }
    
    public void setUrlTemplate(String set){
        url_template=set;
    }
    
    public void setClassName(String set){
        _class=set;
    }
    
   
    public String getDriverVendor(){
        return driver_vendor;
    }
   
    public String getUrlTemplate(){
        return url_template;
    }
    
    
    public String getClassName(){
        return _class;
    }
        
    
}
