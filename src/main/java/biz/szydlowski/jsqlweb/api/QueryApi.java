/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.jsqlweb.api;

import biz.szydlowski.jsqlweb.server.configuration.HexUtils;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Dominik
 */
public class QueryApi {

    private boolean requestConfirmation;
    private String alias;
    private String group;
    private String query; 
    private String jdbc_interface; 
    private String description;
    private String module;
    private boolean lock;
    private List<String> queries = new ArrayList<>();
    
        /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param alias the alias to set
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * @return the command
     */
    public String getQuery() {
        return HexUtils.convertHexToString(query);
    }

    /**
     * @param command the command to set
     */
    public void setQuery(String query) {
        this.query = HexUtils.convertStringToHex(query);
    }

    /**
     * @return the lock
     */
    public boolean isLock() {
        return lock;
    }

    /**
     * @param lock the lock to set
     */
    public void setLock(boolean lock) {
        this.lock = lock;
    }

    /**
     * @return the module
     */
    public String getModule() {
        return module;
    }

    /**
     * @param module the module to set
     */
    public void setModule(String module) {
        this.module = module;
    }

    /**
     * @return the host
     */
    public String getJdbcInterface() {
        return  jdbc_interface;
    }

    /**
     * @param host the host to set
     */
    public void setJdbcInterface(String  jdbc_interface) {
        this. jdbc_interface =  jdbc_interface;
    }

    /**
     * @return the requestConfirmation
     */
    public boolean isRequestConfirmation() {
        return requestConfirmation;
    }

    /**
     * @param requestConfirmation the requestConfirmation to set
     */
    public void setRequestConfirmation(String requestConfirmation) {
        this.requestConfirmation = Boolean.parseBoolean(requestConfirmation);
    }

 
   
}
