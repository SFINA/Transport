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

import agents.backend.FlowNetworkDataTypesInterface;
import java.util.ArrayList;
import network.InterdependentLink;
import network.Link;
import network.LinkInterface;
import network.Node;
import org.apache.log4j.Logger;
import transportation.input.TransportationLinkState;
import transportation.input.TransportationNodeState;

/**
 *
 * @author Marius Bild
 */
public class TransportationFlowNetworkDataTypes implements FlowNetworkDataTypesInterface{
    
    private static final Logger logger = Logger.getLogger(TransportationFlowNetworkDataTypes.class);
    
    public TransportationFlowNetworkDataTypes(){
    }
    
    @Override
    public Enum[] getNodeStates(){
        return TransportationNodeState.values();
    }
    
    @Override
    public Enum[] getLinkStates(){
        return TransportationLinkState.values();
    }
    
    @Override
    public TransportationNodeState parseNodeStateTypeFromString(String transportationNodeState){
        switch(transportationNodeState){
            case "id": 
                return null;
            case "xcoord":
                return TransportationNodeState.XCOORD;
            case "ycoord":
                return TransportationNodeState.YCOORD;
            case "agentnumber":
                return TransportationNodeState.AGENTNUMBER;
            case "agents":
                return TransportationNodeState.AGENTS;
            case "type":
                return TransportationNodeState.TYPE;
            default:
                logger.debug("Transportationnode state is not recognized.");
                return null;
        }
    }
    
    @Override
    public TransportationLinkState parseLinkStateTypeFromString(String transportationLinkState){
        switch(transportationLinkState){
            case "id":
                return null;
            case "capacity":
                return TransportationLinkState.CAPACITY;
            case "freespeed":
                return TransportationLinkState.FREESPEED;
            case "vehiclesperhour":
                return TransportationLinkState.VEHICLESPERHOUR;
            case "length":
                return TransportationLinkState.LENGTH;
            case "permlanes":
                return TransportationLinkState.PERMLANES;
            default:
                logger.debug("Transportation link state is not recognized.");
                return null;
        }
    }
    
    @Override
    public Object parseNodeValuefromString(Enum nodeState, String rawValue){
        TransportationNodeState TransportationNodeState=(TransportationNodeState)nodeState;
        switch(TransportationNodeState){
            case ID:
                return rawValue;
            case XCOORD:
                return rawValue;
            case YCOORD:
                return rawValue;
            case AGENTS:
                String[] agentIds = rawValue.split("_");
                ArrayList<String> agents = new ArrayList<String>(); 
                for(String agent : agentIds){
                    agents.add(agent) ;}
                return agents ;
            case AGENTNUMBER:
                return Integer.parseInt(rawValue);
            case TYPE:
                return rawValue;
            default:
                logger.debug("Transportation node state is not recognized.");
                return null;
        }    
    }
    
     @Override
    public Object parseLinkValueFromString(Enum linkState, String rawValue){
        TransportationLinkState TransportationLinkState=(TransportationLinkState)linkState;
        switch(TransportationLinkState){
            case ID:
                return rawValue;
            case VEHICLESPERHOUR:
                return Double.parseDouble(rawValue);
            case FREESPEED:
                return Double.parseDouble(rawValue);
            case LENGTH:
                return Double.parseDouble(rawValue);
            case PERMLANES:
                return Double.parseDouble(rawValue);
            case CAPACITY:
                return Double.parseDouble(rawValue);
            default:
                logger.debug("Transportation link state is not recognized.");
                return null;
        }
    }
    
    @Override
    public String castNodeStateTypeToString(Enum nodeState){
        TransportationNodeState transportationNodeState = (TransportationNodeState)nodeState;
        switch(transportationNodeState){
            case ID: 
                return null;
            case TYPE:
                return "type";
            case AGENTS:
                return "agents";
            case AGENTNUMBER:
                return "agentnumber";
            case XCOORD:
                return "xcoord";
            case YCOORD:
                return "ycoord";
            default:
                logger.debug("Transportation node state is not recognized.");
                return null;
        }
    }
    
     @Override
    public String castLinkStateTypeToString(Enum linkState){
        TransportationLinkState transportationLinkState = (TransportationLinkState)linkState;
        switch(transportationLinkState){
            case ID:
                return null;
            case LENGTH:
                return "length";
            case PERMLANES:
                return "permlanes";
            case VEHICLESPERHOUR:
                return "vehiclesperhour";
            case CAPACITY:
                return "capacity";
            case FREESPEED:
                return "freespeed";
            default:
                logger.debug("Transportation link state is not recognized.");
                return null;
        }
    }
    
    @Override
    public String castNodeStateValueToString(Enum nodeState, Node node, String missingValue){
        TransportationNodeState transportationNodeState = (TransportationNodeState)nodeState;
        switch(transportationNodeState){
            case AGENTS:
                StringBuilder agents = new StringBuilder();
                ArrayList<String> agentIds = (ArrayList<String>)node.getProperty(transportationNodeState) ; 
                if(agentIds==null){return missingValue;}
                for (String agentId : agentIds) {
                    agents.append(agentId);
                    agents.append("_");
                }
                return agents.toString() ;
        } 
        if(node.getProperty(transportationNodeState)==null){
            return missingValue ; 
        }
        return String.valueOf(node.getProperty(transportationNodeState));
    }
    
    public String castLinkStateValueToString(Enum linkState, LinkInterface link, String missingValue){
                Object value;
        if(link.isInterdependent())
            value = ((InterdependentLink)link).getProperty(linkState);
        else
            value = ((Link)link).getProperty(linkState);
        
        return (value == null ? missingValue : String.valueOf(value));
    }
}
          
