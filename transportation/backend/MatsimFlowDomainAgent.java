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
package transportation.backend;

import agents.backend.FlowDomainAgent;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import network.FlowNetwork;
import network.Link;
import network.Node;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import protopeer.util.quantities.Time;
import transportation.input.MatsimConfig;
import transportation.input.TransportationLinkState;
import transportation.input.TransportationNodeState;
import transportation.output.EventOutputLoader;

/**
 *
 * @author Marius Bild
 */
public class MatsimFlowDomainAgent extends FlowDomainAgent{
        
    private boolean converged;
    private static final Logger logger = Logger.getLogger(MatsimFlowDomainAgent.class) ;
    private FlowNetwork SfinaNet ; 
    private Controler matsimControler ;
    private HashMap<Enum,Object> backendParameters ; 
    private String xmlFolderLocation ;
    private final String xmlFolderName = "temporaryXMLs" ; 
    private final String xmlNetworkFilename ="network.xml" ; 
    private final String xmlPlansFilename = "plans.xml" ;
    private final String xmlConfigFilename = "config.xml" ;
    private final String DOMdoctypeNet = "network" ; 
    private final String DOMdoctypePlans = "plans" ;
    private final String DOMdoctypeConfig = "config" ;
    private final String DOMsystem ="http://www.matsim.org/files/dtd"; 
    private final String DOMpublicIdNet = "/network_v1.dtd";
    private final String DOMpublicIdPlans = "/plans_v4.dtd";
    private final String DOMpublicIdConfig = "/config_v1.dtd";
    private final String randomSeed = "4711" ;
    private final String coordinateSystem = "Atlantis" ;
    private final String legMode = "car" ; 
    private final String defaultActType = "w" ; 
    private final String matsimSimulationStartDayTime = "00:00:00" ; 
    private final String actDuration  ="00:00" ;
    private final String matsimOutputDirectoryName = "MatsimOutput";
    private String matsimOutputFileName = "output_events.xml.gz";

    private String MatsimInputFileLocation ; 
    private TransportationInputType inputType ; 
    private HashMap<String,Double> linkFlowValues ; 
    private HashMap<String,ArrayList<String>> agentPlans ; 

    
    public MatsimFlowDomainAgent(){
        super();
        // Is there a better way to force initializing the FlowNetworkDataTypes class?
        this.setFlowNetworkDataTypes(new TransportationFlowNetworkDataTypes()); 
        this.converged = false;
        logger.debug("initializing Matsim backend");
}
    @Override
    public void setFlowParameters(FlowNetwork flowNetwork){
        flowNetwork.setLinkFlowType(TransportationLinkState.VEHICLESPERHOUR);
        flowNetwork.setLinkCapacityType(TransportationLinkState.CAPACITY);
        flowNetwork.setNodeCapacityType(TransportationNodeState.AGENTNUMBER);
    }
    
    @Override
    public void extractDomainParameters(){
        this.inputType = (TransportationInputType)getDomainParameters().get(TransportationBackendParameter.INPUTTYPE);
        if(this.inputType == TransportationInputType.MATSIM){
            this.MatsimInputFileLocation = (String)getDomainParameters().get(TransportationBackendParameter.INPUTLOCATION);
        }
        
    }
    
    @Override
    public void loadDomainParameters(String backendParamLocation){
        TransportationBackendParameterLoader backendParameterLoader = new TransportationBackendParameterLoader(this.getParameterColumnSeparator());
        this.setDomainParameters(backendParameterLoader.loadBackendParameters(backendParamLocation));
    }   
 
     @Override
    public boolean flowAnalysis(FlowNetwork net){
        
        setXmlFileLocation(getRunningDir()) ; 
        this.SfinaNet = net;
        this.parseAgentsFromNodes(net);
        logger.debug("Agent plans: "+this.agentPlans);
        try{
            parseNetworkTopologyXml(SfinaNet);
            parseFlowPlansXml();
            parseConfigXml();
        }
        catch(ParserConfigurationException | TransformerException excep){
            System.err.println(excep.getClass().getName()+"exception thrown in MatsimFlowBackend >> flowAnalysis: "
                              +excep.getMessage()); 
        }
        Config config = ConfigUtils.createConfig();
        config.addCoreModules();
        ConfigUtils.loadConfig(config,xmlFolderLocation+xmlFolderName+"/"+xmlConfigFilename); 
        Scenario scenario = ScenarioUtils.loadScenario(config) ;
        Controler controler = new Controler(scenario) ;
        this.matsimControler = controler ;
        logger.debug("Matsim initialized");
        matsimControler.run();
        
        try {
            getMatsimResults();
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            java.util.logging.Logger.getLogger(MatsimFlowDomainAgent.class.getName()).log(Level.SEVERE, null, ex);
        } 
        clearFileFolder(this.xmlFolderLocation+this.xmlFolderName);
        clearFileFolder(this.xmlFolderLocation+this.matsimOutputDirectoryName) ;
        
        return true;
    }
    
