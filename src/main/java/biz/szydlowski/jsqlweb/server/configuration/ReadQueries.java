/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package biz.szydlowski.jsqlweb.server.configuration;

import static biz.szydlowski.jsqlweb.server.JsqlServer.queryApi;
import biz.szydlowski.jsqlweb.api.QueryApi;
import biz.szydlowski.utils.OSValidator;
import biz.szydlowski.utils.template.TemplateFile;
import java.io.File;
import java.io.IOException;
import java.util.StringJoiner;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Dominik
 */
public class ReadQueries {
   
    
   private String _setting="setting/queries.xml";
   
   static final Logger logger = LogManager.getLogger(ReadQueries.class);

       
     /** Konstruktor pobierający parametry z pliku konfiguracyjnego "config.xml"
     */
     public  ReadQueries(){
         
         if (OSValidator.isUnix()){
              String absolutePath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
                   absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"));
              _setting = absolutePath + "/" + _setting;
         }
         
         readSetting( _setting);
         TemplateFile _Template = new TemplateFile("default");
         
         for (String file : _Template.getFilenames()){
             readSetting(file);
          }
         
     }
         
     private void readSetting(String filename){
             try {
                        
		File fXmlFile = new File(filename);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();
              
                NodeList  nList = doc.getElementsByTagName("sql"); 
                
		for (int temp = 0; temp < nList.getLength(); temp++) {
                
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                          Element eElement = (Element) nNode;
                                                
                          QueryApi _queryApi = new QueryApi();
                          
                          _queryApi.setAlias(getTagValue("alias", eElement));
                          StringJoiner cmd= new StringJoiner("#NEXTQUERY#");
                                                    
                           for (int count = 0; count < eElement.getElementsByTagName("query").getLength(); count++) {
                               cmd.add(eElement.getElementsByTagName(("query")).item(count).getTextContent());
                           }
                   
                          _queryApi.setQuery(cmd.toString());
                          _queryApi.setModule(getTagValue("module", eElement));
                          _queryApi.setGroup(getTagValue("group", eElement));  
                          _queryApi.setJdbcInterface(getTagValue("jdbc_interface", eElement));  
                          _queryApi.setDescription(getTagValue("description", eElement));
                          _queryApi.setRequestConfirmation(getTagValue("requestConfirmation", eElement));
                     
                          _queryApi.setLock(false);
                          
                           queryApi.add(_queryApi);
		   }
		}
                
                logger.debug("Read queryApi done ");
                                
         }  catch (ParserConfigurationException | SAXException | IOException e) {         
                logger.fatal("queries.xml::XML Exception/Error:", e);
                System.exit(-1);
				
	 }
     }

  
      private static String getTagValue(String sTag, Element eElement) {
            try {
                NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
                Node nValue = (Node) nlList.item(0);
                return nValue.getNodeValue();
            } catch (Exception e){
                logger.error("getTagValue error " + sTag + " "+ e);
                return "ERROR";
            }

      }

 
}
