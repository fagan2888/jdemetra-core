/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demetra.ssf.models;

import demetra.arima.AutoCovarianceFunction;
import demetra.data.DataBlock;
import demetra.data.DataBlockIterator;
import demetra.data.DoubleReader;
import demetra.maths.matrices.Matrix;
import demetra.maths.polynomials.Polynomial;
import demetra.ssf.ISsfDynamics;
import demetra.ssf.ISsfInitialization;
import demetra.ssf.SsfComponent;
import demetra.ssf.implementations.Loading;
import javax.annotation.Nonnull;

/**
 *
 * @author PALATEJ
 */
@lombok.experimental.UtilityClass
public class SsfAr {
    
    public SsfComponent of(@Nonnull double[] ar, double var, int nlags){
        if (ar.length == 0)
            throw new IllegalArgumentException();
        if (nlags<ar.length)
            nlags=ar.length;
        Data data=new Data(ar,var,nlags);
        return new SsfComponent(new Initialization(data), new Dynamics(data), Loading.fromPosition(0));
    }

    @lombok.Value
    private static class Data {

        double[] phi;
        double var;
        int nlags;

        Polynomial ar() {
            double[] c = new double[1 + phi.length];
            c[0] = 1;
            for (int i = 0; i < phi.length; ++i) {
                c[i + 1] = -phi[i];
            }
            return Polynomial.ofInternal(c);
        }

        int dim() {
            return Math.max(phi.length, nlags);
        }
    }

    private static class Initialization implements ISsfInitialization {

        private final Data info;

        Initialization(final Data info) {
            this.info = info;
        }

        @Override
        public int getStateDim() {
            return info.dim();
        }

        @Override
        public boolean isDiffuse() {
            return false;
        }

        @Override
        public int getDiffuseDim() {
            return 0;
        }

        @Override
        public void diffuseConstraints(Matrix b) {
        }

        @Override
        public void a0(DataBlock a0) {
        }

        @Override
        public void Pi0(Matrix pf0) {
        }

        @Override
        public void Pf0(Matrix pf0) {
            AutoCovarianceFunction acf = new AutoCovarianceFunction(Polynomial.ONE, info.ar(), info.var);
            acf.prepare(pf0.getColumnsCount());
            pf0.diagonal().set(acf.get(0));
            for (int i = 1; i < pf0.getColumnsCount(); ++i) {
                pf0.subDiagonal(i).set(acf.get(i));
                pf0.subDiagonal(-i).set(acf.get(i));
            }
        }

    }

    private static class Dynamics implements ISsfDynamics {

        private final Data info;
        private DataBlock z;

        Dynamics(final Data info) {
            this.info = info;
            z = DataBlock.make(info.dim());
        }

        @Override
        public int getInnovationsDim() {
            return 1;
        }

        @Override
        public void V(int pos, Matrix qm) {
            qm.set(0, 0, info.var);
        }

        @Override
        public void S(int pos, Matrix cm) {
            cm.set(0, 0, Math.sqrt(info.var));
        }

        @Override
        public boolean hasInnovations(int pos) {
            return true;
        }

        @Override
        public boolean areInnovationsTimeInvariant() {
            return true;
        }

        @Override
        public void T(int pos, Matrix tr) {
            tr.subDiagonal(-1).set(1);
            tr.row(0).extract(0, info.phi.length).copyFrom(info.phi, 0);
        }

        @Override
        public void TX(int pos, DataBlock x) {
            double z = 0;
            DoubleReader reader = x.reader();
            for (int i = 0; i < info.phi.length; ++i) {
                z += info.phi[i] * reader.next();
            }
            x.fshift(1);
            x.set(0, z);
        }

        @Override
        public void TVT(final int pos, final Matrix vm) {
            z.set(0);
            DataBlockIterator cols = vm.columnsIterator();
            for (int i = 0; i < info.phi.length; ++i) {
                z.addAY(info.phi[i], cols.next());
            }
            TX(pos, z);
            vm.downRightShift(1);
            vm.column(0).copy(z);
            vm.row(0).copy(z);
        }

        @Override
        public void addSU(int pos, DataBlock x, DataBlock u) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addV(int pos, Matrix p) {
            p.add(0, 0, info.var);
        }

        @Override
        public void XT(int pos, DataBlock x) {
            double first = x.get(0);
            x.bshift(1);
            x.setLast(0);
            if (first != 0) {
                for (int i = 0; i < info.phi.length; ++i) {
                    x.add(i, first * info.phi[i]);
                }
            }
        }

        @Override
        public void XS(int pos, DataBlock x, DataBlock xs) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isTimeInvariant() {
            return true;
        }

    }

}