    // this has to go into the listener method
    private void getMatsimResults() throws ParserConfigurationException, 
                                           SAXException,
                                           IOException
    {    
        //Have to test what kind of results are used here..
        EventOutputLoader matsimOutputLoader = new EventOutputLoader(this.xmlFolderLocation+this.matsimOutputDirectoryName+"\\"+this.matsimOutputFileName,
                                                                     this.SfinaNet);
        matsimOutputLoader.loadXml();
        this.linkFlowValues = matsimOutputLoader.getLinkFlowValues() ;
        for(Link currentLink : this.SfinaNet.getLinks()){
            currentLink.replacePropertyElement(TransportationLinkState.VEHICLESPERHOUR,
                                               this.linkFlowValues.get(currentLink.getIndex()));
        }
    } 
       
    
    private String getRunningDir(){
        
        String runningDir = MatsimFlowDomainAgent.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        runningDir = runningDir.replaceAll("%20", " ") ; 
        runningDir = runningDir.substring(1) ; 
     //   runningDir = runningDir.replaceAll("/", "/") ;
        return runningDir ; 
    }
    
    
    //Create xml file directory if not present and set xml folder location of *this
    private void setXmlFileLocation(String location){
        this.xmlFolderLocation = location ;
        File xmlFolder = new File(location+this.xmlFolderName) ; 
        if(!xmlFolder.exists()){
            xmlFolder.mkdir(); 
        }
       
    }
    
    private void clearFileFolder(String folderPath){
        
        File xmlFolder = new File(folderPath) ; 
        
         if(xmlFolder.list().length != 0) {
            for(String s : xmlFolder.list()){
                File currentFile = new File(folderPath+"\\"+s) ;
                
                if(currentFile.isDirectory()){
                    clearFileFolder(currentFile.getAbsolutePath()); 
                    boolean isDeleted = currentFile.delete() ; 
                    if(!isDeleted){
                        logger.debug("Temporary file: "+s+" in: "+folderPath+", was not deleted!") ; 
                    }
                }
                else
                {
                    boolean isDeleted = currentFile.delete() ; 
                    if(!isDeleted){
                        logger.debug("Temporary file: "+s+" in: "+folderPath+", was not deleted!") ; 
                    }
                }
                
            }
            xmlFolder.delete();
        }
    }
    
    private void parseAgentsFromNodes(FlowNetwork net){
        
        //the backend parameter corresponding to the type of input data should go here as an if condition
        //If Matsim scenarios are used, import the plans file directly ...
        
        //####################################################################
        //## Method to translate agent ids in node array to node ids in agent array
        //#####################################################################
        HashMap agentPlans = new HashMap<String,ArrayList<String>>() ;
        
        for(Node currentNode : net.getNodes()){
            ArrayList<String> agentIds = (ArrayList<String>)currentNode.getProperty(TransportationNodeState.AGENTS);
            if(agentIds == null){continue; }
            for(String currentAgent : agentIds){
                if(agentPlans.containsKey(currentAgent)){
                    ArrayList<String> nodeIds =(ArrayList<String>)agentPlans.get(currentAgent) ;
                    nodeIds.add(currentNode.getIndex()) ; 
                    agentPlans.put(currentAgent, nodeIds) ;
                }
                else{
                    ArrayList<String> newNodeList = new ArrayList<String>();
                    newNodeList.add(currentNode.getIndex());
                    agentPlans.put(currentAgent, newNodeList);
                }
            }
        }
        
        HashMap<String,ArrayList<TreeNode<String>>>  possibleAgentPlans  = computePossibleAgentPlans(agentPlans) ; //all possible plans stored in this hash map
        OptimizationVariables output = computeFinalPlans(possibleAgentPlans) ;                                    //get the results  
        HashMap<String,ArrayList<String>> finalPlans = output.getPlans() ;                                       //get final agent plans 
        logger.debug("Optimization of agent plans finished with error value: "+output.getSum()+" .");           //print the optimization quality 
        this.agentPlans = finalPlans ;                                                                          //set final plans
    }
    
