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
package jdplus.regarima.internal;

import jdplus.regarima.RegArmaModel;
import jdplus.arima.IArimaModel;
import jdplus.math.matrices.Matrix;

/**
 *
 * @author Jean Palate
 * @param <S>
 */
@lombok.Value
public class RegArmaEstimation<S extends IArimaModel> {

//    /**
//     * Differenced model
//     */
//    private RegArmaModel<S> model;
//    /**
//     * Objective function
//     */
//    private double objective;
//    private boolean converged;
    private double[] parameters;
    private double[] gradient;
    private Matrix hessian;
//    private int degreesOfFreedom;
}
