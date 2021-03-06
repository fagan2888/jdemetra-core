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

package ec.tstoolkit.algorithm;

import ec.tstoolkit.design.Development;
import java.util.List;

/**
 * Generic description of a group of diagnostics
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public interface IDiagnostics {

    /**
     * Gets the name of the group of diagnostics
     * @return 
     */
    String getName();

    /**
     * Get the list of the tests
     * @return A non empty list of tests.
     */
    List<String> getTests();

    /**
     * Gets the quality of a specified diagnostic
     * @param test
     * @return 
     */
    ProcQuality getDiagnostic(String test);

    /**
     * Gets the value of the diagnostic, if any.
     * Double.Nana is returned if no value are available
     * @param test
     * @return 
     */
    double getValue(String test);

    /**
     * Gets all the warnings related to the given tests.
     * @return The list of warnings or null.
     */
    List<String> getWarnings();
}
