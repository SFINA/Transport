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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import network.FlowNetwork;
import network.Link;
import network.Node;
import org.apache.log4j.Logger;
import transportation.input.TransportationNodeState;
import transportation.input.TransportationLinkState; 
import transportation.output.TransportationFlowWriter;

/**
 *
 * @author Marius Bild
 */
public class TransportationFlowWriter {
    
    private FlowNetwork net;
    private String columnSeparator;
    private String missingValue;
    private static final Logger logger = Logger.getLogger(TransportationFlowWriter.class);
    
    public TransportationFlowWriter(FlowNetwork net, String columnSeparator, String missingValue){
        this.net=net;
        this.columnSeparator=columnSeparator;        
        this.missingValue = missingValue;
    }
    
    public void writeLinkFlowData(String location){
        try{
            File file = new File(location);
            File parent = file.getParentFile();
            if(!parent.exists() && !parent.mkdirs())
                logger.debug("Couldn't create output folder");
            file.createNewFile();
            PrintWriter writer = new PrintWriter(new FileWriter(file,false));
            writer.print("id");
            ArrayList<TransportationLinkState> necessaryStates = new ArrayList<TransportationLinkState>();
            ArrayList<String> stateStrings = new ArrayList<String>();
            for (TransportationLinkState state : TransportationLinkState.values()){
                String stateString = lookupTransportationLinkState(state);
                if (stateString != null){
                    necessaryStates.add(state);
                    stateStrings.add(stateString);
                }
            }
            
            for (int i=0; i<stateStrings.size(); i++)
                writer.print(columnSeparator + stateStrings.get(i));
            writer.print("\n");
            
            for (Link link : net.getLinks()){
                writer.print(link.getIndex());
                for (int i=0; i<necessaryStates.size(); i++)
                    writer.print(columnSeparator + link.getProperty(necessaryStates.get(i)));
                writer.print("\n");
            }
            writer.close();   
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }    
    
    public void writeNodeFlowData(String location){
        try{
            File file = new File(location);
            File parent = file.getParentFile();
            if(!parent.exists() && !parent.mkdirs())
                logger.debug("Couldn't create output folder");
            file.createNewFile();
            PrintWriter writer = new PrintWriter(new FileWriter(file,false));
            writer.print("id");
            ArrayList<TransportationNodeState> necessaryStates = new ArrayList<TransportationNodeState>();
            ArrayList<String> stateStrings = new ArrayList<String>();
            for (TransportationNodeState state : TransportationNodeState.values()){
                String stateString = lookupTransportationNodeState(state);
                if (stateString != null){
                    necessaryStates.add(state);
                    stateStrings.add(stateString);
                }
            }
            
            for (int i=0; i<stateStrings.size(); i++)
                writer.print(columnSeparator + stateStrings.get(i));
            writer.print("\n");
            
            for (Node node : net.getNodes()){
                writer.print(node.getIndex());
                for (int i=0; i<necessaryStates.size(); i++)
                    writer.print(columnSeparator + getActualNodeValue(necessaryStates.get(i),node));
                writer.print("\n");
            }
            writer.close();   
        }
        catch(IOException ex){
            ex.printStackTrace();
        }
    }
    private String lookupTransportationLinkState(TransportationLinkState towerLinkState){
        switch(towerLinkState){
            case ID:
                return "id";
            case CAPACITY:
                return "capacity";
            case VEHICLESPERHOUR:
                return "vehiclesperhour";
            case PERMLANES:
                return "permLanes";
            case LENGTH:
                return "length";
            case FREESPEED:
                return "freeSpeed"; 
            default:
                logger.debug("Power link state is not recognized.");
                return null;
        }
    }
    
    private String lookupTransportationNodeState(TransportationNodeState transportationNodeState){
        switch(transportationNodeState){
            case ID: 
                return "id";
            case XCOORD:
                return "xcoord";
            case YCOORD:
                return "ycoord";
            case AGENTS: 
                return "agents";
            case AGENTNUMBER: 
                return "agentnumber"; 
            case TYPE:
                return "type" ; 
            default:
                logger.debug("Power node state is not recognized.");
                return null;
        }
    }
    
    private String getActualNodeValue(TransportationNodeState transportationNodeState, Node node){
        switch(transportationNodeState){
            case AGENTS:
                StringBuilder agents = new StringBuilder();
                for(String currentNode :(ArrayList<String>)node.getProperty(transportationNodeState)){
                    agents.append(currentNode) ; 
                    agents.append(" "); 
                }
                return agents.toString() ;     
        }
        if(node.getProperty(transportationNodeState) == null)
            return missingValue;
        
        return String.valueOf(node.getProperty(transportationNodeState));
    }
    
    
}
