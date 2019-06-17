/*
 * Copyright 2017 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package demetra.modelling.regression;

import demetra.timeseries.TsDomain;
import demetra.timeseries.TsPeriod;
import org.junit.Test;
import static org.junit.Assert.*;
import jdplus.maths.matrices.FastMatrix;

/**
 *
 * @author Jean Palate
 */
public class TrigonometricVariablesTest {
    
    public TrigonometricVariablesTest() {
    }

    @Test
    public void testMonthly() {
        TrigonometricVariables vars = TrigonometricVariables.regular(12);
        TsDomain domain = TsDomain.of(TsPeriod.monthly(2017, 8), 180);
        FastMatrix M = Regression.matrix(domain, vars);
        //System.out.println(M);
        assertTrue(M.columnList().stream().allMatch(col->Math.abs(col.sum())<1e-6));
    }
    
}