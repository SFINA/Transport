# Transport

#1. Installation:
#1.1 Include all classes in the folder "transportation" to your SFINA project. 
#1.2 From the folder "dist/lib", import all .jar files not already present as libraries into your project.

#2. Creating an experiment: 
#2.1 Create a folder experiment-XX with XX the number of the experiment. Create the necessary folder structure as for any other domain and backend. 
-In the experimentConfig.txt file, set "backendType" to "matsim". 
-In the backendParameters.txt file, set "inputType" to either "Matsim" or "Sfina". "Sfina" means that Sfina specific input is used. "Matsim" tells the backend agent to use a complete Matsim scenario as input. In this case, "inputLocation" in backendParameters has to be specified as the input location of Matsim scenario.

#2.2 Create necessary input data: 
#- Create Network topology input. (generalized input information for all #SFINA domains)
#- Create Network flow input
--> Node flow data: id, xcoord,	ycoord, agentnumber, agents, type
							
id: node id
							
xcoord,ycoord: x coordinate of the node (can be set to zero if no explicit road map is given)
							
agentnumber: number of agents departing/arriving at the node during simulation. If the node is not of type "w" or "h", this information can be spared. 
							
agents: ids of agents crossing the node during simulation. Agent ids are separated by an underline. 
							
type: specifies the type of the node. "w" (inherited from Matsim as "work") labels the node as an arriving node. "h" ("home") labels the node as a departing node. 
						
--> Link flow data: id, capacity, vehiclesperhour, permlanes, length, freespeed

id: link id
							
capacity: maximum vehicle number per hour before traffic breakdown. Unit [vehicle/hour]
							
vehiclesperhour: averaged vehicles per hour value crossing the link over the complete simulation time. 
							
permlanes: number of lanes of the link. 
							
length: link length. Unit [m]
							
freespeed: maximum allowed speed for vehicles on the link. Unit [m/s]
							
#-Note that each node may only be of one type. Hence no agents can depart from a "w" labeled node. 
#-Note that the route of each agent is computed by translating the "agents" link flow quantity to an agent based quantity. Therefore, considering a specific agent, the agent id should be present in a set of connected nodes (i.e. there exists a link between all nodes containing this agent's id in its "agents" quantity). Otherwise this agent will not be processed during bootstrapping.  

#2.3 Create an experiment main class for running your experiment. This can be done analogously to other backends and domains.  