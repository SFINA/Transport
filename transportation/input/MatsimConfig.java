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

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Marius Bild
 */
public enum MatsimConfig{
    
    /**
     *
     */
    GLOBAL("global", new String[] {"randomSeed","coordinateSystem"}),
    NETWORK ("network",new String[] {"inputNetworkFile"}),
    PLANS("plans", new String[] {"inputPlansFile"}),  
    CONTROLER("controler",new String[] {"outputDirectory","firstIteration","lastIteration"}),
    QSIM("qsim",new String[] {"startTime","endTime","snapshotperiod"}),
    PLANCALCSCORE("planCalcScore",new String[]{"activityType_0","activityPriority_0",
                    "activityTypicalDuration_0","activityMinimalDuration_0"})
    ;
    
    private final String moduleName ; //module name
    private final String[] paramNames ; //parameter names of the corresponding module

    MatsimConfig(String name, String[] paramNames) {
        this.moduleName = name ;  
        this.paramNames = paramNames ; 
    }
    
    public final String getModuleName(){
        return moduleName ; 
    }
    
    public final String[] getParamNames(){
        return paramNames ; 
    }
    
    
}
