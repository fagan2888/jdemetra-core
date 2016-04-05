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

package ec.tstoolkit.modelling;

import ec.tstoolkit.design.Development;
import ec.tstoolkit.timeseries.regression.ITsVariable;
import ec.tstoolkit.timeseries.regression.InterventionVariable;
import ec.tstoolkit.timeseries.regression.TsVariableList;
import java.util.List;

/**
 *
 * @author Jean Palate
 */
@Development(status = Development.Status.Preliminary)
public class Variable implements Cloneable {

    public static boolean isUsed(List<Variable> vars) {
        for (Variable var : vars) {
            if (var.status.isSelected()) {
                return true;
            }
        }
        return false;
    }

    public static <T extends ITsVariable> int usedCount(List<Variable> vars, Class<T> tclass) {
        int n = 0;
        for (Variable var : vars) {
            if (var.status.isSelected() && tclass.isInstance(var.getRootVariable())) {
                n += var.variable.getDim();
            }
        }
        return n;
    }

    public static int usedVariablesCount(List<Variable> vars) {
        if (vars == null) {
            return 0;
        }
        int n = 0;
        for (Variable var : vars) {
            if (var.status.isSelected()) {
                n += var.variable.getDim();
            }
        }
        return n;
    }

    public static boolean needTesting(List<Variable> vars) {
        if (vars == null || vars.isEmpty()) {
            return false;
        }
        for (Variable var : vars) {
            if (var.status.needTesting()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isUsageDefined(List<Variable> vars) {
        if (vars == null || vars.isEmpty()) {
            return true;
        }
        for (Variable var : vars) {
            if (!var.status.isDefined()) {
                return false;
            }
        }
        return true;
    }

    public static boolean select(List<Variable> vars, RegStatus status) {
        if (vars == null || vars.isEmpty()) {
            return false;
        }
        boolean found = false;
        for (Variable var : vars) {
            if (status == var.status) {
                found = true;
            } else {
                var.status = RegStatus.Excluded;
            }
        }
        return found;
    }

    public static Variable search(List<Variable> vars, ITsVariable x) {
        if (vars == null || vars.isEmpty()) {
            return null;
        }
        for (Variable var : vars) {
            if (x == var.variable) {
                return var;
            }
        }
        return null;
    }

    public static boolean replace(List<Variable> vars, ITsVariable xold, ITsVariable xnew) {
        if (vars == null || vars.isEmpty()) {
            return false;
        }
        for (Variable var : vars) {
            if (xold == var.variable) {
                var.variable = xnew;
                return true;
            }
        }
        return false;
    }

    public static boolean setStatus(List<Variable> vars, ITsVariable x, RegStatus status) {
        if (vars == null || vars.isEmpty()) {
            return false;
        }
        for (Variable var : vars) {
            if (x == var.variable) {
                var.status = status;
                return true;
            }
        }
        return false;
    }

    public static <S extends ITsVariable> void setStatus(List<Variable> vars, Class<S> sclass, RegStatus status) {
        if (vars != null) {
            for (Variable var : vars) {
                if (sclass.isInstance(var.getRootVariable())) {
                    var.status = status;
                }
            }
        }
    }

    public static RegStatus getStatus(List<Variable> vars, ITsVariable x) {
        if (vars != null && !vars.isEmpty()) {
            for (Variable var : vars) {
                if (x == var.variable) {
                    return var.status;
                }
            }
        }
        return RegStatus.Undefined;
    }

    public Variable(ITsVariable var) {
        variable = var;
        type = ComponentType.Undefined;
    }

    public Variable(ITsVariable var, ComponentType cmp) {
        variable = var;
        type = cmp;
    }

    public Variable(ITsVariable var, ComponentType cmp, RegStatus status) {
        variable = var;
        type = cmp;
        this.status = status;
    }

    @Override
    public Variable clone() {
        try {
            Variable var = (Variable) super.clone();
            return var;
        } catch (CloneNotSupportedException err) {
            throw new AssertionError();
        }
    }

     public ITsVariable getVariable() {
        return variable;
    }

    public ITsVariable getRootVariable() {
        return TsVariableList.getRoot(variable);
    }

    public <T extends ITsVariable> boolean isCompatible(Class<T> tclass) {
        return tclass.isAssignableFrom(TsVariableList.getRoot(variable).getClass());
    }

    public void setVariable(ITsVariable var) {
        variable = var;
    }
    
    public static ComponentType searchType(InterventionVariable var){
        return DeterministicComponent.getType(var);
    }
    
    private ITsVariable variable;
    public final ComponentType type;
    public RegStatus status = RegStatus.Prespecified;
}
