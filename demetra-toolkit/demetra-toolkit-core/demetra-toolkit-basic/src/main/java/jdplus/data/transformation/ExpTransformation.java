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
package jdplus.data.transformation;

import demetra.data.DoubleSeq;
import demetra.design.Development;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class ExpTransformation implements DataTransformation {

    public static final ExpTransformation EXEMPLAR = new ExpTransformation();

    /**
     *
     * @param data
     * @return
     */
    public boolean canTransform(DoubleSeq data) {
        return true;
    }

    /**
     *
     * @return
     */
    @Override
    public DataTransformation converse() {
        return LogTransformation.EXEMPLAR;
    }

    /**
     *
     * @param data
     * @param ljacobian
     * @return
     */
    @Override
    public DoubleSeq transform(DoubleSeq data, LogJacobian ljacobian) {
        double[] x = data.toArray();
        for (int i = 0; i < x.length; ++i) {
            x[i] = Math.exp(x[i]);
        }
        if (ljacobian != null) {
            double s = 0;
            if (ljacobian.missing == null) {
                for (int i = ljacobian.start; i < ljacobian.end; ++i) {
                    if (Double.isFinite(x[i])) {
                        s += x[i];
                    }
                }
            } else {
                int nmissing = ljacobian.missing.length;
                int imissing = 0, ic = ljacobian.start;
                while (imissing < nmissing && ljacobian.missing[imissing] < ic) {
                    ++imissing;
                }
                while (imissing != nmissing && ic < ljacobian.end) {
                    if (ic == ljacobian.missing[imissing]) {
                        ++ic;
                        ++imissing;
                    } else {
                        s += x[ic++];
                    }
                }
                while (ic < ljacobian.end) {
                    s += x[ic++];
                }
            }
            ljacobian.value += s;
        }
        return DoubleSeq.of(x);
    }

    @Override
    public double transform(double x) {
        return Math.exp(x);
    }
}
