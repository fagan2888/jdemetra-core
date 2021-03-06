/*
* Copyright 2013 National Bank of Belgium
*
* Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
* by the European Commission - subsequent versions of the EUPL (the "Licence");
* You may not use this work except in compliance with the Licence.
* You may obtain a copy of the Licence at:
*
* http://ec.europa.eu/idabc/eupl
*
* Unless required by applicable law or agreed to in writing, software 
* distributed under the Licence is distributed on an "AS IS" basis,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the Licence for the specific language governing permissions and 
* limitations under the Licence.
*/

package ec.satoolkit.x11;

import ec.tstoolkit.design.Development;
import ec.tstoolkit.design.IntValue;

/**
 *
 * @author Frank Osaer, Jean Palate
 */
@Development(status = Development.Status.Alpha)
public enum X11Step implements IntValue {

    /**
     * 
     */
    A,
    /**
     *
     */
    B,
    /**
     * 
     */
    C,
    /**
     *
     */
    D,
    /**
     * 
     */
    E,
    /**
     *
     */
    F;

    /**
     * Returns the value of this X11Step as an int.
     * @return
     */
    @Override
    public int intValue()
    {
	if (this == A)
	    return -1;
	else if (this == B)
	    return 0;
	else if (this == C)
	    return 1;
	else if (this == D)
	    return 2;
	else if (this == E)
	    return 3;
	else
	    return 4;

    }

};
