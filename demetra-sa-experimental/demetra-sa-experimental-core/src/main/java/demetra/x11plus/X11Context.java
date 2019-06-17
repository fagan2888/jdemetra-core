/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demetra.x11plus;

import jdplus.data.DataBlock;
import jdplus.data.analysis.DiscreteKernel;
import jdplus.maths.linearfilters.FiniteFilter;
import jdplus.maths.linearfilters.SymmetricFilter;
import demetra.sa.DecompositionMode;
import java.util.function.IntToDoubleFunction;
import demetra.data.DoubleSeq;

/**
 *
 * @author Jean Palate <jean.palate@nbb.be>
 */
@lombok.Value
@lombok.Builder
public class X11Context {

    @lombok.NonNull
    DecompositionMode mode;
    @lombok.NonNull
    Number period;
    int trendFilterLength;
    int localPolynomialDegree;
    String kernel;
    String leftAsymmetricEndPoints, rightAsymmetricEndPoints;
    @lombok.NonNull
    SeasonalFilterOption initialSeasonalFilter;
    @lombok.NonNull
    SeasonalFilterOption finalSeasonalFilter;
    double lowerSigma, upperSigma;

    public static X11ContextBuilder builder() {
        X11ContextBuilder builder = new X11ContextBuilder();
        builder.mode = DecompositionMode.Multiplicative;
        builder.trendFilterLength = 13;
        builder.kernel = "Henderson";
        builder.localPolynomialDegree = 3;
        builder.leftAsymmetricEndPoints = "LC";
        builder.rightAsymmetricEndPoints = "LC";
        builder.initialSeasonalFilter = SeasonalFilterOption.S3X3;
        builder.finalSeasonalFilter = SeasonalFilterOption.S3X5;
        builder.lowerSigma = 1.5;
        builder.upperSigma = 2.5;
        return builder;
    }

    public DoubleSeq remove(DoubleSeq l, DoubleSeq r) {
        if (mode == DecompositionMode.Multiplicative || mode == DecompositionMode.PseudoAdditive) {
            return DoubleSeq.onMapping(l.length(), i -> l.get(i) / r.get(i));
        } else {
            return DoubleSeq.onMapping(l.length(), i -> l.get(i) - r.get(i));
        }
    }

    public DoubleSeq add(DoubleSeq l, DoubleSeq r) {
        if (mode == DecompositionMode.Multiplicative || mode == DecompositionMode.PseudoAdditive) {
            return DoubleSeq.onMapping(l.length(), i -> l.get(i) * r.get(i));
        } else {
            return DoubleSeq.onMapping(l.length(), i -> l.get(i) + r.get(i));
        }
    }

    public void remove(DoubleSeq l, DoubleSeq r, DataBlock q) {
        if (mode == DecompositionMode.Multiplicative || mode == DecompositionMode.PseudoAdditive) {
            q.set(l, r, (x, y) -> x / y);
        } else {
            q.set(l, r, (x, y) -> x - y);
        }
    }

    public void add(DoubleSeq l, DoubleSeq r, DataBlock q) {
        if (mode == DecompositionMode.Multiplicative || mode == DecompositionMode.PseudoAdditive) {
            q.set(l, r, (x, y) -> x * y);
        } else {
            q.set(l, r, (x, y) -> x + y);
        }
    }

    public SymmetricFilter trendFilter() {
        int horizon = trendFilterLength / 2;
        IntToDoubleFunction weights = weights(horizon, kernel);
        return jdplus.maths.linearfilters.LocalPolynomialFilters.of(horizon, localPolynomialDegree, weights);
    }

    private static final double SQRPI = Math.sqrt(Math.PI);

    public FiniteFilter[] leftAsymmetricTrendFilters(SymmetricFilter sfilter, double ic) {
        double d = 2 / (SQRPI * ic);
        return asymmetricFilters(sfilter, true, d);
    }

    public FiniteFilter[] rightAsymmetricTrendFilters(SymmetricFilter sfilter, double ic) {
        double d = 2 / (SQRPI * ic);
        return asymmetricFilters(sfilter, false, d);
    }

    private FiniteFilter[] asymmetricFilters(SymmetricFilter sfilter, boolean left, double d) {
        FiniteFilter[] afilters;
        int horizon = sfilter.getUpperBound();
        String endpoints = left ? leftAsymmetricEndPoints : rightAsymmetricEndPoints;
        if (endpoints.equals("DAF")) {
            IntToDoubleFunction weights = weights(horizon, kernel);
            afilters = new FiniteFilter[horizon];
            for (int i = 0; i < afilters.length; ++i) {
                afilters[horizon-i-1] = jdplus.maths.linearfilters.LocalPolynomialFilters.directAsymmetricFilter(horizon, i, localPolynomialDegree, weights);
            }
        } else if (endpoints.equals("CN")) {
            afilters = new FiniteFilter[horizon];
            for (int i = 0; i < afilters.length; ++i) {
                afilters[horizon-i-1] = jdplus.maths.linearfilters.LocalPolynomialFilters.cutAndNormalizeFilter(sfilter, i);
            }
        } else {
            int u = 0;
            double[] c = new double[]{d};
            switch (endpoints) {
                case "CC":
                    c = new double[0];
                case "LC":
                    u = 0;
                    break;
                case "QL":
                    u = 1;
                    break;
                case "CQ":
                    u = 2;
                    break;
            }
            afilters = new FiniteFilter[horizon];
            for (int i = 0; i < afilters.length; ++i) {
                afilters[horizon-i-1] = jdplus.maths.linearfilters.LocalPolynomialFilters.asymmetricFilter(sfilter, i, u, c, null);
            }
        }
        return afilters;

    }

    static IntToDoubleFunction weights(int horizon, String filter) {
        switch (filter) {
            case "Uniform":
                return DiscreteKernel.uniform(horizon);
            case "Biweight":
                return DiscreteKernel.biweight(horizon);
            case "Triweight":
                return DiscreteKernel.triweight(horizon);
            case "Tricube":
                return DiscreteKernel.tricube(horizon);
            case "Triangular":
                return DiscreteKernel.triangular(horizon);
            case "Parabolic":
                return DiscreteKernel.parabolic(horizon);
            case "Gaussian":
                return DiscreteKernel.gaussian(4 * horizon);
            default:
                return DiscreteKernel.henderson(horizon);
        }
    }

}