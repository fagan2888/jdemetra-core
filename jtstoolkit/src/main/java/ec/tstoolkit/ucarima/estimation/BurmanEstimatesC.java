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
package ec.tstoolkit.ucarima.estimation;

import ec.tstoolkit.ucarima.WienerKolmogorovEstimators;
import ec.tstoolkit.arima.ArimaException;
import ec.tstoolkit.arima.ArimaModel;
import ec.tstoolkit.arima.IArimaModel;
import ec.tstoolkit.arima.estimation.FastArimaForecasts;
import ec.tstoolkit.data.DataBlock;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.design.Development;
import ec.tstoolkit.maths.linearfilters.BackFilter;
import ec.tstoolkit.maths.linearfilters.SymmetricFilter;
import ec.tstoolkit.maths.matrices.*;
import ec.tstoolkit.maths.polynomials.Polynomial;
import ec.tstoolkit.maths.polynomials.UnitRoots;
import ec.tstoolkit.ucarima.UcarimaModel;
import java.util.Arrays;

/**
 * Estimation of the components of an UCARIMA model using a variant of the
 * Burman's algorithm.</br>This class is based on the program SEATS+ developed
 * by Gianluca Caporello and Agustin Maravall -with programming support from
 * Domingo Perez and Roberto Lopez- at the Bank of Spain, and on the program
 * SEATS, previously developed by Victor Gomez and Agustin Maravall.<br>It
 * corresponds more especially to a modified version of the routine
 * <i>ESTBUR</i>
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Release)
public class BurmanEstimatesC {

    private double[] m_data;
    private int m_nf;
    private WienerKolmogorovEstimators m_wk;
    private Polynomial m_ar, m_ma;
    private AbstractLinearSystemSolver m_solver;
    private Polynomial[] m_g;
    private double m_ser = 1, m_mean;
    private int m_nparams;
    // private int m_p, m_q;, m_r;
    private double[][] m_e, m_f;
    private double[] m_xb, m_xf;
    private boolean m_bmean;

    /**
     * Creates a new instance of WKEstimators
     */
    public BurmanEstimatesC() {
    }

    private void calc(final int cmp) {
        if (m_e[cmp] != null || m_data == null) {
            return;
        }
        extendSeries();
        int n = m_data.length;
        if (cmp == 0 && isTrendConstant()) {
            double[] e = new double[n];
            double m = correctedMean();
            Arrays.fill(e, m);
            m_e[cmp] = e;
            double[] f = new double[m_nf];
            Arrays.fill(f, m);
            m_f[cmp] = f;
            return;
        } else if (m_g[cmp] == null) {
            return;
        }

        int nf = m_xf.length;
        double[] ma = m_ma.getCoefficients();
        double[] ar = m_ar.getCoefficients();

        // qstar is the order of the ma polynomial
        // pstar is the order of the ar polynomial
        int qstar = ma.length - 1;
        int pstar = ar.length - 1;
        if (useD1()) {
            ++qstar;
        }

        // //////////////////////////////////
        // Complete z, the original series
        // z is the extended series with forecasts and backcasts
        double[] z = new double[n + 2 * nf];
        for (int i = 0; i < n; ++i) {
            z[i + nf] = m_data[i];
        }

        int ntmp = nf + n;
        for (int i = 0; i < nf; ++i) {
            z[ntmp + i] = m_xf[i];
        }
        ntmp = nf - 1;
        for (int i = 0; i < nf; ++i) {
            z[ntmp - i] = m_xb[i];
        }
        if (useMean()) {
            double m = correctedMean();
            for (int i = 0; i < z.length; ++i) {
                z[i] -= m;
            }
        }
        // //////////////////////////////////
        // Compute w1(t) = g(F) z(t)
        Polynomial g = m_g[cmp];
        int gstar = g.getDegree();
        double[] w1 = new double[n + qstar];
        for (int i = 0; i < n + qstar; ++i) {
            double s = g.get(0) * z[nf + i];
            for (int j = 1; j <= gstar; ++j) {
                s += g.get(j) * z[nf + i + j];
            }
            w1[i] = s;
        }
        // calculation of x2

        // calculation of w2=g*data, for -q<=t<n+nf , elements -q to n+nf of
        // data w1: elt t=0 at place q
        double[] w2 = new double[n + nf + qstar];
        for (int i = 0; i < n + nf + qstar; ++i) {
            double s = g.get(0) * z[nf - qstar + i];
            for (int j = 1; j <= gstar; ++j) {
                s += g.get(j) * z[nf - qstar + i - j];
            }
            w2[i] = s;
        }

        double[] ww = new double[pstar + qstar];
        ntmp = n + qstar - pstar;
        for (int i = 0; i < pstar; ++i) {
            ww[i] = w1[ntmp + i];
        }

        double[] mx = ww.length == 0 ? new double[0] : m_solver.solve(ww);
        int nx1 = n + Math.max(2 * qstar, nf);
        double[] x1 = new double[nx1];
        for (int i = 0; i < pstar + qstar; ++i) {
            x1[ntmp + i] = mx[i];
            // backward iteration
        }
        for (int i = ntmp - 1; i >= 0; --i) {
            double s = w1[i];
            for (int j = 1; j < ma.length; ++j) {
                s -= x1[i + j] * ma[j];
            }
            x1[i] = s;
        }

        // forward iteration
        for (int i = ntmp + pstar + qstar; i < nx1; ++i) {
            double s = 0;
            for (int j = 1; j <= pstar; ++j) {
                s -= ar[j] * x1[i - j];
            }
            x1[i] = s;
        }

        for (int i = 0; i < pstar; ++i) {
            ww[i] = w2[pstar - i - 1];
        }
        mx = ww.length == 0 ? new double[0] : m_solver.solve(ww);
        int nx2 = n + 2 * qstar + Math.max(nf, 2 * qstar);
        double[] x2 = new double[nx2];
        for (int i = 0; i < pstar + qstar; ++i) {
            x2[pstar + qstar - 1 - i] = mx[i];

            // iteration w2 start in -q, x2 in -2*q delta q
        }
        for (int i = pstar + qstar; i < nx2; ++i) {
            double s = w2[i - qstar];
            for (int j = 1; j < ma.length; ++j) {
                s -= x2[i - j] * ma[j];
            }
            x2[i] = s;
        }

        double[] rslt = new double[n];
        for (int i = 0; i < n; ++i) {
            rslt[i] = (x1[i] + x2[i + 2 * qstar]);
        }
        if (cmp == 0 && useMean()) {
            double m = correctedMean();
            for (int i = 0; i < rslt.length; ++i) {
                rslt[i] += m;
            }
        }
        m_e[cmp] = rslt;

        if (m_nf > 0) {
            double[] fcast = new double[m_nf];

            for (int i = 0; i < m_nf; ++i) {
                fcast[i] = (x1[n + i] + x2[n + i + 2 * qstar]);
            }
            if (cmp == 0 && useMean()) {
                double m = correctedMean();
                for (int i = 0; i < fcast.length; ++i) {
                    fcast[i] += m;
                }
            }
            m_f[cmp] = fcast;
        }
    }

    protected void clearForecasts() {
        m_xb = null;
        m_xf = null;
        if (m_f != null) {
            for (int i = 0; i < m_f.length; ++i) {
                m_f[i] = null;
            }
        }
    }

    /**
     *
     */
    private void clearResults() {
        m_ser = 1;
        m_mean = 0;
        if (m_e != null) {
            for (int i = 0; i < m_e.length; ++i) {
                m_e[i] = null;
            }
        }
    }

    /**
     *
     * @param cmp
     * @param signal
     * @return
     */
    public double[] estimates(final int cmp, final boolean signal) {

        calc(cmp);
        if (signal) {
            return m_e[cmp];
        } else {
            double[] e = new double[m_data.length];
            for (int i = 0; i < e.length; ++i) {
                e[i] = m_data[i] - m_e[cmp][i];
            }
            return e;
        }
    }

    /**
     *
     */
    private void extendSeries() {
        if (m_xb != null) {
            return;
        }
        int nf = 0;
        for (int i = 0; i < m_g.length; ++i) {
            int nr = 0;
            if (m_g[i] != null) {
                nr = m_g[i].getDegree();
            }
            if (m_ma != null) {
                nr += m_ma.getDegree();
            }
            if (m_bmean) {
                nr += 2;
            }
            if (nr > nf) {
                nf = nr;
            }
        }

        if (m_nf > nf) {
            nf = m_nf;
        }
        if (m_bmean && nf <= m_ar.getDegree()) {
            nf = m_ar.getDegree() + 1;
        }

        FastArimaForecasts fcast = new FastArimaForecasts(m_wk.getUcarimaModel().getModel(), m_bmean);
        m_xf = fcast.forecasts(new DataBlock(m_data), nf);
        m_xb = fcast.forecasts(new DataBlock(m_data).reverse(), nf);
        if (m_bmean) {
            m_mean = fcast.getMean();
        } else {
            m_mean = 0;
        }
    }

    private IArimaModel model() {
        return m_wk.getUcarimaModel().getModel();
    }

    // compute mean/P(1), where P is the stationary AR 
    private double correctedMean() {
        IArimaModel arima = model();
        return m_mean / arima.getStationaryAR().getPolynomial().evaluateAt(1);
    }

