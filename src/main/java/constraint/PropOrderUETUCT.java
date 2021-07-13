/*
@author Arthur Godet <arth.godet@gmail.com>
@since 02/12/2020
*/

package constraint;

import gnu.trove.list.array.TIntArrayList;
import java.util.Arrays;
import org.chocosolver.memory.IStateBool;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.tools.ArrayUtils;

/**
 * Propagator for the Order constraint specified to the UET-UCT, as described in the following thesis :
 * TODO: add the thesis citation when it is fixed (and/or the CP paper if accepted)
 *
 * @author Arthur Godet <arth.godet@gmail.com>
 */
public class PropOrderUETUCT extends Propagator<IntVar> {
    private static final int IDLE = -1;
    protected final IntVar[] order;
    protected final IntVar[] starts;
    protected final BoolVar[][] assignments;
    protected final int[][] predecessors;

    protected final int min;

    protected final IStateInt idxCurrentOrder;
    protected final IStateBool[] isOrdered;
    protected final IStateInt[][] machinesHeur;
    protected final int[][] machines;
    protected final IStateInt[] predInDpath;

    private final TIntArrayList dpath = new TIntArrayList();

    public PropOrderUETUCT(IntVar[] order, IntVar[] starts, BoolVar[][] assignments, int[][] predecessors) {
        super(ArrayUtils.append(order, starts), PropagatorPriority.QUADRATIC, false);
        this.order = order;
        this.starts = starts;
        this.assignments = assignments;
        this.predecessors = predecessors;

        this.isOrdered = new IStateBool[order.length];
        int n = starts.length;
        int m = assignments[0].length;
        this.machinesHeur = new IStateInt[m][n];
        this.machines = new int[m][n];
        this.predInDpath = new IStateInt[n];
        for(int i = 0; i < isOrdered.length; i++) {
            this.isOrdered[i] = getModel().getEnvironment().makeBool(false);
            this.predInDpath[i] = getModel().getEnvironment().makeInt(-1);
            for(int j = 0; j < m; j++) {
                this.machinesHeur[j][i] = getModel().getEnvironment().makeInt(IDLE);
            }
        }

        this.idxCurrentOrder = getModel().getEnvironment().makeInt(0);
        min = Arrays.stream(starts).mapToInt(IntVar::getLB).min().getAsInt();
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        if(vIdx < order.length) {
            return IntEventType.instantiation();
        } else {
            return IntEventType.all();
        }
    }

    protected void updateIdxCurrentOrder() {
        int idx = idxCurrentOrder.get();
        while(idx < order.length && order[idx].isInstantiated() && isOrdered[order[idx].getValue()].get()) {
            idx++;
        }
        idxCurrentOrder.set(idx);
        if(idxCurrentOrder.get() == order.length) {
            setPassive();
        }
    }

    private void buildSchedule() {
        for(int i = 0; i < isOrdered.length; i++) {
            for(int j = 0; j < machinesHeur.length; j++) {
                this.machines[j][i] = machinesHeur[j][i].get();
            }
        }
        for(int i = 0; i < starts.length; i++) {
            if(starts[i].isInstantiated() && isOrdered[i].get()) {
                for(int j = 0; j < machinesHeur.length; j++) {
                    if(assignments[i][j].isInstantiatedTo(1)) {
                        machines[j][starts[i].getValue()] = i;
                    }
                }
                for(int j = 0; j < predecessors[i].length; j++) {
                    if(starts[predecessors[i][j]].isInstantiated() && starts[predecessors[i][j]].getValue() + 1 == starts[i].getValue()) {
                        this.predInDpath[i].set(predecessors[i][j]);
                        break;
                    }
                }
            }
        }
    }

    private void computeDpath(int var, int start) {
        int p = -1;
        int startDpath = start;
        dpath.clear();
        dpath.add(var);
        do {
            for(int j = 0; j < predecessors[var].length; j++) {
                if(starts[predecessors[var][j]].getValue() + 1 == startDpath) {
                    startDpath--;
                    p = predInDpath[predecessors[var][j]].get();
                    var = predecessors[var][j];
                    dpath.add(var);
                    break;
                }
            }
        } while(p != -1);
    }

