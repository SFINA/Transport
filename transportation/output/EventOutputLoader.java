    /*
 * Copyright (C) 2017 SFINA Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package transportation.output;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import network.FlowNetwork;
import network.Link;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import static protopeer.time.EventScheduler.logger;

/**
 *
 * @author Marius Bild
 */
public class EventOutputLoader {
    
    private  String outputFilename ; 
    private HashMap<String, Double > vehiclesPerHour ;
    private FlowNetwork SfinaNet ; 
  
    
    public EventOutputLoader(String filename, FlowNetwork net){
        
        this.outputFilename = filename; 
        this.SfinaNet = net ; 
    }
    
    
    public void loadXml()throws ParserConfigurationException, 
                                SAXException, 
                                IOException
    {    
        if(outputFilename==null){
            logger.debug("Output filename not set in <EventOutputLoader.loadXml()> !");
            return; 
        }
        if(outputFilename.substring(outputFilename.lastIndexOf(".")+1).equals("gz")){
            String newFileName = outputFilename.replace(".gz", "");
            decompressGzipFile(outputFilename,newFileName);
            this.outputFilename = newFileName ;
        }
        this.vehiclesPerHour = new HashMap<>(); 
        
        File fXmlFile = new File(outputFilename);
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	Document doc = dBuilder.parse(fXmlFile); 
        doc.getDocumentElement().normalize();
        NodeList events = doc.getElementsByTagName("event");
        // initialize start time of the simulation as time of first event
        double startTime = Double.parseDouble(events.item(0).getAttributes().getNamedItem("time").getNodeValue()) ;
        double endTime = Double.parseDouble(events.item(events.getLength()-1).getAttributes().getNamedItem("time").getNodeValue()) ;
        double totSimTime ;
        for(Link currentLink : this.SfinaNet.getLinks()){
            
            String currentLinkId = currentLink.getIndex() ; 
            int vehicles = 0 ;
            for(int j=0; j<events.getLength(); j++){
                
                Node currentEvent = events.item(j) ;
                double eventTime = Double.parseDouble(currentEvent.getAttributes().getNamedItem("time").getNodeValue()) ;
                Node link = currentEvent.getAttributes().getNamedItem("link"); 
                Node type = currentEvent.getAttributes().getNamedItem("type") ;
                Node vehicle = currentEvent.getAttributes().getNamedItem("vehicle"); 
                if(link==null || type==null || vehicle==null){continue;}
                String eventLink  = link.getNodeValue();
                String eventType  = type.getNodeValue();
                String eventVehicle = vehicle.getNodeValue() ; 
                if(currentLinkId.equals(eventLink)&&
                   eventType.equals("entered link")) // || eventType.equals("vehicle enters traffic")))
                {    
                    boolean foundExit = false;
                    for(int k=j+1; k<events.getLength();k++){
                        Node compLink = events.item(k).getAttributes().getNamedItem("link"); 
                        Node compType = events.item(k).getAttributes().getNamedItem("type") ;
                        Node compVehicle = events.item(k).getAttributes().getNamedItem("vehicle");
                        if(compLink==null || compType==null || compVehicle==null){continue;}
                        String comparingEvent = compType.getNodeValue() ; 
                        String comparingLink = compLink.getNodeValue(); 
                        String comparingVehicle = compVehicle.getNodeValue() ; 
                        
                        if(comparingLink.equals(eventLink)&&
                           comparingEvent.equals("left link") //|| comparingEvent.equals("vehicle leaves traffic")) 
                                &&
                           comparingVehicle.equals(eventVehicle))
                        {   
                            foundExit = true;
                            break;
                        }
                    }
                    if(foundExit){
                        vehicles++ ; 
                    }
                }
            }
            totSimTime = (endTime - startTime)/3600  ;
            double vehPHour = vehicles/totSimTime  ;
            DecimalFormat df = new DecimalFormat("#,##");      
            vehPHour = Double.valueOf(df.format(vehPHour));
            this.vehiclesPerHour.putIfAbsent(currentLinkId, vehPHour) ;
        }
    }
    public HashMap<String,Double> getLinkFlowValues(){
        return this.vehiclesPerHour ; 
    }
    
    public void setOutputFilename(String filename){
        this.outputFilename = filename  ; 
    }
    
    private static void decompressGzipFile(String gzipFile, String newFile) {
        try {
            FileInputStream fis = new FileInputStream(gzipFile);
            GZIPInputStream gis = new GZIPInputStream(fis);
            FileOutputStream fos = new FileOutputStream(newFile);
            byte[] buffer = new byte[1024];
            int len;
            while((len = gis.read(buffer)) != -1){
                fos.write(buffer, 0, len);
            }
            //close resources
            fos.close();
            gis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