    private HashMap<String,ArrayList<TreeNode<String>>> computePossibleAgentPlans(HashMap<String,ArrayList<String>> unsortedPlans){
        
        HashMap<String,ArrayList<TreeNode<String>>> possibleAgentPlans = new HashMap<String,ArrayList<TreeNode<String>>>(); //initialize storage for all possible agent plans 
        
        for(String agentId : unsortedPlans.keySet()){                                       //iterate throgh all agents 
            ArrayList<String> planNodes = unsortedPlans.get(agentId) ;                      //get unsorted plan of current agent
            possibleAgentPlans.putIfAbsent(agentId, new ArrayList<TreeNode<String>>());     //if not present, create new entry in possibleAgentPlans for this agent
            
            for(String NodeId : planNodes){                                                 //Iterate through all nodes in the current unsorted Plan
                Node currentNode = this.SfinaNet.getNode(NodeId);                           //extract current Node
                String type = (String)currentNode.getProperty(TransportationNodeState.TYPE) ; //get the node property
                if(type == null){continue;}
                if(type.equals("h")){                                                        //if type is a starting Node type --> Do:
                    TreeNode<String> startingLink = new TreeNode<String>(NodeId);                           
                    ArrayList<TreeNode<String>> currentAgentStartingLink = possibleAgentPlans.get(agentId);    //Add the starting node to the starting Node list of current Agent  
                    if(currentAgentStartingLink.contains(startingLink)){ //in case the current node is contained in agent route more than once and it is a starting Node, 
                        continue;                                         //do not create two identical trees for it! 
                    }
                    ArrayList<String> iterablePlanNodes = (ArrayList<String>)planNodes.clone();
                    iterablePlanNodes.remove(NodeId) ;                 //the current Node was already used in the current route (it's a starting Node!). So remove it from the copied list! 
                    createTree(startingLink,planNodes.size()-1,iterablePlanNodes) ; //create a route Tree for the current starting node of this agent
                    ArrayList<TreeNode<String>> newLeafs = startingLink.getLeafs() ; 
                    for(TreeNode<String> currentLeaf : newLeafs){
                        int branchSize = currentLeaf.getLength() ; 
                        if(branchSize == planNodes.size()){
                            currentAgentStartingLink.add(startingLink) ; //add the starting node to the ArrayList of current Agent
                        }
                        else{currentLeaf.removeBranch();}
                    }
                    possibleAgentPlans.put(agentId, currentAgentStartingLink) ;    //now put the complete Plan into possibleAgentPlans map
               
                }    
            }
        }
        return possibleAgentPlans ; 
}
    
private OptimizationVariables computeFinalPlans(HashMap<String, ArrayList<TreeNode<String>>> possibleAgentPlans) {
        
        HashMap<String,ArrayList<String>> agentPlans = new HashMap<String,ArrayList<String>>(); 
        HashMap<String,Integer> arrivingNumbers = new HashMap<String,Integer>();
        HashMap<String,Integer> departingNumbers = new HashMap<String,Integer>();
        for(Node node : this.SfinaNet.getNodes()){//Determine arriving and departing numbers of nodes
            if(node.getProperty(TransportationNodeState.TYPE)==null){continue;}
            switch((String)node.getProperty(TransportationNodeState.TYPE)){
                case "w":
                    arrivingNumbers.put(node.getIndex(), (Integer)node.getProperty(TransportationNodeState.AGENTNUMBER));
                    break;
                case "h":
                    departingNumbers.put(node.getIndex(), (Integer)node.getProperty(TransportationNodeState.AGENTNUMBER));
                    break;
                default:
            }
        }
        //Build the complete Trees 
        ArrayList<TreeNode<String>> singleAgentTrees = new ArrayList<TreeNode<String>>(); //model agents as tree roots and add starting nodes of the respective agent as children
        for(String currentAgent : possibleAgentPlans.keySet()){                           //iterate through agents 
            TreeNode<String> agent = new TreeNode<String>(currentAgent);                  //initialize new agent Node
            for(TreeNode currentStartingLink : possibleAgentPlans.get(currentAgent)){     //iterate through all starting nodes of current Agent
                agent.addChild(currentStartingLink);                                      //add every starting node as child to agent node
            }                                                                                                   
            singleAgentTrees.add(agent);                                                  //Now add agent to AgentTree list. Now each agent corresponds to exactly one tree
        }
        //Determination of single routes for each agent.
        boolean converged = false ;                                                       //Set iteration variables
        int iter = 0 ;                                                                      
        int maxIter = 1000 ;                                                              //Max iterartions allowed
        int iterSum = -1;                                                                 //measure of iteration 
        //HashMap<String,Integer> arrIterValues = arrivingNumbers ; 
        //HashMap<String,Integer> depIterValues = departingNumbers ;
        HashMap<String,ArrayList<String>> iterPlans = new HashMap<>();                    //Map, storing the current set of plans with best values  
        while(!converged && iter < maxIter){                                              //initialize loop
            HashMap<String,ArrayList<String>> currentPlans = pickPlans(singleAgentTrees,arrivingNumbers,departingNumbers); //Pick from all possible plans one single plan of each agent stochastically 
            int sum = this.getIterationResults(currentPlans, arrivingNumbers, departingNumbers) ;           //compute iteration results of the plans picked
            if(iterSum == -1){iterSum = sum ; iterPlans = currentPlans ; iter++ ;}                          //this is the case for the first iteration 
            if(sum == 0){                                                                                   //this is the case where the result is perfect
                converged = true ;                                                                          // end iteration, as a perfectly matching solution has been found
                iterPlans = currentPlans ;                                                                  // set iteration plans 
                iterSum = sum ;                                                                             //set iteration result
                break;                                                                                      //stop loop
            }
            if(sum < iterSum){                                                                              //this is the case where a better solution was found but it is not optimal
                iterSum = sum ;                                                                                 
                iterPlans = currentPlans;
            }
            iter++; 
        }                                                                                                   //in case the current pick was not better than before, do nothing 
        OptimizationVariables output = new OptimizationVariables(iterSum,iterPlans);                        //initialize output 
        return output ;
    }
    
     private void createTree(TreeNode<String> startingLink, int i, ArrayList<String> nodeIds){//nodeIds does not contain startingLink
         if(i==0){return ;}
         if(i!=0){                                                                             //Condition for stop of recursion
            FlowNetwork net = this.SfinaNet ; 
            int type_w_nodes = 0; 
            for(String nodeId : nodeIds){                                                      //count all nodes of arriving type in nodeIds
                Node currentNode = null ;
                currentNode = net.getNode(nodeId) ;
                if(!currentNode.containsProperty(TransportationNodeState.TYPE)){
                    continue;
                }
                String nodeType = (String)currentNode.getProperty(TransportationNodeState.TYPE);
                if(nodeType!=null && nodeType.equals("w")){ //in order to function properly, a node of type "w" can only be chosen
                    type_w_nodes++ ;                                                           //if this is not the last node in the List and the #"w" > 1 or this is the last node in the list
                }
            }
            boolean foundNextNode = false ;
            for(Link currentLink : net.getNode((String)startingLink.getValue()).getOutgoingLinks()){ //run through all outgoing links of startingLink
                Node endNode = (Node)currentLink.getEndNode() ;                                    //get the end node of the current Link
                String type  = (String)endNode.getProperty(TransportationNodeState.TYPE);    //Get type of node under consideration
                boolean A = i==1 ;//conditions to check 
                boolean B ; 
                if(type!=null){
                     B = type.equals("w"); }
                else{
                     B = false ;     
                        }
                boolean C = type_w_nodes == 1  ;
                if(nodeIds.contains(endNode.getIndex()) && ( (!A&&( !B||!C )) || (A&&B&&C)) ){    //check if its contained in the specified agent route AND check this condition (given by a certain truth table)
                    TreeNode<String> newChild = new TreeNode<String>(startingLink,endNode.getIndex()); //create new TreeNode for adding as child 
                    ArrayList<String> iterableNodeIds = (ArrayList<String>)nodeIds.clone() ;                            //Make copy of nodeIds
                    iterableNodeIds.remove(endNode.getIndex())  ;                            //remove the newChild node such that its not considered for the route anymore (if a node exists in the route twice, only one instance is removed here!
                    //recursion
                    nodeIds.remove(endNode.getIndex());
                    foundNextNode = true ; 
                    createTree(newChild, i-1 , nodeIds);                             //Repeat the same process for the newChild node, until all nodes in nodeIds are placed in the route
                }
            }
            if(!foundNextNode){ 
                startingLink.removeBranch();
            }
        }
    }
    
