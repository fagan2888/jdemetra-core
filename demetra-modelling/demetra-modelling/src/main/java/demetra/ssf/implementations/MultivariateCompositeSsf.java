/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demetra.ssf.implementations;

import demetra.data.DoubleSequence;
import demetra.ssf.ISsfDynamics;
import demetra.ssf.ISsfInitialization;
import demetra.ssf.ISsfLoading;
import demetra.ssf.SsfComponent;
import demetra.ssf.SsfException;
import demetra.ssf.multivariate.IMultivariateSsf;
import demetra.ssf.multivariate.ISsfErrors;
import demetra.ssf.multivariate.ISsfMeasurements;
import demetra.ssf.multivariate.MultivariateSsf;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author palatej
 */
public class MultivariateCompositeSsf extends MultivariateSsf {

    private final int[] pos;
    private int[] dim;

    private MultivariateCompositeSsf(int[] pos, int[] dim, ISsfInitialization initializer, ISsfDynamics dynamics, ISsfMeasurements measurements) {
        super(initializer, dynamics, measurements);
        this.pos = pos;
        this.dim = dim;
    }

    public static Builder builder() {
        return new Builder();
    }

    @lombok.Value
    public static class Item {

        private String component;
        private double coefficient;
    }

    @lombok.Value
    public static class Equation {

        private final List<Item> items = new ArrayList<>();
        private double measurementError;

        public void add(Item item) {
            this.items.add(item);
        }
    }

    static class Builder {

        private final List<SsfComponent> components = new ArrayList<>();
        private final List<String> names = new ArrayList<>();
        private final List<Equation> equations = new ArrayList<>();
        private ISsfErrors measurementsError;

        public Builder add(String name, SsfComponent cmp) {
            components.add(cmp);
            names.add(name);
            return this;
        }

        public Builder add(Equation equation) {
            equations.add(equation);
            return this;
        }

        public Builder measurementError(ISsfErrors measurementsError) {
            this.measurementsError = measurementsError;
            return this;
        }

        public IMultivariateSsf build() {
            if (components.isEmpty() || equations.isEmpty()) {
                return null;
            }
            // build dim / pos
            int n = components.size();
            int neq = equations.size();
            int[] dim = new int[n];
            int[] pos = new int[n];
            ISsfInitialization[] i = new ISsfInitialization[n];
            ISsfDynamics[] d = new ISsfDynamics[n];
            ISsfLoading[] l = new ISsfLoading[n];
            int cpos = 0;
            for (int j = 0; j < n; ++j) {
                SsfComponent cur = components.get(j);
                pos[j] = cpos;
                dim[j] = i[j].getStateDim();
                cpos += dim[j];
                i[j] = cur.initialization();
                d[j] = cur.dynamics();
                l[j] = cur.loading();
            }
            ISsfErrors errors = measurementsError;
            if (errors == null) {
                errors = MeasurementsError.of(DoubleSequence.onMapping(neq, k -> equations.get(k).getMeasurementError()));
            }
            // creates the equations
            ISsfLoading[] loadings = new ISsfLoading[neq];
            for (int j = 0; j < neq; ++j) {
                loadings[j] = loadingOf(equations.get(j), pos, dim);
            }

            return new MultivariateSsf(new CompositeInitialization(dim, i),
                    new CompositeDynamics(dim, d),
                    new Measurements(loadings, errors));
        }

        private ISsfLoading loadingOf(Equation eq, int[] pos, int[] dim) {
            int[] c = cmpOf(eq);
            int[] npos = new int[c.length];
            int[] ndim = new int[c.length];
            ISsfLoading[] loadings = new ISsfLoading[c.length];
            for (int j = 0; j < c.length; ++j) {
                ISsfLoading loading = components.get(c[j]).loading();
                loadings[j] = Loading.rescale(loading, eq.items.get(j).coefficient);
                npos[j] = pos[c[j]];
                ndim[j] = dim[c[j]];
            }
            return new ComplexLoading(npos, ndim, loadings);
        }

        private int[] cmpOf(Equation eq) {
            int[] c = new int[eq.items.size()];
            for (int i = 0; i < c.length; ++i) {
                c[i] = cmp(eq.items.get(i).component);
            }
            return c;
        }

        private int cmp(String name) {
            int c = names.indexOf(name);
            if (c < 0) {
                throw new SsfException(SsfException.MODEL);
            }
            return c;
        }
    }

}
