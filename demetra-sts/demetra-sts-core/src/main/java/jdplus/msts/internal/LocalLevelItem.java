/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdplus.msts.internal;

import jdplus.msts.StateItem;
import demetra.data.DoubleSeq;
import jdplus.msts.ModelItem;
import jdplus.msts.MstsMapping;
import jdplus.msts.VarianceInterpreter;
import jdplus.ssf.SsfComponent;
import jdplus.sts.LocalLevel;
import java.util.Collections;
import java.util.List;
import jdplus.msts.ParameterInterpreter;
import jdplus.ssf.ISsfLoading;
import jdplus.ssf.StateComponent;

/**
 *
 * @author palatej
 */
public class LocalLevelItem extends StateItem {

    public final VarianceInterpreter v;
    public final double initial;

    public LocalLevelItem(String name, final double lvar, final boolean fixed, final double initial) {
        super(name);
        this.initial = initial;
        this.v = new VarianceInterpreter(name + ".var", lvar, fixed, true);
    }

    @Override
    public void addTo(MstsMapping mapping) {
        mapping.add(v);
        mapping.add((p, builder) -> {
            double e = p.get(0);
            SsfComponent cmp = LocalLevel.of(e, initial);
            builder.add(name, cmp);
            return 1;
        });
    }

    @Override
    public List<ParameterInterpreter> parameters() {
        return Collections.singletonList(v);
    }

    @Override
    public StateComponent build(DoubleSeq p) {
        double e = p.get(0);
        return LocalLevel.stateComponent(e, initial);
    }

    @Override
    public int parametersCount() {
        return 1;
    }

    @Override
    public ISsfLoading defaultLoading(int m) {
        return LocalLevel.loading();
    }

    @Override
    public int defaultLoadingCount() {
        return 1;
    }

    @Override
    public int stateDim() {
        return 1;
    }
}
