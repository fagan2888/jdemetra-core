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
package jdplus.regarima.ami;

import demetra.data.Data;
import jdplus.regarima.RegArimaModel;
import jdplus.regarima.outlier.ExactSingleOutlierDetector;
import jdplus.sarima.SarimaModel;
import demetra.arima.SarimaSpecification;
import jdplus.regsarima.GlsSarimaProcessor;
import demetra.timeseries.TsPeriod;
import org.junit.Test;
import static org.junit.Assert.*;
import demetra.data.DoubleSeq;
import jdplus.regsarima.internal.HannanRissanenInitializer;

/**
 *
 * @author Jean Palate
 */
public class OutliersDetectionModuleTest {

    public OutliersDetectionModuleTest() {
    }

    @Test
    public void testMonthly() {
        TsPeriod start = TsPeriod.monthly(1967, 1);
        HannanRissanenInitializer hr = HannanRissanenInitializer.builder().build();
        OutliersDetectionModule<SarimaModel> od = OutliersDetectionModule.build(SarimaModel.class)
                .processor(GlsSarimaProcessor.builder().initializer(hr).build())
                .detector(new ExactSingleOutlierDetector(null, null, null))
                .setAll()
                .build();
        od.setCriticalValue(3.0);
        SarimaSpecification spec=SarimaSpecification.airline(12);
        SarimaModel sarima = SarimaModel.builder(spec).setDefault().build();
        System.out.println("Full");
//        Consumer<int[]> hook = a -> System.out.println("Add outlier: " + od.getFactory(a[1]).getCode() + '-' + start.plus(a[0]).display());
//        Consumer<int[]> rhook = a -> System.out.println("Remove outlier: " + od.getFactory(a[1]).getCode() + '-' + start.plus(a[0]).display());
        RegArimaModel<SarimaModel> regarima = RegArimaModel.<SarimaModel>builder().y(DoubleSeq.copyOf(Data.PROD)).arima(sarima).build();
//        od.setAddHook(hook);
//        od.setRemoveHook(rhook);
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; ++i) {
            od.prepare(regarima.getObservationsCount());
            od.process(regarima);
        }
        long t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
        assertTrue(od.getOutliers().length == 8);
    }

    @Test
    public void testMonthly2() {
        TsPeriod start = TsPeriod.monthly(1967, 1);
        HannanRissanenInitializer hr = HannanRissanenInitializer.builder().build();
        OutliersDetectionModule<SarimaModel> od = OutliersDetectionModule.build(SarimaModel.class)
                .processor(GlsSarimaProcessor.builder().initializer(hr).build())
                .setAll()
                .build();
        od.setCriticalValue(3.0);
        SarimaSpecification spec=SarimaSpecification.airline(12);
        SarimaModel sarima = SarimaModel.builder(spec).setDefault().build();
        System.out.println("Fast");
//        Consumer<int[]> hook = a -> System.out.println("Add outlier: " + od.getFactory(a[1]).getCode() + '-' + start.plus(a[0]).display());
//        Consumer<int[]> rhook = a -> System.out.println("Remove outlier: " + od.getFactory(a[1]).getCode() + '-' + start.plus(a[0]).display());
        RegArimaModel<SarimaModel> regarima = RegArimaModel.<SarimaModel>builder().y(DoubleSeq.copyOf(Data.PROD)).arima(sarima).build();
//        od.setAddHook(hook);
//        od.setRemoveHook(rhook);
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < 100; ++i) {
            od.prepare(regarima.getObservationsCount());
            od.process(regarima);
        }
        long t1 = System.currentTimeMillis();
        System.out.println(t1 - t0);
        assertTrue(od.getOutliers().length == 8);
    }
}