    private HashMap<String, ArrayList<String>> pickPlans(ArrayList<TreeNode<String>> singleAgentTrees, HashMap<String, Integer> arrIterValues, HashMap<String, Integer> depIterValues) {
        
        HashMap<String,ArrayList<String>> agentPlans = new HashMap<>();                             //initialize hash map for output 
        for(TreeNode currentAgent : singleAgentTrees){                                              //iterate through the agent trees
            
            ArrayList<TreeNode<String>> currentChildren  =currentAgent.getChildren() ;             //get children of current agent node (corresponding to the starting Nodes of the plan
            ArrayList<TreeNode<String>> allChildLeafs = currentAgent.getLeafs()  ;                 //get Leafs of current agent node ( corresponding to the ending Nodes of all plans 
            HashMap<Integer,Double> childrenProb = this.computeRelativeFrequencies(currentChildren, depIterValues);            //Compute relative Probabilities for Starting nodes and end Nodes
            HashMap<Integer,Double> leafProb = new HashMap<>();                             //The leaf probabilities have to be computed for every subtree of the agent tree (conditional probability given a starting Node)
            int m = 0 ;                                                                     //Running variable for leaf Probability Map. Is increased whenever a leaf probability is set. 
                                                                                            //This works since the Leafs of a Tree are stored in the same order than the leafs of every 1st layer subtree
            for(int j=0;j<currentChildren.size();j++){                                      //Iterate through all starting nodes of current Agent
                Double currentChildProb = childrenProb.get(j) ;                             //get the Probability of the current Starting Node
                TreeNode<String> currentChild = (TreeNode<String>)currentChildren.get(j)  ; //get the current starting Node (j-th subtree of current Agent)
                ArrayList<TreeNode<String>> currentLeafs = currentChild.getLeafs()  ;       //get the current starting Nodes leafs 
                HashMap<Integer,Double> currentLeafProb = this.computeRelativeFrequencies(currentLeafs,arrIterValues) ; //get the leaf probabilities of the current leafs (conditional probabilities
                for(int k : currentLeafProb.keySet()){                                      //Now iterate through all leaf probabilities
                    leafProb.put(m, currentLeafProb.get(k)*currentChildProb) ;              //put the current running variable value and the joint plan probability P(leaf|start)*P(start) inside the leaf prob. map  
                    m++ ;                                                                   //every Leaf node is mapped to the index it has in the complete , allChildLeafs Map. 
                    
                }
            }
            double rnd = Math.random() ;                                        //Pick a random number in [0;1] 
            double cumulativeProb = 0.0  ;                                      //set the cumulative probability to 0 
            for(int k : leafProb.keySet()){                                     //iterate through the leaf Probability
                cumulativeProb += leafProb.get(k) ;                             //add current leaf prob to cumulative
                if(rnd <= cumulativeProb){                                      //if the random number is smaller than the cummulative probability, pick the current route
                    agentPlans.putIfAbsent((String)currentAgent.getValue(), createRoute(allChildLeafs.get(k))) ;  
                }
            }
        }
        return agentPlans ; 
    }
    
    private HashMap<Integer,Double> computeRelativeFrequencies(ArrayList<TreeNode<String>> nodeSet, HashMap<String,Integer> indNumbers){
        //Compute the relative frequencies of the Nodes stored in <nodeSet>. The absolute frequencies of each node are given in <indNumbers>
        HashMap<Integer,relativeFrequency> relativeFrequencies = new HashMap<>();       
        HashMap<Integer,Double> probabilities = new HashMap<>(); 
        
        for(int j=0;j<nodeSet.size();j++){
                if(indNumbers.containsKey((String)nodeSet.get(j).getValue())){
                    Integer currentNumber = indNumbers.get((String)nodeSet.get(j).getValue()); 
                    int departingNumber =currentNumber ;
                    relativeFrequency currentFrequency = new relativeFrequency(); 
                    currentFrequency.increaseIndividual(departingNumber);
                    if(relativeFrequencies.isEmpty()){
                        relativeFrequencies.put(j, currentFrequency) ;
                    }
                    else{
                       Iterator relFreqIter = relativeFrequencies.keySet().iterator() ;
                       if(currentFrequency==null || !relFreqIter.hasNext()){
                           System.out.println("NULL!");
                       } 
                       currentFrequency.increaseEnsemble(relativeFrequencies.get((Integer)relFreqIter.next()).getEnsembleSize()); //add ensemble size to current ensemble
                       for(int k : relativeFrequencies.keySet()){
                           relativeFrequencies.get(k).increaseEnsemble(departingNumber);} // increase ensemble size of remaining children
                       relativeFrequencies.put(j, currentFrequency) ;                    //Put new Probability to the Map
                    }//The handling of the Ensemble might be better when implementing as a class...    
                }
        }
        int j=0 ; 
        for(int k: relativeFrequencies.keySet()){
            probabilities.put(j, relativeFrequencies.get(k).getRelativeFrequency()); 
            j++;
        }
        return probabilities ; 
    }
 