//    private void extendSeriesOld() {
//	if (m_xb != null)
//	    return;
//	int nf = 0;
//	for (int i = 0; i < m_g.length; ++i) {
//	    int nr = 0;
//	    if (m_g[i] != null)
//		nr = m_g[i].length - 1;
//	    if (m_ma != null)
//		nr += m_ma.length - 1;
//	    if (m_bmean)
//		nr += 2;
//	    if (nr > nf)
//		nf = nr;
//	}
//
//	if (m_nf > nf)
//	    nf = m_nf;
//	int n = m_data.length;
//	double[] xb = new double[n];
//
//	FastArimaML fml = new FastArimaML();
//	SemiInfiniteSampleForecast fcast = new SemiInfiniteSampleForecast();
//
//	IArimaModel model = m_wk.getUcarimaModel().getModel();
//
//	fml.setModel(model);
//	fml.setMeanCorrection(m_bmean);
//	fcast.setModel(model);
//	fcast.setForecastHorizon(nf);
//
//	fml.process(new DataBlock(m_data));
//	m_ser = fml.ser(m_nparams);
//	m_res = fml.getResiduals();
//	fcast.process(m_data, m_res, fml.getDMean(), fml.ser(m_nparams));
//
//	m_xf = fcast.getForecasts();
//	// m_exf = fcast.getForecastsStdev();
//
//	int ntmp = n - 1;
//	for (int i = 0; i <= ntmp; ++i)
//	    xb[ntmp - i] = m_data[i];
//	fml.process(new DataBlock(xb));
//	m_bres = fml.getResiduals();
//	fcast.process(xb, m_bres, fml.getDMean(), fml.ser(m_nparams));
//	m_xb = fcast.getForecasts();
//	// m_exb = fcast.getForecastsStdev();
//    }
    /**
     *
     * @param cmp
     * @param signal
     * @return
     */
    public double[] forecasts(final int cmp, final boolean signal) {
        calc(cmp);
        if (signal) {
            return m_f[cmp];
        } else {
            double[] f = new double[m_nf];
            for (int i = 0; i < f.length; ++i) {
                f[i] = m_xf[i] - m_f[cmp][i];
            }
            return f;
        }
    }

    /**
     *
     * @return
     */
    public double[] getData() {
        return m_data;
    }

    /**
     *
     * @return
     */
    public WienerKolmogorovEstimators getEstimators() {
        return m_wk;
    }

    /**
     *
     * @return
     */
    public int getForecastsCount() {
        return m_nf;
    }

    /**
     *
     * @return
     */
    public int getHyperParametersCount() {
        return m_nparams;
    }

    /**
     *
     * @return
     */
    public double getSer() {
        return m_ser;
    }

    public void setSer(double ser) {
        m_ser = ser;
    }

    /**
     *
     * @return
     */
    public double[] getSeriesBackcasts() {
        extendSeries();
        return m_xb;
    }

    /**
     *
     * @return
     */
    public double[] getSeriesForecasts() {
        extendSeries();
        return m_xf;
    }

    /**
     *
     * @return
     */
    public UcarimaModel getUcarimaModel() {
        return m_wk.getUcarimaModel();
    }

    /**
     *
     */
    private void initModel() {
        UcarimaModel ucm = m_wk.getUcarimaModel();
        // cfr burman-wilson algorithm
        IArimaModel model = ucm.getModel();
        int ncmps = ucm.getComponentsCount();
        m_e = new double[ncmps][];
        m_f = new double[ncmps][];
        m_g = new Polynomial[ncmps];

        m_ma = model.getMA().getPolynomial();
        double v = model.getInnovationVariance();
        if (v != 1) {
            m_ma = m_ma.times(Math.sqrt(v));
        }
        m_ar = model.getAR().getPolynomial();

        for (int i = 0; i < ncmps; ++i) {
            ArimaModel cmp = ucm.getComponent(i);
            if (!cmp.isNull()) {
                SymmetricFilter sma = cmp.sma();
                if (!sma.isNull()) {
                    BackFilter umar = model.getNonStationaryAR(), ucar = cmp.getNonStationaryAR();
                    BackFilter nar = umar.divide(ucar);
                    BackFilter smar = model.getStationaryAR(), scar = cmp.getStationaryAR();
                    BackFilter.SimplifyingTool smp = new BackFilter.SimplifyingTool(true);
                    if (smp.simplify(smar, scar)) {
                        smar = smp.getLeft();
                        scar = smp.getRight();
                    }

                    BackFilter dar = scar;
                    nar = nar.times(smar);

                    BackFilter denom = new BackFilter(m_ma).times(dar);
                    SymmetricFilter c = sma.times(SymmetricFilter.createFromFilter(nar));
                    double mvar = model.getInnovationVariance();
                    if (mvar != 1) {
                        c = c.times(1 / mvar);
                    }
                    BackFilter gf = c.decompose(denom);
                    m_g[i] = gf.getPolynomial();
                } else {
                    m_g[i] = Polynomial.ZERO;
                }
            }
        }
        if (useD1()) {
            m_ar = m_ar.times(UnitRoots.D1);
        }

        initSolver();
    }

    private boolean useD1() {
        // we use D1 correction when there is a mean and UR in the AR part of the model
        return m_bmean && model().getNonStationaryARCount() > 0;
    }

    private boolean useMean() {
        // we use the mean if there is a mean and if we don't use D1 correction
        // it appens when the model doesn't contain non stationary roots
        return m_bmean && model().getNonStationaryARCount() == 0;
    }

    private boolean isTrendConstant() {
        return m_wk.getUcarimaModel().getComponent(0).isNull();
    }

    private void initSolver() {
        Polynomial ma = m_ma;
        Polynomial ar = m_ar;
        int qstar = ma.getDegree();
        int pstar = ar.getDegree();
        if (useD1()) {
            qstar += 1;
        }

        //////////////////////////////////
//         Complete z, the original series
//         z is the extended series with forecasts and backcasts
        Matrix m = new Matrix(pstar + qstar, pstar + qstar);
        for (int i = 0; i < pstar; ++i) {
            for (int j = 0; j <= ma.getDegree(); ++j) {
                m.set(i, i + j, ma.get(j));
            }
        }
        for (int i = 0; i < qstar; ++i) {
            for (int j = 0; j <= pstar; ++j) {
                m.set(i + pstar, i + j, ar.get(pstar - j));
            }
        }

        m_solver = new CroutDoolittle();
        m_solver.decompose(m);
    }

    /**
     *
     * @return
     */
    public boolean isMeanCorrection() {
        return m_bmean;
    }

    /**
     *
     * @param data
     */
    public void setData(final IReadDataBlock data) {
        m_data = new double[data.getLength()];
        data.copyTo(m_data, 0);
        clearResults();
        clearForecasts();
    }

    /**
     *
     * @param value
     */
    public void setEstimators(final WienerKolmogorovEstimators value) {
        m_wk = value;
        initModel();
        clearResults();
        clearForecasts();
    }

    /**
     *
     * @param value
     */
    public void setForecastsCount(final int value) {
        m_nf = value;
        clearForecasts();
    }

    /**
     *
     * @param value
     */
    public void setHyperParametersCount(int value) {
        m_nparams = value;
    }

    /**
     *
     * @param value
     */
    public void setUcarimaModel(final UcarimaModel value) {
        m_wk = new WienerKolmogorovEstimators(value);
        m_bmean = false;
        initModel();
        clearResults();
        clearForecasts();
    }

    /**
     *
     * @param value
     */
    public void setUcarimaModelWithMean(final UcarimaModel value) {
        m_wk = new WienerKolmogorovEstimators(value);
        m_bmean = true;
        initModel();
        clearResults();
        clearForecasts();
    }

    /**
     *
     * @param cmp
     * @return
     */
    public double[] stdevEstimates(final int cmp) {
        calc(cmp);
        if (m_wk.getUcarimaModel().getComponent(cmp).isNull()) {
            return new double[m_data.length];
        } else {
            try {
                int n = (m_data.length + 1) / 2;
                double[] err = m_wk.totalErrorVariance(cmp, true, 0, n);
                double[] e = new double[m_data.length];
                for (int i = 0; i < err.length; ++i) {
                    double x = m_ser * Math.sqrt(err[i]);
                    e[i] = x;
                    e[e.length - i - 1] = x;
                }
                return e;
            } catch (ArimaException | MatrixException err) {
                return new double[m_data.length];
            }
        }
    }

    /**
     *
     * @param cmp
     * @param signal
     * @return
     */
    public double[] stdevForecasts(final int cmp, final boolean signal) {
        try {
            calc(cmp);
            if (m_wk.getUcarimaModel().getComponent(cmp).isNull()) {
                if (signal) {
                    return new double[m_nf];
                } else {
                    return null;
                }
            }

            double[] e = m_wk.totalErrorVariance(cmp, signal, -m_nf, m_nf);
            double[] err = new double[m_nf];
            for (int i = 0; i < m_nf; ++i) {
                err[i] = m_ser * Math.sqrt(e[m_nf - 1 - i]);
            }
            return err;
        } catch (ArimaException | MatrixException err) {
            return null;
        }
    }
}
