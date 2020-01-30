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
package jdplus.regarima;

import jdplus.arima.estimation.IArimaMapping;
import jdplus.arima.IArimaModel;
import jdplus.arima.StationaryTransformation;
import internal.jdplus.arima.FastKalmanFilter;
import jdplus.data.DataBlock;
import demetra.data.DoubleSeqCursor;
import jdplus.likelihood.ConcentratedLikelihoodWithMissing;
import jdplus.linearmodel.LeastSquaresResults;
import jdplus.linearmodel.LinearModel;
import jdplus.linearmodel.Ols;
import jdplus.math.linearfilters.BackFilter;
import jdplus.math.polynomials.Polynomial;
import jdplus.math.polynomials.UnitRoots;
import jdplus.regsarima.GlsSarimaProcessor;
import jdplus.sarima.SarimaModel;
import demetra.arima.SarimaSpecification;
import jdplus.regsarima.internal.HannanRissanenInitializer;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import demetra.data.DoubleSeq;
import jdplus.likelihood.Likelihood;
import jdplus.math.functions.levmar.LevenbergMarquardtMinimizer;

/**
 *
 * @author Jean Palate
 */
@lombok.experimental.UtilityClass
public class RegArimaUtility {

    /**
     * Data corrected for regression effects (except mean effect)
     *
     * @param <M>
     * @param model
     * @param concentratedLikelihood
     * @return
     */
    public <M extends IArimaModel> DoubleSeq linearizedData(@NonNull RegArimaModel<M> model, @NonNull ConcentratedLikelihoodWithMissing concentratedLikelihood) {
        double[] res = model.getY().toArray();

        // handle missing values
        int[] missing = model.missing();
        if (missing.length > 0) {
            DoubleSeq missingEstimates = concentratedLikelihood.missingEstimates();
            for (int i = 0; i < missing.length; ++i) {
                res[missing[i]] -= missingEstimates.get(i);
            }
        }
        DoubleSeq b = concentratedLikelihood.coefficients();
        DataBlock e = DataBlock.of(res);
        if (b.length() > 0) {
            List<DoubleSeq> x = model.getX();
            int cur = model.isMean() ? 1 : 0;
            for (int i = 0; i < x.size(); ++i) {
                double bcur = b.get(cur++);
                e.apply(x.get(i), (u, v) -> u - bcur * v);
            }
        }
        return e;
    }

    public <M extends IArimaModel> DoubleSeq interpolatedData(@NonNull RegArimaModel<M> model, @NonNull ConcentratedLikelihoodWithMissing concentratedLikelihood) {
        int[] missing = model.missing();
        if (missing.length == 0) {
            return model.getY();
        }
        double[] y = model.getY().toArray();

        // handle missing values
        DoubleSeqCursor reader = concentratedLikelihood.missingEstimates().cursor();
        for (int i = 0; i < missing.length; ++i) {
            y[missing[i]] -= reader.getAndNext();
        }
        return DoubleSeq.of(y);
    }

    /**
     * Regression effect generated by a range of variable
     *
     * @param <M>
     * @param model
     * @param concentratedLikelihood
     * @param startPos Start position (including) of the removed regression
     * variables.
     * That start position is defined in the list of all regression variables,
     * excluding
     * possible missing values (measured by additive outliers) and mean
     * correction.
     * @param nvars Number of removed regression variable
     * @return
     */
    public <M extends IArimaModel> DoubleSeq regressionEffect(@NonNull RegArimaModel<M> model,
            @NonNull ConcentratedLikelihoodWithMissing concentratedLikelihood, int startPos, int nvars) {
        DoubleSeq b = concentratedLikelihood.coefficients();
        DataBlock e = DataBlock.make(model.getObservationsCount());
        if (b.length() > 0) {
            List<DoubleSeq> x = model.getX();
            DoubleSeqCursor reader = b.cursor();
            reader.moveTo(model.isMean() ? startPos + 1 : startPos);
            int i0 = startPos, i1 = startPos + nvars;
            for (int i = i0; i < i1; ++i) {
                double bcur = reader.getAndNext();
                e.apply(x.get(i), (u, v) -> u + bcur * v);
            }
        }
        return e.unmodifiable();
    }