   private ArrayList<String> createRoute(TreeNode<String> leaf) {
        //Create an Array with the complete branch corresponding to the leaf <leaf> 
        if(leaf.getRoot()==null){
            System.err.println("Leaf is without root. Single Node was passed in <MatsimFlowBackend.createRoute>");
            return null ; 
        }
        ArrayList<String> route = new ArrayList<>();
        TreeNode<String> currentNode = leaf.getRoot() ; 
        route.add((String)leaf.getValue()) ;
        while(currentNode != null){            //do this while the current Node still has a root 
            route.add((String)currentNode.getValue()) ; 
            currentNode = currentNode.getRoot()  ;
        }
        route.remove(route.size()-1) ;      //remove the last node in the rout(which corresponds to the agent id!) 
        //swap values from end to front
        int k = 0 ; //symmetric varible 
        for(int j=route.size()-1; j>=(route.size())/2;j--){
            String intermediate = route.get(k) ; 
            route.set(k, route.get(j)) ; 
            route.set(j, intermediate) ; 
            k++ ; 
        }
        return route ; 
    }
    
    //calculate the sum of all differences between the required arriving/departing of all nodes and the actual obtained ones from <pickPlans> 
    private int getIterationResults(HashMap<String,ArrayList<String>> agentPlans, HashMap<String,Integer> arrNumbers,HashMap<String,Integer> depNumbers){
        
        int sum = 0 ;
        HashMap<String,Integer> iterArriving = new HashMap<>() ;
        HashMap<String,Integer> iterDeparting = new HashMap<>(); 
        
        for(String currentAgent : agentPlans.keySet()){
            ArrayList<String> currentPlan = agentPlans.get(currentAgent);
            String starting = currentPlan.get(0);
            String ending   = currentPlan.get(currentPlan.size()-1) ; 
            if(iterDeparting.containsKey(starting)){
                iterDeparting.put(starting, iterDeparting.get(starting)+1) ; 
            }else
            {   iterDeparting.put(starting, 1);}
            if(iterArriving.containsKey(ending)){
                iterArriving.put(ending, iterArriving.get(ending)+1) ; 
            }else
            {   iterArriving.put(ending, 1);}
        }
        //arriving
        for(String currentArr : arrNumbers.keySet()){
            if(iterArriving.containsKey(currentArr)){
                sum += abs(arrNumbers.get(currentArr)-iterArriving.get(currentArr));
                iterArriving.remove(currentArr); 
            }
            else{
                sum += arrNumbers.get(currentArr); 
            }
        }
        for(String currentArr : iterArriving.keySet()){
            sum += iterArriving.get(currentArr); 
        }
        //departing
        for(String currentDep : depNumbers.keySet()){
            if(iterDeparting.containsKey(currentDep)){
                sum += abs(depNumbers.get(currentDep)-iterDeparting.get(currentDep));
                iterDeparting.remove(currentDep); 
            }
            else{
                sum += depNumbers.get(currentDep); 
            }
        }
        for(String currentDep : iterDeparting.keySet()){
            sum += iterDeparting.get(currentDep); 
        }
        return sum ; 
    }
    
    
    private void parseNetworkTopologyXml(FlowNetwork net) throws ParserConfigurationException,
                                                                 TransformerConfigurationException, 
                                                                 TransformerException
    {          
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        
        Document doc = docBuilder.newDocument() ;
        Element network = doc.createElement("network");
        Attr netName = doc.createAttribute("name");
        netName.setValue("temporary Network");
        network.setAttributeNode(netName); 
        doc.appendChild(network); 
        
        Element nodes = doc.createElement("nodes");
        network.appendChild(nodes);
        
        
        for(Node current_node : net.getNodes()){
            
            Element node =doc.createElement("node");   
            Attr id = doc.createAttribute("id"); 
            Attr x  = doc.createAttribute("x"); 
            Attr y  = doc.createAttribute("y");
            id.setNodeValue(current_node.getIndex());
            x.setNodeValue("0"); //dummy values. This can be retrieved from from/to and link length info..
            y.setNodeValue("0");
            node.setAttributeNode(id);
            node.setAttributeNode(x);
            node.setAttributeNode(y);
            nodes.appendChild(node) ;
        }
        Element links = doc.createElement("links");
        network.appendChild(links); 
        
        for(Link current_link : net.getLinks()){
            
            Element link = doc.createElement("link") ; 
            Attr id = doc.createAttribute("id") ; 
            Attr from = doc.createAttribute("from") ; 
            Attr to = doc.createAttribute("to") ; 
            Attr len = doc.createAttribute("length") ; 
            Attr cap = doc.createAttribute("capacity") ; 
            Attr freesp = doc.createAttribute("freespeed"); 
            Attr permlns = doc.createAttribute("permlanes"); 
            id.setNodeValue(current_link.getIndex());
            from.setNodeValue(current_link.getStartNode().getIndex());
            to.setNodeValue(current_link.getEndNode().getIndex());
            setAttrIfNotNull(len, (Double)current_link.getProperty(TransportationLinkState.LENGTH));
            setAttrIfNotNull(cap, (Double)current_link.getProperty(TransportationLinkState.CAPACITY));
            setAttrIfNotNull(freesp, (Double)current_link.getProperty(TransportationLinkState.FREESPEED));
            setAttrIfNotNull(permlns,(Double)current_link.getProperty(TransportationLinkState.PERMLANES));
            link.setAttributeNode(id);
            link.setAttributeNode(from);
            link.setAttributeNode(to);
            link.setAttributeNode(len);
            link.setAttributeNode(cap);
            link.setAttributeNode(freesp);
            link.setAttributeNode(permlns);
            links.appendChild(link);
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DocumentType docType = docBuilder.getDOMImplementation().createDocumentType(DOMdoctypeNet,"", DOMsystem+DOMpublicIdNet) ; //DOCTYPE TO BE ADDED!
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(xmlFolderLocation+xmlFolderName+"/"+xmlNetworkFilename));
        transformer.transform(source, result);
    }
    private void setAttrIfNotNull(Attr attr , Object value){ ;
        if(value != null){
            attr.setNodeValue(Double.toString((Double)value));
        }
        else{
            attr.setNodeValue("0");
        }
    }

