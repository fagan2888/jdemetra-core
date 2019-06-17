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
package demetra.regarima;

import demetra.design.Development;
import demetra.arima.SarimaModel;
import demetra.data.DoubleSeq;
import demetra.maths.matrices.Matrix;
import demetra.linearmodel.LinearModel;


/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Alpha)
@lombok.Value
@lombok.Builder(toBuilder=true)
public class RegSarimaModelType {
    
    @lombok.NonNull
    private LinearModel model;
    
    @lombok.NonNull
    private SarimaModel sarima;
    
    //<editor-fold defaultstate="collapsed" desc="delegate to model">
    public DoubleSeq getY() {
        return model.getY();
    }
    
    public boolean isMeanCorrection() {
        return model.isMeanCorrection();
    }
    
    public Matrix getX() {
        return model.getX();
    }
   
    //</editor-fold>
}