    /**
     *
     * @param <M>
     * @param model
     * @return
     */
    public <M extends IArimaModel> DoubleSeq olsResiduals(@NonNull RegArimaModel<M> model) {
        LinearModel lm = model.differencedModel().asLinearModel();
        if (lm.getVariablesCount() > 0) {
            LeastSquaresResults lsr = Ols.compute(lm);
            return lm.calcResiduals(lsr.getCoefficients());
        } else {
            return lm.getY();
        }
    }

    /**
     *
     * @param <M>
     * @param model
     * @param concentratedLikelihood
     * @return
     */
    public <M extends IArimaModel> DoubleSeq fullResiduals(@NonNull RegArimaModel<M> model, @NonNull ConcentratedLikelihoodWithMissing concentratedLikelihood) {
        // compute the residuals...
        if (model.getVariablesCount() == 0) {
            return concentratedLikelihood.e();
        }

        DoubleSeq ld = linearizedData(model, concentratedLikelihood);
        StationaryTransformation st = model.arima().stationaryTransformation();
        DataBlock dld;

        if (st.getUnitRoots().getDegree() == 0) {
            dld = DataBlock.of(ld);
            if (model.isMean()) {
                dld.sub(concentratedLikelihood.coefficients().get(0));
            }
        } else {
            dld = DataBlock.make(ld.length() - st.getUnitRoots().getDegree());
        }
        st.getUnitRoots().apply(ld, dld);

        FastKalmanFilter kf = new FastKalmanFilter((IArimaModel) st.getStationaryModel());
        Likelihood ll = kf.process(dld);
        return ll.e();

    }

    public IRegArimaProcessor<SarimaModel> processor(IArimaMapping<SarimaModel> mapping, boolean ml, double eps) {
        HannanRissanenInitializer initializer = HannanRissanenInitializer.builder()
                .stabilize(true)
                .useDefaultIfFailed(true)
                .build();
        return GlsSarimaProcessor.builder()
                .mapping(mapping)
                .minimizer(LevenbergMarquardtMinimizer.builder())
                .precision(eps)
                .initializer(initializer)
                .useMaximumLikelihood(ml)
                .build();
    }

    public RegArimaModel<SarimaModel> airlineModel(DoubleSeq data, boolean mean, int ifreq, boolean seas) {
        // use airline model with mean
        SarimaSpecification spec = seas ? SarimaSpecification.airline(ifreq) : SarimaSpecification.m011(ifreq);
        SarimaModel arima = SarimaModel.builder(spec)
                .setDefault()
                .build();
        return RegArimaModel.<SarimaModel>builder()
                .arima(arima)
                .y(data)
                .meanCorrection(mean)
                .build();
    }

    public BackFilter differencingFilter(int freq, int d, int bd) {
        Polynomial X = null;
        if (d > 0) {
            X = UnitRoots.D(1, d);
        }
        if (bd > 0) {
            Polynomial XD = UnitRoots.D(freq, bd);
            if (X == null) {
                X = XD;
            } else {
                X = X.times(XD);
            }
        }
        if (X == null) {
            X = Polynomial.ONE;
        }
        return new BackFilter(X);
    }

    /**
     *
     * @param differencing
     * @param n
     * @return
     */
    public double[] meanRegressionVariable(final BackFilter differencing, final int n) {
        double[] m = new double[n];

        double[] D = differencing.asPolynomial().toArray();
        int d = D.length - 1;
        m[d] = 1;
        for (int i = d + 1; i < n; ++i) {
            double s = 1;
            for (int j = 1; j <= d; ++j) {
                s -= m[i - j] * D[j];
            }
            m[i] = s;
        }
        return m;
    }

    public static int defaultLjungBoxLength(final int period) {
        switch (period) {
            case 12:
                return 24;
            case 1:
                return 8;
            default:
                return 4 * period;
        }
    }
}
