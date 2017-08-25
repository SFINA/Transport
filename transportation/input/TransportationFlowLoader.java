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
package transportation.input;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import network.FlowNetwork;
import network.Node;
import network.Link;
import org.apache.log4j.Logger;
/**
 *
 * @author Marius Bild
 */
public class TransportationFlowLoader {

    private final FlowNetwork net;
    private final String parameterValueSeparator;
    private static final Logger logger = Logger.getLogger(TransportationFlowLoader.class);
    private final String missingValue;
    
    public TransportationFlowLoader(FlowNetwork net, String parameterValueSeparator, String missingValue){
        this.net=net;
        this.parameterValueSeparator=parameterValueSeparator;
        this.missingValue=missingValue;
    }
    
    public void loadNodeFlowData(String location){
        ArrayList<Node> nodes = new ArrayList<Node>(net.getNodes());
        ArrayList<TransportationNodeState> TransportationNodeStates = new ArrayList<TransportationNodeState>();
        HashMap<String,ArrayList<String>> nodesStateValues = new HashMap<String,ArrayList<String>>();
        File file = new File(location);
        Scanner scr = null;
        try {
            scr = new Scanner(file);
            if(scr.hasNext()){
                StringTokenizer st = new StringTokenizer(scr.next(), parameterValueSeparator);
                while(st.hasMoreTokens()){
                    String stateName = st.nextToken();
                    TransportationNodeState state = this.lookupTransportationNodeState(stateName);
                    TransportationNodeStates.add(state);
                }
            }
            while(scr.hasNext()){
                ArrayList<String> values=new ArrayList<String>();
                StringTokenizer st = new StringTokenizer(scr.next(), parameterValueSeparator);
                while (st.hasMoreTokens()) {
                    values.add(st.nextToken());
		}
                nodesStateValues.put(values.get(0), values);
            }
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        this.injectNodeStates(nodes, TransportationNodeStates, nodesStateValues);
        
    }
    
    
    public void loadLinkFlowData(String location){
        ArrayList<Link> links = new ArrayList(net.getLinks());
        ArrayList<TransportationLinkState> TransportationLinkStates = new ArrayList<TransportationLinkState>();
        HashMap<String, ArrayList<String>> linksStateValues = new HashMap<String, ArrayList<String>>();
        File file = new File(location);
        Scanner scr = null;
        try {
            scr = new Scanner(file);
            if(scr.hasNext()){
                StringTokenizer st = new StringTokenizer(scr.next(), parameterValueSeparator);
                while(st.hasMoreTokens()){
                    String stateName = st.nextToken();
                    TransportationLinkState state = this.lookupTransportationLinkState(stateName);
                    TransportationLinkStates.add(state);
                }
            }
            while(scr.hasNext()){
                ArrayList<String> values = new ArrayList<String>();
                StringTokenizer st = new StringTokenizer(scr.next(), parameterValueSeparator);
                while (st.hasMoreTokens()) {
                    values.add(st.nextToken());
		}
                linksStateValues.put(values.get(0), values);
            }
        }
        catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        this.injectLinkStates(links, TransportationLinkStates, linksStateValues);
                
    }
    
    
    private TransportationNodeState lookupTransportationNodeState(String transportationNodeState){
        
        switch(transportationNodeState){
            case "id": 
                return TransportationNodeState.ID;
            case "xcoord":
                return TransportationNodeState.XCOORD;
            case "ycoord":
                return TransportationNodeState.YCOORD;
            case "agents":
                return TransportationNodeState.AGENTS;
            case "agentnumber":
                return TransportationNodeState.AGENTNUMBER ;
            case "type":
                return TransportationNodeState.TYPE ; 
            default:
                logger.debug("Transportation node state is not recognized.");
                return null;    
        }
    }
    
    private TransportationLinkState lookupTransportationLinkState(String transportationLinkState){
        switch(transportationLinkState){
            case "id":
                return TransportationLinkState.ID;
            case "capacity":
                return TransportationLinkState.CAPACITY;
            case "vehiclesperhour":
                return TransportationLinkState.VEHICLESPERHOUR;
            case "permLanes":
                return TransportationLinkState.PERMLANES;
            case "length":
                return TransportationLinkState.LENGTH ; 
            case "freeSpeed":
                return TransportationLinkState.FREESPEED; 
            default:
                logger.debug("Transportation link state is not recognized: " + transportationLinkState);
                return null;
        }
    }
        
    private void injectNodeStates(ArrayList<Node> nodes, ArrayList<TransportationNodeState> TransportationNodeStates, HashMap<String,ArrayList<String>> nodesStates){
           for(Node node : nodes){
               ArrayList<String> rawValues = nodesStates.get(node.getIndex());
               for(int i=0;i<rawValues.size();i++){
                   TransportationNodeState state = TransportationNodeStates.get(i);
                   String rawValue = rawValues.get(i);
                   if(!rawValue.equals(this.missingValue) && !state.equals(TransportationNodeState.ID)){
                       node.addProperty(state, this.getActualNodeValue(state, rawValues.get(i)));}
               }
           }
    }
    
    private void injectLinkStates(ArrayList<Link> links, ArrayList<TransportationLinkState> transportationLinkStates, HashMap<String,ArrayList<String>> linksStates){
        for(Link link : links){
            ArrayList<String> rawValues = linksStates.get(link.getIndex());
            for(int i=0;i<rawValues.size();i++){
                TransportationLinkState state = transportationLinkStates.get(i);
                String rawValue = rawValues.get(i);
                if(!rawValue.equals(this.missingValue) && !state.equals(TransportationLinkState.ID))
                    link.addProperty(state, this.getActualLinkValue(state, rawValue));
            }
        }
    }
    
     private Object getActualNodeValue(TransportationNodeState TransportationNodeState, String rawValue){

         switch(TransportationNodeState){
            case ID:
                return rawValue;
            case XCOORD:
                return rawValue ;
            case YCOORD: 
                return rawValue; 
            case AGENTNUMBER: 
                return Integer.parseInt(rawValue); 
            case TYPE: 
                return rawValue; 
            case AGENTS: 
                String[] agentIds = rawValue.split("_");
                ArrayList<String> agents = new ArrayList<String>(); 
                for(String agent : agentIds){
                    agents.add(agent) ;}
                return agents ;
            default:
                logger.debug("Transportation node state is not recognized.");
                return null;    
         }
     }
 
     private Object getActualLinkValue(TransportationLinkState transportationLinkState, String rawValue){
        switch(transportationLinkState){
            case ID:
                return rawValue;
            case CAPACITY:
                return Double.parseDouble(rawValue);
            case VEHICLESPERHOUR:
                return Double.parseDouble(rawValue);
            case LENGTH:
                return Double.parseDouble(rawValue);
            case PERMLANES:
                return Double.parseDouble(rawValue);
            case FREESPEED:
                return Double.parseDouble(rawValue);
            default:
                logger.debug("Transportation link state is not recognized: " + transportationLinkState);
                return null;
        }
     }
}


