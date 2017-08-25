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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import power.backend.PowerBackendParameter;

/**
 *
 * @author Marius Bild
 */
public class TransportationBackendParameterLoader {
    private String columnSeparator;
    private static final Logger logger = Logger.getLogger(TransportationBackendParameterLoader.class);
    
    public TransportationBackendParameterLoader(String columnSeparator){
        this.columnSeparator=columnSeparator;
    }
    
    public HashMap<Enum,Object> loadBackendParameters(String location){
        HashMap<Enum,Object> backendParameters = new HashMap();
        File file = new File(location);
        Scanner scr = null;
        try {
            scr = new Scanner(file);
            while(scr.hasNext()){
                StringTokenizer st = new StringTokenizer(scr.next(), columnSeparator);
                Enum param = null;
                Object value = null;

                switch(st.nextToken()){
                    case "inputType":
                        param = TransportationBackendParameter.INPUTTYPE;
                        switch(st.nextToken()){
                            case "Sfina":
                                value = TransportationInputType.SFINA ; 
                                break;
                            case "matsim":
                                value = TransportationInputType.MATSIM ; 
                                break;
                            default: 
                                logger.debug("input type either not given or unknown. Sfina input is used!");
                                value = TransportationInputType.SFINA ; 
                                break;
                        }
                        break;
                    case "inputLocation": 
                        param = TransportationBackendParameter.INPUTLOCATION ;
                        value = st.nextToken() ; 
                        System.out.println(value);
                        break;
                    default:
                        logger.debug("Backend parameter type cannot be recognized!");
                        
                }
                backendParameters.put(param, value);
            }
        }
        catch (FileNotFoundException ex){
            ex.printStackTrace();
        }
        return backendParameters;
    }
}