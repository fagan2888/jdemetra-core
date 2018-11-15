/*
 * Copyright 2016 National Bank of Belgium
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
package demetra.ssf.akf;

import demetra.data.DataBlock;
import demetra.maths.functions.IFunction;
import demetra.maths.functions.ssq.ISsqFunction;
import demetra.ssf.univariate.ISsf;
import demetra.data.DoubleSequence;
import demetra.likelihood.ILikelihoodFunctionPoint;
import demetra.maths.functions.IFunctionDerivatives;
import demetra.maths.functions.NumericalDerivatives;
import demetra.maths.functions.ssq.ISsqFunctionDerivatives;
import demetra.maths.functions.ssq.SsqNumericalDerivatives;
import demetra.ssf.univariate.ILikelihoodComputer;

/**
 *
 * @author Jean Palate
 * @param <S>
 */
public class AkfFunctionPoint<S, F extends ISsf> implements
        ILikelihoodFunctionPoint<MarginalLikelihood> {

    /**
     *
     */
    private final F currentSsf;
    private final S current;

    /**
     *
     */
    private final MarginalLikelihood ll;
    private final DataBlock p;
    private DataBlock E;
    private final AkfFunction<S, F> fn;

    /**
     *
     * @param fn
     * @param p
     */
    public AkfFunctionPoint(AkfFunction<S, F> fn, DoubleSequence p) {
        this.fn = fn;
        this.p = DataBlock.of(p);
        current=fn.getMapping().map(p);
        currentSsf = fn.getBuilder().buildSsf(current);
        ILikelihoodComputer<MarginalLikelihood> computer= AkfToolkit.marginalLikelihoodComputer();
        ll=computer.compute(currentSsf, fn.getData());
    }

    public F getSsf() {
        return currentSsf;
    }

    public S getCore() {
        return current;
    }

    @Override
    public DoubleSequence getE() {
        if (E == null) {
            DoubleSequence res = ll.e();
            if (res == null) {
                return null;
            } else {
                E = DataBlock.select(res, x->Double.isFinite(x));
                if (fn.isMaximumLikelihood()) {
                    double factor = Math.sqrt(ll.factor());
                    E.mul(factor);
                }
            }
        }
        return E;
    }

    /**
     *
     * @return
     */
    @Override
    public MarginalLikelihood getLikelihood() {
        return ll;
    }

    @Override
    public DoubleSequence getParameters() {
        return p;
    }

    @Override
    public double getSsqE() {
        if (ll == null) {
            return Double.NaN;
        }
        return fn.isMaximumLikelihood() ? ll.ssq() * ll.factor() : ll.ssq();
    }

    @Override
    public double getValue() {
        if (ll == null) {
            return Double.NaN;
        }
        if (fn.isLog()) {
            return fn.isMaximumLikelihood() ? -ll.logLikelihood() : Math.log(ll.ssq());
        } else {
            return fn.isMaximumLikelihood() ? ll.ssq() * ll.factor() : ll
                    .ssq();
        }
    }

    @Override
    public ISsqFunction getSsqFunction() {
        return fn;
    }

    @Override
    public IFunction getFunction() {
        return fn;
    }
    
    @Override
     public IFunctionDerivatives derivatives(){
        return new NumericalDerivatives(this, fn.isSymmetric(), fn.isMultiThreaded());
    };

    @Override
     public ISsqFunctionDerivatives ssqDerivatives(){
        return new SsqNumericalDerivatives(this, fn.isSymmetric(), fn.isMultiThreaded());
    };
}