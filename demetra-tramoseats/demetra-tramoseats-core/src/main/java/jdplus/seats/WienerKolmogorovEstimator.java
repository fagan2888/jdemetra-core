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
package jdplus.seats;

import demetra.data.DoubleSeq;
import demetra.data.DoublesMath;
import demetra.design.Development;
import demetra.modelling.ComponentInformation;
import demetra.sa.ComponentType;
import demetra.sa.DecompositionMode;
import demetra.sa.SeriesDecomposition;
import java.util.ArrayList;
import jdplus.ucarima.UcarimaModel;

/**
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
public class WienerKolmogorovEstimator implements IComponentsEstimator {

    private int nf(int period, int npred) {
        if (npred >= 0) {
            return npred;
        } else {
            return period * (-npred);
        }
    }

    public WienerKolmogorovEstimator() {
    }

    /**
     *
     * @param model
     * @return
     */
    @Override
    public SeriesDecomposition decompose(SeatsModel model) {
        SeriesDecomposition.Builder decomposition = SeriesDecomposition.builder(DecompositionMode.Additive);
        BurmanEstimates burman = new BurmanEstimates();
//	BurmanEstimates burman = new BurmanEstimates();
        UcarimaModel ucm = model.getUcarimaModel();
        ucm = ucm.compact(2, 2);

        DoubleSeq s = model.getTransformedSeries();
        int nf = nf(model.getOriginalModel().getFrequency(), model.getForecastsCount());
        burman.setForecastsCount(nf);

        // check the ucarima model. 
        // ucm=checkModel(ucm);
        if (model.isMeanCorrection()) {
            burman.setUcarimaModelWithMean(ucm);
        } else {
            burman.setUcarimaModel(ucm);
        }
        burman.setData(s);
        burman.setSer(Math.sqrt(model.getInnovationVariance()));
        int ncmps = ucm.getComponentsCount();

        DoubleSeq[] cmps = new DoubleSeq[ncmps];
        DoubleSeq[] fcmps = new DoubleSeq[ncmps];
        DoubleSeq[] ecmps = new DoubleSeq[ncmps];
        DoubleSeq[] efcmps = new DoubleSeq[ncmps];

        for (int i = 0; i < ncmps; ++i) {
            if (i == 0 || !ucm.getComponent(i).isNull()) {
                double[] tmp = burman.estimates(i, true);
                cmps[i] = DoubleSeq.of(tmp);
                ecmps[i] = DoubleSeq.of(burman.stdevEstimates(i));
                tmp = burman.forecasts(i, true);
                if (tmp != null) {
                    fcmps[i] = DoubleSeq.of(tmp);
                }
                tmp = burman.stdevForecasts(i, true);
                if (tmp != null) {
                    efcmps[i] = DoubleSeq.of(tmp);
                }
            }
        }

        DoubleSeq fs = burman.getSeriesForecasts();
        DoubleSeq efs = null, efsa = null;
        for (int i = 0; i < efcmps.length; ++i) {
            if (efcmps[i] != null) {
                DoubleSeq var = efcmps[i].fn(z -> z * z);
                efs = DoublesMath.add(efs, var);
                if (i != 1) {
                    efsa = DoublesMath.add(efsa, var);
                }

            }
        }

        decomposition.add(s, ComponentType.Series);
        decomposition.add(fs, ComponentType.Series, ComponentInformation.Forecast);
        if (efs != null) {
            decomposition.add(efs.fn(z->z<=0 ? 0 :Math.sqrt(z)), ComponentType.Series, ComponentInformation.StdevForecast);
        }
        if (cmps[0] != null) {
            decomposition.add(cmps[0], ComponentType.Trend);
        }
        if (cmps[1] != null) {
            decomposition.add(cmps[1], ComponentType.Seasonal);
        }

        if (fcmps[0] != null) {
            decomposition.add(fcmps[0], ComponentType.Trend, ComponentInformation.Forecast);
        }
        if (fcmps[1] != null) {
            decomposition.add(fcmps[1], ComponentType.Seasonal, ComponentInformation.Forecast);
        }
        decomposition.add(DoublesMath.subtract(fs, fcmps[1]),
                ComponentType.SeasonallyAdjusted, ComponentInformation.Forecast);
        if (ecmps[0] != null) {
            decomposition.add(ecmps[0], ComponentType.Trend, ComponentInformation.Stdev);
        }
        if (efcmps[0] != null) {
            decomposition.add(efcmps[0], ComponentType.Trend, ComponentInformation.StdevForecast);
        }
        decomposition.add(DoublesMath.subtract(s, cmps[1]),
                ComponentType.SeasonallyAdjusted);
        if (ecmps[1] != null) {
            decomposition.add(ecmps[1], ComponentType.Seasonal, ComponentInformation.Stdev);
            decomposition.add(ecmps[1], ComponentType.SeasonallyAdjusted, ComponentInformation.Stdev);
            decomposition.add(efsa.fn(z->z<=0 ? 0 : Math.sqrt(z)), ComponentType.SeasonallyAdjusted, ComponentInformation.StdevForecast);
        }
        if (efcmps[1] != null) {
            decomposition.add(efcmps[1], ComponentType.Seasonal, ComponentInformation.StdevForecast);
        }
        decomposition.add(cmps[2], ComponentType.Irregular);
        if (fcmps[2] != null) {
            decomposition.add(fcmps[2], ComponentType.Irregular, ComponentInformation.Forecast);
        }
        if (ecmps[2] != null) {
            decomposition.add(ecmps[2], ComponentType.Irregular, ComponentInformation.Stdev);
        }
        if (efcmps[2] != null) {
            decomposition.add(efcmps[2], ComponentType.Irregular, ComponentInformation.StdevForecast);
        }

        return decomposition.build();
    }

}