    private void parseFlowPlansXml() throws ParserConfigurationException,
                                            TransformerConfigurationException,
                                            TransformerException
    {
        
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument() ;
        Element plans = doc.createElement("plans") ;
        doc.appendChild(plans);
        Set<String> agentIds = this.agentPlans.keySet();
        
        for(String currentAgent : agentIds){
            ArrayList<String> currentRoute = this.agentPlans.get(currentAgent) ;
            StringBuilder route = new StringBuilder();
            if(currentRoute.size()<=1){
                logger.debug("agent "+currentAgent+"'s route contains less than 2 elements. Agent discarded!");
                continue;}
            String start = currentRoute.get(0); 
            String end   = currentRoute.get(currentRoute.size()-1); 
            Node startingNode  = this.SfinaNet.getNode(start);
            Node endNode = this.SfinaNet.getNode(end) ; 
            String startingLink = null ;
            String endingLink = null ; 
            ArrayList<String> currentRouteLinks = new ArrayList<String>(); 
            //use this in case node ids are used in agent routes!
            if(startingNode.getIncomingLinks().isEmpty()){
                for(Link link : startingNode.getOutgoingLinks()){
                    if(link.getEndNode().getIndex().equals(currentRoute.get(1))){
                        startingLink = link.getIndex() ; 
                    }
                }
                currentRoute.remove(0) ;
            }
            else{
                startingLink = startingNode.getIncomingLinks().get(0).getIndex() ;
            }
            if(endNode.getOutgoingLinks().isEmpty()){
                for(Link link : endNode.getIncomingLinks()){
                    if(link.getStartNode().getIndex().equals(currentRoute.get(currentRoute.size()-2))){
                        endingLink = link.getIndex() ; 
                    }
                }
                currentRoute.remove(currentRoute.size()-1) ;
            }
            else{
                endingLink = endNode.getOutgoingLinks().get(0).getIndex() ; 
            }
            //this is only used for the route info = link ids , case!
//            for(int j=0; j<currentRoute.size()-1;j++){
//                boolean flag = false ;
//                Node node = SfinaNet.getNode(currentRoute.get(j)) ;
//                if(node.getOutgoingLinks().isEmpty()){
//                    logger.debug("node in route has no outgoing links. Cannot complete agent plan for agent"+currentAgent);
//                }
//                for(Link link : node.getOutgoingLinks()){
//                    if(link.getEndNode().getIndex().equals(currentRoute.get(j+1))){
//                        currentRouteLinks.add(link.getIndex()+" "); 
//                        flag = true ; 
//                        if(j==0){ startingLink = link.getIndex() ; }
//                        if(j==currentRoute.size()-2){ endingLink = link.getIndex() ; }
//                    }
//                }
//                if(!flag){
//                    logger.debug("Route of agent "+currentAgent+" terminated before all nodes were passed. Nodes not connected!");
//                    break; 
//                }
//            }
//            for(String currentLink : currentRouteLinks){
//                route.append(currentLink+" ") ;
            for(String currentNode : currentRoute){
                route.append(currentNode+" ") ;
            }  
            Element person = doc.createElement("person") ; 
            Element plan = doc.createElement("plan");
            Element leg  = doc.createElement("leg");
            Element legRoute = doc.createElement("route") ;
            Attr id = doc.createAttribute("id") ;
            for(int j=0; j<2 ; j++){
                Element act = doc.createElement("act") ;
                if(j==0){
                    Attr type = doc.createAttribute("type") ; 
                    Attr x = doc.createAttribute("x") ; 
                    Attr y = doc.createAttribute("y") ; 
                    Attr actLink = doc.createAttribute("link") ; 
                    Attr duration = doc.createAttribute("dur") ; 
                    Attr mode = doc.createAttribute("mode");
                    actLink.setNodeValue(startingLink);
                    duration.setNodeValue(actDuration);
                    mode.setNodeValue(legMode); 
                    x.setNodeValue("0");
                    y.setNodeValue("0");        
                    type.setNodeValue(defaultActType);
                    act.setAttributeNodeNS(type); 
                    act.setAttributeNodeNS(x);
                    act.setAttributeNodeNS(y);
                    act.setAttributeNodeNS(actLink);
                    act.setAttributeNode(duration) ;
                    leg.setAttributeNodeNS(mode);
                    legRoute.appendChild(doc.createTextNode(route.toString()));
                    leg.appendChild(legRoute);
                    plan.appendChild(act);
                    plan.appendChild(leg);
                }
                else{
                    Attr type = doc.createAttribute("type") ; 
                    Attr x = doc.createAttribute("x") ; 
                    Attr y = doc.createAttribute("y") ; 
                    Attr actLink = doc.createAttribute("link") ; 
                    Attr mode = doc.createAttribute("mode");
                    actLink.setNodeValue(endingLink);
                    mode.setNodeValue(legMode); 
                    x.setNodeValue("0");
                    y.setNodeValue("0");        
                    type.setNodeValue(defaultActType);
                    act.setAttributeNodeNS(type);
                    act.setAttributeNodeNS(x);
                    act.setAttributeNodeNS(y);
                    act.setAttributeNodeNS(actLink);
                    plan.appendChild(act);
                }
            }
            id.setNodeValue(currentAgent) ;
            person.setAttributeNodeNS(id);
            person.appendChild(plan);
            plans.appendChild(person) ;   
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DocumentType docType = docBuilder.getDOMImplementation().createDocumentType(DOMdoctypePlans, "",DOMsystem+DOMpublicIdPlans); //DOCTYPE TO BE ADDED!
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(xmlFolderLocation+xmlFolderName+"/"+xmlPlansFilename));
        transformer.transform(source, result);
    }
    
