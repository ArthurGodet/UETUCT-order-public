/*
@author Arthur Godet <arth.godet@gmail.com>
@since 21/01/2020
*/
package constraint;

import data.Factory;
import data.InstanceUETUCT;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.nary.alldifferent.PropAllDiffAC;
import org.chocosolver.solver.constraints.nary.alldifferent.PropAllDiffBC;
import org.chocosolver.solver.constraints.nary.alldifferent.PropAllDiffInst;
import org.chocosolver.solver.constraints.nary.channeling.PropInverseChannelAC;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;

public class UETUCTModel {
    private final Model model;
    private final InstanceUETUCT instance;
    private final IntVar[] starts;
    private final Task[] tasks;
    private final BoolVar[][] assignments;
    private final IntVar makespan;
    private IntVar[] order;

    public UETUCTModel(InstanceUETUCT instance, ConfigurationUETUCT configuration, boolean withDuplication) {
        this.instance = instance;
        int n = instance.getN();
        int m = instance.getM();
        this.model = new Model();
        this.starts = model.intVarArray("starts", n, 0, n);
        this.assignments = model.boolVarMatrix(n, m);
        int maxChain = Arrays.stream(instance.deepestSuccessor()).max().getAsInt();
        this.makespan = model.intVar("makespan", Math.max((int) Math.ceil(1.0*n/m), maxChain), n);
        model.max(model.intOffsetView(this.makespan, -1), this.starts).post();

        // each task is assigned to at least one machine
        for(int i = 0; i<n; i++) {
            model.sum(assignments[i], (withDuplication ? ">=" : "="), 1).post();
        }

        // only one task at a time on each machine
        this.tasks = new Task[n];
        for(int i = 0; i<n; i++) {
            tasks[i] = model.taskVar(starts[i], 1);
        }
        for(int j = 0; j<m; j++) {
            int finalJ = j;
            IntVar[] heights = IntStream.range(0, n).mapToObj(i -> assignments[i][finalJ]).toArray(BoolVar[]::new);
            model.cumulative(tasks, heights, model.intVar(1)).post();
        }

        // constraint precedence
        for(int i = 0; i<n; i++) {
            int[] succ = instance.getSucc()[i];
            BoolVar[] directSucc = model.boolVarArray(succ.length);
            for(int k = 0; k<succ.length; k++) {
                model.arithm(starts[succ[k]], "-", starts[i], ">=", 1).post();
                model.arithm(starts[succ[k]], "-", starts[i], "=", 1).reifyWith(directSucc[k]);
                for(int j = 0; j<m; j++) {
                    model.ifThen(
                        directSucc[k],
                        model.arithm(assignments[i][j], ">=", assignments[succ[k]][j])
                    );
                }
            }
            IntVar sumDirect = model.intVar(0,succ.length);
            model.sum(directSucc, "=", sumDirect).post();
            model.sum(assignments[i], ">=", sumDirect).post();
        }

        if(
            configuration == ConfigurationUETUCT.ORDER
                || configuration == ConfigurationUETUCT.ORDER_ADAPTED
        ) {
            this.order = model.intVarArray("order", n, 0, n - 1);
            IntVar[] indexes = model.intVarArray("indexes", order.length, 0, order.length - 1);
            model.post(
                new Constraint(
                    "OrderCstr",
                    new PropOrderUETUCT(order, starts, assignments, instance.getPred()),
                    new PropInverseChannelAC(order, indexes, 0, 0),
                    new PropAllDiffInst(order),
                    new PropAllDiffAC(order, true),
                    new PropAllDiffInst(indexes),
                    new PropAllDiffBC(indexes)
                )
            );
            int[][] ancestors = instance.getAncestors();
            for(int i = 0; i < ancestors.length; i++) {
                for(int j = 0; j < ancestors[i].length; j++) {
                    model.arithm(indexes[ancestors[i][j]], "<", indexes[i]).post();
                }
            }
        }
        setSearch(configuration);

        // set objective as minimizing C
        model.setObjective(false, makespan);
    }

    private void setSearch(ConfigurationUETUCT configurationUETUCT) {
        if (ConfigurationUETUCT.NAIVE == configurationUETUCT) {
            ArrayList<IntVar> list = new ArrayList<>();
            for(int i = 0; i < starts.length; i++) {
                list.add(starts[i]);
                list.addAll(Arrays.asList(assignments[i]));
            }
            model.getSolver().setSearch(
                Search.inputOrderLBSearch(list.toArray(new IntVar[0]))
            );
        } else if (ConfigurationUETUCT.ORDER == configurationUETUCT) {
            model.getSolver().setSearch(
                Search.intVarSearch(
                    new InputOrder<>(model),
                    var -> {
                        int val = var.getLB();
                        for(int v = var.getLB(); v <= var.getUB(); v = var.nextValue(v)) {
                            if(starts[v].getLB() < starts[val].getLB()) {
                                val = v;
                            }
                        }
                        return val;
                    },
                    order
                )
            );
        } else if (ConfigurationUETUCT.ORDER_ADAPTED == configurationUETUCT) {
            int[] deepestSuccessor = instance.deepestSuccessor();
            model.getSolver().setSearch(
                Search.intVarSearch(
                    new InputOrder<>(model),
                    var -> {
                        int val = var.getLB();
                        for(int v = var.getLB(); v <= var.getUB(); v = var.nextValue(v)) {
                            if(deepestSuccessor[v] > deepestSuccessor[val]) {
                                val = v;
                            }
                        }
                        return val;
                    },
                    order
                )
            );
        } else {
            throw new UnsupportedOperationException("Configuration ("+configurationUETUCT+") not supported for search");
        }
    }

    public Model getModel() {
        return model;
    }

    public static String toString(Solver solver, boolean finalStats) {
        return (finalStats ?
                solver.getMeasures().getTimeToBestSolutionInNanoSeconds() :
                solver.getMeasures().getTimeCountInNanoSeconds()
        ) / 1000000 + ";"
            + solver.getBestSolutionValue().intValue() + ";"
            + solver.getNodeCount() + ";"
            + solver.getBackTrackCount() + ";"
            + solver.getFailCount() + ";";
    }

    public static void main(String[] args) {
        ConfigurationUETUCT configuration = ConfigurationUETUCT.valueOf(args[0]);
        long timeLimitInMilliseconds = Long.parseLong(args[1]) * 60000;
        InstanceUETUCT instance = Factory.fromFile(args[2], InstanceUETUCT.class);
        UETUCTModel uetuctModel = new UETUCTModel(instance, configuration, true);
        Solver solver = uetuctModel.getModel().getSolver();

        solver.limitTime(timeLimitInMilliseconds);
        while(solver.solve()) {
            System.out.println(toString(solver, false));
        }
        System.out.println(
            instance.getName() + ";"
                + solver.getTimeCountInNanoSeconds() / 1000000 + ";"
                + toString(solver, true)
        );
    }
}
