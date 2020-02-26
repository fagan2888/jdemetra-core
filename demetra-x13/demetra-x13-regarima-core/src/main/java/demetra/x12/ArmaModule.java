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
package demetra.x12;

import demetra.design.BuilderPattern;
import jdplus.regarima.RegArimaModel;
import jdplus.regarima.RegArimaUtility;
import jdplus.regsarima.regular.IArmaModule;
import jdplus.regsarima.regular.ModelDescription;
import jdplus.regsarima.regular.ProcessingResult;
import jdplus.regsarima.regular.RegSarimaModelling;
import jdplus.sarima.SarimaModel;
import demetra.arima.SarimaOrders;
import demetra.arima.SarmaOrders;
import demetra.data.DoubleSeq;

/**
 *
 * @author Jean Palate
 */
public class ArmaModule implements IArmaModule {

    public static Builder builder() {
        return new Builder();
    }

    @BuilderPattern(ArmaModule.class)
    public static class Builder {

        private boolean wn = false;
        private boolean balanced = false, mixed = true;
        private double eps = 1e-5;

        private Builder() {
        }

        Builder balanced(boolean balanced) {
            this.balanced = balanced;
            return this;
        }

        Builder mixed(boolean mixed) {
            this.mixed = mixed;
            return this;
        }

        Builder estimationPrecision(double eps) {
            this.eps = eps;
            return this;
        }

        Builder acceptWhiteNoise(boolean ok) {
            this.wn = ok;
            return this;
        }

        ArmaModule build() {
            return new ArmaModule(this);
        }
    }


        private final boolean wn, balanced, mixed;
        private final double eps;

    private ArmaModule(Builder builder) {
        this.balanced = builder.balanced;
        this.mixed = builder.mixed;
        this.wn = builder.wn;
        this.eps = builder.eps;
    }

    private ArmaModuleImpl createModule() {
        return ArmaModuleImpl.builder()
                .acceptWhiteNoise(wn)
                .balanced(balanced)
                .mixed(mixed)
                .estimationPrecision(eps)
                .maxP(2)
                .maxQ(2)
                .maxBp(1)
                .maxBq(1)
                .build();
    }

    @Override
    public ProcessingResult process(RegSarimaModelling context) {
        ModelDescription desc = context.getDescription();
        SarimaOrders curspec = desc.specification();
        DoubleSeq res = RegArimaUtility.olsResiduals(desc.regarima());
        ArmaModuleImpl impl = createModule();
        SarmaOrders nspec = impl.process(res, curspec.getPeriod(), curspec.getD(), curspec.getBd(), desc.getAnnualFrequency()>1);
        if (nspec.equals(curspec.doStationary())) {
            return ProcessingResult.Unchanged;
        }
        curspec = SarimaOrders.of(nspec, curspec.getD(), curspec.getBd());
        desc.setSpecification(curspec);
        return ProcessingResult.Changed;
    }

    public SarimaOrders process(RegArimaModel<SarimaModel> regarima, boolean seas) {
        SarimaOrders curSpec = regarima.arima().orders();
        DoubleSeq res = RegArimaUtility.olsResiduals(regarima);
        ArmaModuleImpl impl = createModule();
        SarmaOrders spec = impl.process(res, curSpec.getPeriod(), curSpec.getD(), curSpec.getBd(), curSpec.getPeriod() > 1);
        if (spec == null) {
            curSpec.setDefault(seas);
            return curSpec;
        } else {
            return SarimaOrders.of(spec, curSpec.getD(), curSpec.getBd());
        }
    }
}