    private void parseConfigXml()throws ParserConfigurationException,
                                        TransformerConfigurationException, 
                                        TransformerException
    {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument() ;
        Element config = doc.createElement("config");
        
        for(MatsimConfig m : MatsimConfig.values()){
            Attr moduleName = doc.createAttribute("name");
            Element module = doc.createElement("module");
            moduleName.setNodeValue(m.getModuleName());
            module.setAttributeNodeNS(moduleName); 
            
            for (String currentName : m.getParamNames()) {
                Attr paramName = doc.createAttribute("name");
                Attr paramValue = doc.createAttribute("value");
                Element param = doc.createElement("param") ;
                paramName.setNodeValue(currentName);
                switch(paramName.getNodeValue()){
                    case("randomSeed"):
                        paramValue.setNodeValue(randomSeed);
                        break;
                    case("coordinateSystem"): 
                        paramValue.setNodeValue(coordinateSystem);
                        break;
                    case("inputNetworkFile"):
                        paramValue.setNodeValue(xmlFolderLocation+xmlFolderName+"/"+xmlNetworkFilename);
                        break;
                    case("inputPlansFile"):
                        paramValue.setNodeValue(xmlFolderLocation+xmlFolderName+"/"+xmlPlansFilename);
                        break;
                    case("outputDirectory"):
                        paramValue.setNodeValue(xmlFolderLocation+matsimOutputDirectoryName);
                        break;
                    case("firstIteration"):
                        paramValue.setNodeValue("0");
                        break;
                    case("lastIteration"):
                        paramValue.setNodeValue("0");
                        break;
                    case("startTime"):
                        paramValue.setNodeValue(matsimSimulationStartDayTime);
                        break;
                    case("endTime"):
                        paramValue.setNodeValue(matsimSimulationStartDayTime);
                        break;
                    case("snapshotperiod"):
                        paramValue.setNodeValue(matsimSimulationStartDayTime);
                        break;
                    case("activityType_0"):
                        paramValue.setNodeValue(defaultActType);
                        break;
                    case("activityPriority_0"):
                        paramValue.setNodeValue("1");
                        break;
                    case("activityTypicalDuration_0"):
                        paramValue.setNodeValue("00:01:00");
                        break;
                    case("activityMinimalDuration_0"): 
                        paramValue.setNodeValue("00:00:00");
                        break;
                    default:
                        logger.debug("Parameter Name <"+paramName.getNodeValue()+"> for module <"
                                + m.getModuleName()+"> can not be found in MatsimConfig enum type!");      
                        break;
                }
                param.setAttributeNodeNS(paramName);
                param.setAttributeNodeNS(paramValue);
                module.appendChild(param);
            }
            config.appendChild(module);
        }
        doc.appendChild(config) ; 
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DocumentType docType = docBuilder.getDOMImplementation().createDocumentType(DOMdoctypeConfig,"", DOMsystem+DOMpublicIdConfig) ; //DOCTYPE TO BE ADDED!
        transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, docType.getSystemId());
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(xmlFolderLocation+xmlFolderName+"/"+xmlConfigFilename));
        transformer.transform(source, result);
        
    }
    
    //#################################################
    //### HELPER CLASSES AND FUNCTIONS              ###
    //#################################################
    private class relativeFrequency<Double>{
        
        private double numerator; 
        private double denominator ; 
        private double ratio ; 
        
        public relativeFrequency(){
            this.numerator = 0 ; 
            this.denominator = 0 ; 
            this.ratio = this.computeRatio(); 
        }
        
        public relativeFrequency(double numerator,double denominator){
            this.numerator = numerator ; 
            this.denominator = denominator ;
            this.ratio = this.computeRatio(); 
        }
        
        public void increaseIndividual(double value){
            this.numerator += value ;
            this.denominator +=value ; 
            this.ratio = this.computeRatio(); 
        }
        public void increaseEnsemble(double value){
            this.denominator += value ;
            this.ratio = this.computeRatio(); 
        }
        public void reduceEnsemple(double value){
            this.denominator -= value ;
            this.ratio = this.computeRatio(); 
        }
        public void reduceIndividual(double value){
            this.denominator -= value ;
            this.ratio = this.computeRatio(); 
        }
        public double getRelativeFrequency(){
            return this.ratio ;
        }
        public double getEnsembleSize(){
            return this.denominator;
        }
        private double computeRatio(){
            
            if(this.denominator==0 && this.numerator !=0){
                System.err.println("Unexpected relative frequency detected, ensemble value zero with individual value > zero!");  
            }
            if(this.denominator==0 && this.numerator ==0){ 
                return 0;
            }
            return this.numerator/this.denominator ; 
        }
    }
    private class TreeNode<T> {
        
        private T data ;
        private TreeNode<T> root ; 
        private ArrayList<TreeNode<T>> children ; 
        private ArrayList<TreeNode<T>> leafs ; 
        private boolean hasLeafs ; 
        
        public TreeNode(){
            
            this.root= null;
            this.data = null ; 
            this.children = new ArrayList<TreeNode<T>>();
            this.leafs = new ArrayList<TreeNode<T>>();
            this.hasLeafs = false ; 
        }
         public TreeNode(T data){
            
            this.root= null;
            this.data = data ; 
            this.children = new ArrayList<TreeNode<T>>();
            this.leafs = new ArrayList<TreeNode<T>>();
            this.hasLeafs = false ; 

        }
          public TreeNode(TreeNode parent){
            
            this.root= parent;
            this.data = null ; 
            this.children = new ArrayList<TreeNode<T>>();
            parent.addChild(this);
            this.leafs = new ArrayList<TreeNode<T>>();
            this.hasLeafs = false ; 

        }
        public TreeNode(TreeNode parent, T data){
            
            this.root= parent;
            this.data = data ; 
            this.children = new ArrayList<TreeNode<T>>();
            this.leafs = new ArrayList<TreeNode<T>>();
            parent.addChild(this);
            this.hasLeafs = false ;
           }
           
        private void setRoot(TreeNode root){
            this.root = root ; 
        }   
           
        public void addChild(TreeNode child){
            ArrayList<TreeNode<T>> currentChildren = this.children ; 
            currentChildren.add(child); 
            this.children = currentChildren ;
            child.setRoot(this) ;
            
        }   
        public void removeChild(int index){
           ArrayList<TreeNode<T>> currentChildren = this.children ;  
           if(currentChildren.isEmpty() || currentChildren==null){return;}
           TreeNode<T> child = currentChildren.get(index) ; 
           child.root = null;  
           currentChildren.remove(index) ;
           this.children = currentChildren ; 
        }
        
        public void removeRoot(){
            if(this.root == null){return ;}
            TreeNode<T> rootNode = this.root;
            ArrayList<TreeNode<T>> children = rootNode.getChildren() ; 
            children.remove(children.indexOf(this)); 
            rootNode.children = children ; 
            this.root = null ; 
        }
        
        public TreeNode<T> getRoot(){
            return this.root ; 
        }
        
        public Object getValue(){
            return this.data ; 
        }
        public ArrayList<TreeNode<T>> getChildren(){
            return this.children ; 
        }
        public TreeNode<T> getChild(int index){
            return this.children.get(index) ;
        }
        
        public TreeNode<T> getTreeRoot(TreeNode node){
            if(node.getRoot() == null){
                return node ; 
            }
            else{
                return getTreeRoot(node.getRoot());
            }
        }
        public ArrayList<TreeNode<T>> getLeafs(){
            if(this.hasLeafs){return this.leafs;}
            else{
            this.computeLeafs();
            this.hasLeafs = true ; 
            return this.leafs ;
            }
        }
        
        private boolean computeLeafs(){
            if(this.getChildren().isEmpty()){
                return false;}
            else{
                for(int k=0;k<this.getChildren().size();k++){
                    computeLeafs(this,this.getChild(k));}
                return true ; }
        }

        private void computeLeafs(TreeNode root,TreeNode node) {
           if(root.getValue().equals("2")&& root.root!=null &&root.getRoot().getValue().equals("1")){
               //System.out.println("JUMP!");
           }
           if(node.getChildren().isEmpty()){
               root.addLeaf(node);
           }
           else{
               for(int j=0;j<node.getChildren().size();j++){ 
                   computeLeafs(root,node.getChild(j));}
           }
        }

        private void addLeaf(TreeNode node) {
            ArrayList<TreeNode<T>> currentLeafs = this.leafs ; 
            currentLeafs.add(node); 
            this.leafs = currentLeafs ;
        }
        
        private void removeBranch(){            //removes the branch of whih *this is the leaf.
            if(this.root == null){return;}
            if(this.root.getChildren().size()==1){
                root.removeBranch();
                root.removeChild(0);
            }
            else{
               if(root==null){return;}
               int ind = this.root.getChildren().indexOf(this) ;
               this.root.removeChild(ind); 
               
            }
        }
        public int getLength(){
            if(this.root==null){return 1;}
            int length = 1 ; 
            TreeNode<T> node = this.root ; 
            while(node != null){
                length+=1; 
                node = node.getRoot() ;
            }
            return length ; 
        }
        
    }
    private class OptimizationVariables{
        
        HashMap<String,ArrayList<String>> plans ; 
        int sum ; 
        
        public OptimizationVariables(){
            this.sum = 0 ; 
            this.plans = new HashMap<>();
        }
        public OptimizationVariables(int sum, HashMap<String,ArrayList<String>> plans ){
            this.sum = sum ; 
            this.plans = plans ; 
        }
        public void setPlans(HashMap<String,ArrayList<String>> plans){
           this.plans = plans ; 
        }
        public void setSum(int sum){
           this.sum = sum  ;
        }
        
        public HashMap<String,ArrayList<String>> getPlans(){
            return this.plans ; 
        }
        public double getSum(){ 
            return this.sum ; 
        }
        
    }
    
}
