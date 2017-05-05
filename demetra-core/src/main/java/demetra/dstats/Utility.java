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
package demetra.dstats;

import demetra.design.Development;

/**
 * 
 * @author Frank Osaer, Jean Palate
 */
@Development(status = Development.Status.Alpha)
public final class Utility {

    /**
     *
     */
    public interface calcProbDelegate {

        /**
         *
         * @param x
         * @return
         */
        double calcProb(double x);
    }
    private static final double[] m_a = new double[]{2.2352520354606839287,
        1.6102823106855587881e02, 1.0676894854603709582e03,
        1.8154981253343561249e04, 6.5682337918207449113e-2};
    private static final double[] m_b = new double[]{
        4.7202581904688241870e01, 9.7609855173777669322e02,
        1.0260932208618978205e04, 4.5507789335026729956e04};

    ;
    private static final double[] m_c = new double[]{
        3.9894151208813466764e-1, 8.8831497943883759412,
        9.3506656132177855979e01, 5.9727027639480026226e02,
        2.4945375852903726711e03, 6.8481904505362823326e03,
        1.1602651437647350124e04, 9.8427148383839780218e03,
        1.0765576773720192317e-8};
    private static final double[] m_d = new double[]{
        2.2266688044328115691e01, 2.3538790178262499861e02,
        1.5193775994075548050e03, 6.4855582982667607550e03,
        1.8615571640885098091e04, 3.4900952721145977266e04,
        3.8912003286093271411e04, 1.9685429676859990727e04};
    private static final double[] m_p = new double[]{2.1589853405795699e-1,
        1.274011611602473639e-1, 2.2235277870649807e-2,
        1.421619193227893466e-3, 2.9112874951168792e-5,
        2.307344176494017303e-2};
    private static final double[] m_q = new double[]{1.28426009614491121,
        4.68238212480865118e-1, 6.59881378689285515e-2,
        3.78239633202758244e-3, 7.29751555083966205e-5};
    private static final double[] s_xnum = new double[]{-0.322232431088,
        -1.000000000000, -0.342242088547, -0.204231210245e-1,
        -0.453642210148e-4};
    private static final double[] s_xden = new double[]{0.993484626060e-1,
        0.588581570495, 0.531103462366, 0.103537752850, 0.38560700634e-2};
    private final static int m_maxit = 200;

    static double calcPoly(final double[] coeff, final double val) {
        double term = coeff[coeff.length - 1];
        for (int i = coeff.length - 2; i >= 0; i--) {
            term = coeff[i] + term * val;
        }

        return term;
    }

    static double calcPoly(final double[] coeff, final int leng,
            final double val) {
        double term = coeff[leng - 1];
        for (int i = leng - 2; i >= 0; i--) {
            term = coeff[i] + term * val;
        }

        return term;
    }

    static double intProbability(final double x, final ProbabilityType pt) {
        double res = 0.0;
        double y = Math.abs(x);
        if (y <= NumConstants.THRSH) {
            double xsq = 0.0;
            if (y > NumConstants.EPS) {
                xsq = x * x;
            }
            double xnum = m_a[4] * xsq;
            double xden = xsq;
            for (int i = 0; i < 3; i++) {
                xnum = (xnum + m_a[i]) * xsq;
                xden = (xden + m_b[i]) * xsq;
            }
            res = (x * (xnum + m_a[3]) / (xden + m_b[3])) + 0.5;
        } else if (y <= NumConstants.ROOT32) {
            double xnum = m_c[8] * y;
            double xden = y;
            for (int i = 0; i < 7; i++) {
                xnum = (xnum + m_c[i]) * y;
                xden = (xden + m_d[i]) * y;
            }

            double result = (xnum + m_c[7]) / (xden + m_d[7]);
            double xsq = Math.floor(y * 1.6) / 1.6;
            double del = (y - xsq) * (y + xsq);
            result = Math.exp(-xsq * xsq * 0.5) * Math.exp(-del * 0.5) * result;
            if (x > 0.0) {
                res = 1.0 - result;
            } else {
                res = result;
            }
        } else {
            double result = 0.0;
            double xsq = 1.0 / (x * x);
            double xnum = m_p[5] * xsq;
            double xden = xsq;
            for (int i = 0; i < 4; i++) {
                xnum = (xnum + m_p[i]) * xsq;
                xden = (xden + m_q[i]) * xsq;
            }

            result = xsq * (xnum + m_p[4]) / (xden + m_q[4]);
            result = (NumConstants.SQRPI - result) / y;
            xsq = Math.floor(x * 1.6) / 1.6;
            double del = (x - xsq) * (x + xsq);
            result = Math.exp(-xsq * xsq * 0.5) * Math.exp(-del * 0.5) * result;
            if (x > 0.0) {
                res = 1.0 - result;
            } else {
                res = result;
            }
        }

        if (pt == ProbabilityType.Upper) {
            res = 1 - res;
        }
        return res;
    }

    static double intProbabilityInverse(final double p,
            final calcProbDelegate cb) {
        double eps = 1.0e-13;
        double pp = 1.0 - p;

        if (p <= 0.5) {
            pp = p;
        }

        // initialize
        double strtx = stnVal(pp);
        double xcur = strtx;

        // NEWTON iterations
        for (int i = 0; i < m_maxit; i++) {
            double res = cb.calcProb(xcur);
            double dx = (res - pp)
                    / (NumConstants.SQRPI * Math.exp(-0.5 * xcur * xcur));
            xcur -= dx;
            if (Math.abs(dx / xcur) <= eps) {
                if (p > 0.5) {
                    return -xcur;
                } else {
                    return xcur;
                }
            }
        }

        throw new DStatException(DStatException.ERR_ITER);
    }

    static double stnVal(final double p) {
        double sign = 1.0;
        double z = 1.0 - p;
        if (p <= 0.5) {
            sign = -1.0;
            z = p;
        }

        double y = Math.sqrt(-2.0 * Math.log(z));
        return (y + (calcPoly(s_xnum, y) / calcPoly(s_xden, y))) * sign;
    }

    private Utility() {
    }
}
