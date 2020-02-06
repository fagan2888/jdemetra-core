/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demetra.timeseries.regression;

import java.util.Arrays;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Root of all regression variable definition. All definitions must contain
 * enough information for generating the actual regression variables in a given
 * context (corresponding to a ModellingContext).
 *
 * @author palatej
 */
public interface ITsVariable {

    int dim();

    public static int dim(@NonNull ITsVariable... vars) {
        return dim(Arrays.stream(vars));
    }

    public static int dim(@NonNull Stream<ITsVariable> vars) {
        return vars.mapToInt(var->var.dim()).sum();
    }

    public static String nextName(String name) {
        int pos0 = name.lastIndexOf('('), pos1 = name.lastIndexOf(')');
        if (pos0 > 0 && pos1 > 0) {
            String prefix = name.substring(0, pos0);
            int cur = 1;
            try {
                String num = name.substring(pos0 + 1, pos1);
                cur = Integer.parseInt(num) + 1;
            } catch (NumberFormatException err) {

            }
            StringBuilder builder = new StringBuilder();
            builder.append(prefix).append('(').append(cur).append(')');
            return builder.toString();
        } else {
            return name + "(1)";
        }
    }

}