    private int minAccValue(int v, boolean heur, boolean inst) throws ContradictionException {
        int m = min;
        int nb = 0;
        for(int i = 0; i < predecessors[v].length; i++) {
            int t = starts[predecessors[v][i]].getValue();
            if(m < t) {
                nb = 1;
                m = t;
            } else if(m == t) {
                nb++;
            }
        }
        if(nb >= 2) {
            m += 2;
        } else if(nb == 1) {
            m += 1;
            computeDpath(v, m);
            if(heur) {
                for(int k = 0; k < machinesHeur.length; k++) {
                    for(int j = 0; j < dpath.size(); j++) {
                        if(machinesHeur[k][m-j].get() == dpath.getQuick(j)) {
                            if(inst) {
                                for(int l = 0; l < dpath.size(); l++) {
                                    int task = dpath.getQuick(l);
                                    assignments[task][k].instantiateTo(1, this);
                                    machinesHeur[k][starts[task].getValue()].set(task);
                                }
                                starts[v].instantiateTo(m, this);
                            }
                            return m; // D-path of v can be placed from here
                        } else if(machinesHeur[k][m-j].get() != IDLE) {
                            break;
                        } else if(j == dpath.size() - 1) {
                            if(inst) {
                                for(int l = 0; l < dpath.size(); l++) {
                                    int task = dpath.getQuick(l);
                                    assignments[task][k].instantiateTo(1, this);
                                    machinesHeur[k][starts[task].getValue()].set(task);
                                }
                                starts[v].instantiateTo(m, this);
                            }
                            return m; // D-path of v can be placed from here
                        }
                    }
                }
            } else {
                for(int k = 0; k < machines.length; k++) {
                    for(int j = 0; j < dpath.size(); j++) {
                        if(machines[k][m-j] == dpath.getQuick(j)) {
                            return m; // D-path of v can be placed from here
                        } else if(machines[k][m-j] != IDLE) {
                            break;
                        } else if(j == dpath.size() - 1) {
                            return m; // D-path of v can be placed from here
                        }
                    }
                }
            }
            m++;
        }
        boolean found;
        do {
            found = false;
            if(heur) {
                for(int k = 0; k < machinesHeur.length && !found; k++) {
                    found = machinesHeur[k][m].get() == IDLE;
                    if(found && inst) {
                        assignments[v][k].instantiateTo(1, this);
                        starts[v].instantiateTo(m, this);
                        machinesHeur[k][m].set(v);
                    }
                }
            } else {
                for(int k = 0; k < machines.length && !found; k++) {
                    found = machines[k][m] == IDLE;
                }
            }
            if(!found) {
                m++;
            }
        } while(!found);
        return m;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        do {
            updateIdxCurrentOrder();
            if(idxCurrentOrder.get() == order.length) {
                return;
            }
            buildSchedule();
            int idx = idxCurrentOrder.get();
            for(int v = order[idx].getLB(); v <= order[idx].getUB(); v = order[idx].nextValue(v)) {
                boolean allPredOrdered = Arrays.stream(predecessors[v]).allMatch(i -> isOrdered[i].get());
                int m = minAccValue(v,false, false);
                int mHeur = minAccValue(v, true, false);
                starts[v].updateLowerBound(m, this);
                if(!allPredOrdered || starts[v].getLB() > mHeur || m != mHeur) {
                    order[idx].removeValue(v, this);
                }
            }
            if(order[idx].isInstantiated()) {
                int i = order[idxCurrentOrder.get()].getValue();
                minAccValue(i,true, true);
                int t = starts[i].getValue();
                for(int k = 0; k < machinesHeur.length; k++) {
                    if(machinesHeur[k][t].get() != IDLE && machinesHeur[k][t].get() != i) {
                        assignments[i][k].instantiateTo(0, this);
                    }
                }
                isOrdered[i].set(true);
                for(int o = idx + 1; o < order.length; o++) {
                    order[o].removeValue(i, this);
                }
            }
        } while(order[idxCurrentOrder.get()].isInstantiated() && isOrdered[order[idxCurrentOrder.get()].getValue()].get());
    }

    @Override
    public ESat isEntailed() {
        for(int i = 0; i < order.length; i++) {
            if(!order[i].isInstantiated() || !starts[order[i].getValue()].isInstantiated()) {
                return ESat.UNDEFINED;
            }
        }
        return ESat.TRUE;
    }
}
