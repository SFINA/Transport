/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package transportation.input;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node ;
import org.xml.sax.SAXException;
import static protopeer.time.EventScheduler.logger;

/**
 *
 * @author Marius Bild
 */
public class transformMATSIMtoSFINAInput {
    
    public String networkFileLocation  ;
    public String plansFileLocation ; 
    public String sfinaInputFileLocation ; 
    private final String columnSeparator = "," ; 
    
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        
        String networkLoc = "C:\\Users\\Marius Bild\\MATSIM_01\\matsim-example-project\\src\\main\\java\\org\\matsim\\example\\test input\\network.xml" ; ;
        String plansLoc   = "C:\\Users\\Marius Bild\\MATSIM_01\\matsim-example-project\\src\\main\\java\\org\\matsim\\example\\test input\\plans.xml" ; 
        String sfinaLoc   = "C:\\Users\\Marius Bild\\SFINA_05\\SFINA\\core\\experiments\\experiment-03\\peer-0\\input";
        transformMATSIMtoSFINAInput transformer = new transformMATSIMtoSFINAInput(networkLoc,plansLoc,sfinaLoc) ; 
        transformer.parseNetworkToSfina();
    }

    
    public transformMATSIMtoSFINAInput(String netLoc, String plansLoc, String sfinaLoc){
        this.networkFileLocation = netLoc ; 
        this.plansFileLocation = plansLoc ;
        this.sfinaInputFileLocation = sfinaLoc ; 
    }
    
    public void parseNetworkToSfina() throws ParserConfigurationException, SAXException, IOException{
        
        String fileEnding = this.networkFileLocation.substring(networkFileLocation.lastIndexOf(".")+1) ;
        if(fileEnding.equals("gz")){
            String newFileName = networkFileLocation.replace(".gz", "");
            decompressGzipFile(networkFileLocation,newFileName);
            this.networkFileLocation = newFileName ;
        }
        
        File networkXml = new File(networkFileLocation) ;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	Document doc = dBuilder.parse(networkXml);
        NodeList nodes = doc.getElementsByTagName("node");
        NodeList links = doc.getElementsByTagName("link") ; 
        HashMap<String,HashMap<String,String>> nodeData = getNodeData(nodes) ;
        HashMap<String,HashMap<String,String>> linkData = getLinkData(links) ;
        getPlansInformation(plansFileLocation,nodeData,linkData) ;
        writeTopologyNodeData(nodeData,sfinaInputFileLocation+"/time_1/"+"topology/"+"nodes.txt");
        writeTopologyLinkData(linkData,sfinaInputFileLocation+"/time_1/"+"topology/"+"links.txt");
        writeFlowNodeData(nodeData,sfinaInputFileLocation+"/time_1/"+"flow/"+"nodes.txt");
        writeFlowLinkData(linkData,sfinaInputFileLocation+"/time_1/"+"flow/"+"links.txt");
    }

    public HashMap<String,HashMap<String,String>> getNodeData(NodeList nodes){
        HashMap<String,HashMap<String,String>> nodeProperties = new HashMap<>();
        for (int j=0; j<nodes.getLength(); j++){
            Node currentNode = nodes.item(j); 
            Node id = currentNode.getAttributes().getNamedItem("id"); 
            Node x  = currentNode.getAttributes().getNamedItem("x"); 
            Node y  = currentNode.getAttributes().getNamedItem("y");
            String xCoord ; 
            String yCoord ;
            String id_ ; 
            if(id==null){continue ;}
            else{id_ = id.getNodeValue() ;}
            if(x==null){xCoord = "0";}
            else{xCoord = x.getNodeValue();} 
            if(y==null){yCoord = "0";}
            else{yCoord = y.getNodeValue();}
            HashMap<String,String> properties = new HashMap<>();
            properties.putIfAbsent("xcoord", xCoord) ; 
            properties.putIfAbsent("ycoord", yCoord) ; 
            nodeProperties.putIfAbsent(id_,properties ); 
            }
        return nodeProperties ; 
    }
    
    public HashMap<String,HashMap<String,String>> getLinkData(NodeList links){
        
        HashMap<String,HashMap<String,String>> linkProperties = new HashMap<>();
        for (int j=0; j<links.getLength(); j++){
            Node currentNode = links.item(j); 
            Node id = currentNode.getAttributes().getNamedItem("id"); 
            Node from  = currentNode.getAttributes().getNamedItem("from"); 
            Node to  = currentNode.getAttributes().getNamedItem("to");
            Node length  = currentNode.getAttributes().getNamedItem("length");
            Node capacity  = currentNode.getAttributes().getNamedItem("capacity");
            Node freespeed  = currentNode.getAttributes().getNamedItem("freespeed");
            Node permlanes  = currentNode.getAttributes().getNamedItem("permlanes");
            String fromNode ; 
            String toNode ;
            String id_ ;
            String length_ ; 
            String capacity_ ; 
            String freespeed_; 
            String permlanes_ ;
            boolean isActivated = true ; 
            if(id==null || from==null || to==null || length==null){continue ;}
            else{
                id_ = id.getNodeValue() ;
                fromNode = from.getNodeValue()  ;
                toNode = to.getNodeValue()  ;
                length_ = length.getNodeValue() ; 
            }
            if(capacity==null){capacity_ = "0"; isActivated = false ;}
            else{capacity_ = capacity.getNodeValue();} 
            if(freespeed==null){freespeed_ = "0"; isActivated = false ;}
            else{freespeed_ = freespeed.getNodeValue();}
            if(permlanes==null){permlanes_ = "0"; isActivated = false ;}
            else{permlanes_ = permlanes.getNodeValue();}
            HashMap<String,String> properties = new HashMap<>();
            properties.putIfAbsent("from_node_id", fromNode) ; 
            properties.putIfAbsent("to_node_id", toNode) ;
            properties.putIfAbsent("capacity", capacity_) ;
            properties.putIfAbsent("freespeed", freespeed_) ;
            properties.putIfAbsent("permlanes", permlanes_) ;
            properties.putIfAbsent("length", length_) ;
            if(isActivated){properties.putIfAbsent("status", "1") ;}
            else{properties.putIfAbsent("status", "0") ;} ;
            linkProperties.putIfAbsent(id_,properties ); 
            }
        return linkProperties ; 
    }
    
    public void getPlansInformation(String plansLocation, HashMap<String,HashMap<String,String>> nodeData,HashMap<String,HashMap<String,String>> linkData) throws ParserConfigurationException,
                                                                                                                                                                  SAXException, 
                                                                                                                                                                  IOException{
        String fileEnding = this.plansFileLocation.substring(plansFileLocation.lastIndexOf(".")+1) ;
        if(fileEnding.equals("gz")){
            String newFileName = plansFileLocation.replace(".gz", "");
            decompressGzipFile(plansFileLocation,newFileName);
            this.plansFileLocation = newFileName ;}
        File plansXml = new File(plansFileLocation) ;
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
	Document doc = dBuilder.parse(plansXml);
        NodeList agents = doc.getElementsByTagName("person") ; 
        for(int j=0; j<agents.getLength(); j++){
            Node agent = agents.item(j) ;
            Node agentId = agent.getAttributes().getNamedItem("id") ; 
            if(agentId ==null || agent.getChildNodes().getLength()==0){continue;}
            String currentId = agentId.getNodeValue()  ; 
            NodeList plans = agent.getChildNodes() ;
            Node plan = null ;
            for(int n=0;n<plans.getLength();n++){
                if(plans.item(n).getNodeName().equals("plan")){
                    plan = plans.item(n) ; 
                    break; 
                }
            }
            if(plan==null || plan.getChildNodes().getLength()<3){continue;}
            NodeList actsNlegs = plan.getChildNodes() ;
            String[] nodes = null ;
            ArrayList<String> realNodes = new ArrayList<>();
            for(int k=0; k<actsNlegs.getLength();k++){
                Node leg = actsNlegs.item(k) ;
                if(!leg.getNodeName().equals("leg")){
                    continue;}
                if(!leg.getAttributes().getNamedItem("mode").getNodeValue().equals("car")){
                    continue;}
                if(leg.getChildNodes().getLength()==0){
                    continue;}
                else{
                   NodeList routes = leg.getChildNodes()  ;
                   for(int m=0;m<routes.getLength();m++){
                       Node route = routes.item(m) ; 
                       if(route.getNodeName().equals("route")&&!route.getTextContent().isEmpty()){
                           nodes = route.getTextContent().split(" ");  //here actually link id's are read, not node id's
                           int iter = 0 ; 
                           for(String node : nodes){
                               if(!linkData.containsKey(node)){
                                   continue ; 
                               }
                               String startNode = null ; 
                               if(iter==0){
                                 startNode = linkData.get(node).get("from_node_id") ;
                                 realNodes.add(startNode); 
                               }
                               String endNode = linkData.get(node).get("to_node_id"); 
                               realNodes.add(endNode);
                               iter++; 
                           }
                           break; 
                       }
                   }
                }
                if(realNodes!=null){logger.debug("Found route for agent "+currentId+": "+nodes);break;}
            }
            if(realNodes.isEmpty()){continue;}
            ArrayList<String> usedNodes = new ArrayList<>(); 
            for(String currentNode : realNodes){
                
                HashMap<String,String> currentProps = nodeData.get(currentNode) ; 
                if(currentProps.containsKey("agents")){
                    String nodeAgents = currentProps.get("agents") ; 
                    nodeAgents +=(currentId+"_");
                    currentProps.put("agents",nodeAgents) ;}
                else{ 
                    String nodeAgents = currentId+"_"; 
                    currentProps.put("agents", nodeAgents);
                }
                if(usedNodes.contains(currentNode)){continue; }   //avoid the same node is counted twice if it's only once a ending or starting node!
                if(currentProps.containsKey("agentnumber") && currentProps.containsKey("type")){
                    String type = currentProps.get("type") ; 
                    Integer agentNumber = Integer.parseInt(currentProps.get("agentnumber")) ; 
                    if((realNodes.get(0).equals(currentNode)&& type.equals("h")) ||
                       (realNodes.get(realNodes.size()-1).equals(currentNode)&& type.equals("w"))){
                        agentNumber+=1 ; 
                        usedNodes.add(currentNode) ; 
                        currentProps.put("agentnumber", Integer.toString(agentNumber)) ;
                    }   
                    else{
                        if(type.equals("-")&& realNodes.get(0).equals(currentNode)){
                            currentProps.put("type", "h"); 
                            currentProps.put("agentnumber", "1") ;        
                            usedNodes.add(currentNode) ;
                        }
                        if(type.equals("-")&& realNodes.get(realNodes.size()-1).equals(currentNode)){
                            currentProps.put("type", "w"); 
                            currentProps.put("agentnumber", "1") ;
                            usedNodes.add(currentNode) ;
                        }
                    }
                }
                else{
                    String agentNumber = "0" ; 
                    if(realNodes.get(0).equals(currentNode)){
                        agentNumber = "1" ;
                        usedNodes.add(currentNode) ; 
                        String type = "h" ; 
                        currentProps.put("type", type) ; 
                        currentProps.put("agentnumber",agentNumber ) ;
                    }
                    else{
                        if(realNodes.get(realNodes.size()-1).equals(currentNode)){
                            agentNumber = "1" ;
                            usedNodes.add(currentNode) ; 
                            String type = "w" ; 
                            currentProps.put("type", type); 
                            currentProps.put("agentnumber", agentNumber) ; 
                        }
                        else{
                            String type = "-" ;
                            currentProps.put("type",type) ; 
                            currentProps.put("agentnumber",agentNumber); 
                        }
                    }
                }
                nodeData.put(currentNode, currentProps) ;
            }
        }
    }
    
    public void writeTopologyNodeData(HashMap<String,HashMap<String,String>> data , String location){
        try{
            File file = new File(location);
            if(file.exists()){
                logger.debug("File: "+location+" already exists, overwriting data!");
                file.delete() ; }
            file.createNewFile();
            PrintWriter writer = new PrintWriter(new FileWriter(file,false));
            writer.println("id,status") ; 
            for(String currentId : data.keySet()){
                writer.println(currentId+",1");
            }
            writer.close();   
        }
        catch(IOException ex){
        }
    }
    
    public void writeTopologyLinkData(HashMap<String,HashMap<String,String>> data , String location){
        try{
            File file = new File(location);
            if(file.exists()){
                logger.debug("File: "+location+" already exists, overwriting data!");
                file.delete() ; }
            file.createNewFile();
            PrintWriter writer = new PrintWriter(new FileWriter(file,false));
            writer.println("id,from_node_id,to_node_id,status");
            for(String currentId : data.keySet()){
                HashMap<String,String> currentProps = data.get(currentId) ;
                writer.println(currentId+","+currentProps.get("from_node_id")
                                        +","+currentProps.get("to_node_id")
                                        +","+currentProps.get("status")); 
            }
            writer.close();
        }
        catch(IOException ex){
        }
    }
    public void writeFlowNodeData(HashMap<String,HashMap<String,String>> data, String location){
        try{
            File file = new File(location);
            if(file.exists()){
                logger.debug("File: "+location+" already exists, overwriting data!");
                file.delete() ; }
            file.createNewFile();
            PrintWriter writer = new PrintWriter(new FileWriter(file,false));
            writer.println("id,xcoord,ycoord,agentnumber,agents,type");
            for(String currentId : data.keySet()){
                HashMap<String,String> currentProps = data.get(currentId) ;
                String xcoord = currentProps.get("xcoord") ;
                if(xcoord ==null){xcoord = "0";}
                String ycoord = currentProps.get("ycoord") ;
                if(xcoord ==null){ycoord = "0";}
                String agentnumber = currentProps.get("agentnumber") ;
                if(agentnumber ==null){agentnumber = "-";}
                String agents = currentProps.get("agents") ;
                if(agents ==null){agents = "-";}
                String type = currentProps.get("type") ;
                if(type ==null){type = "-";}
                writer.println(currentId+","+xcoord
                                        +","+ycoord
                                        +","+agentnumber
                                        +","+agents
                                        +","+type);
            }
            writer.close();
        }
        catch(IOException ex){
        }
    }
    
    public void writeFlowLinkData(HashMap<String,HashMap<String,String>> data, String location){
        try{
            File file = new File(location);
            if(file.exists()){
                logger.debug("File: "+location+" already exists, overwriting data!");
                file.delete() ; }
            file.createNewFile();
            PrintWriter writer = new PrintWriter(new FileWriter(file,false));
            writer.println("id,capacity,vehiclesperhour,permlanes,length,freespeed");
            for(String currentId : data.keySet()){
                HashMap<String,String> currentProps = data.get(currentId) ;
                    writer.println(currentId+","+currentProps.get("capacity")
                                            +",-"  //vehicles per hour value always not present in input!
                                            +","+currentProps.get("permlanes")
                                            +","+currentProps.get("length")
                                            +","+currentProps.get("freespeed")); 
            }
            writer.close();
        }
        catch(IOException ex){
        }
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
