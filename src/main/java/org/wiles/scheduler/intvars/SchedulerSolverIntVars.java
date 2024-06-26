package org.wiles.scheduler.intvars;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.util.List;
import java.util.stream.IntStream;

public class SchedulerSolverIntVars {

    static {
        Loader.loadNativeLibraries();
    }

    private final TableIntVars table;


    public SchedulerSolverIntVars(TableIntVars table) {
        this.table = table;
    }

    public void solve(CpSolverSolutionCallback onSolutionCallback) {


        //setup week days
        IntStream.range(0, 5).forEach(day -> {
            // exactly one intern on call on week days
            table.getModel().addEquality(LinearExpr.sum(table.getDay(day, TableIntVars.Shift.WEEK_DAY)), 1);
            // no interns on week end or short calls on week days
            table.getModel().addEquality(LinearExpr.sum(table.getDay(day, TableIntVars.Shift.WEEKEND_SHORTCALL, TableIntVars.Shift.WEEKEND)), 0);
        });

        IntStream.range(5, 7).forEach(day -> {
            // weekend shortcall + weekend call = 2 (short call and weekend calls covered)
            table.getModel().addEquality(LinearExpr.sum(table.getDay(day, TableIntVars.Shift.WEEKEND_SHORTCALL, TableIntVars.Shift.WEEKEND)), 2);
            table.getModel().addEquality(LinearExpr.sum(table.getDay(day, TableIntVars.Shift.WEEK_DAY)), 0);
        });

        // at most one shift per day per intern
        table.getInternDayMap().asMap().values().stream().map(x -> x.toArray(new IntVar[0])).forEach(internDay -> table.getModel().addLessOrEqual(LinearExpr.sum(internDay), 1));

//        addGaps(table);
        distributeShifts(table);
//        table.getModel().addImplication()

       /* Arrays.stream(table.getDayRange()).forEach(day -> {
            IntStream.range(0, table.getInterns().size() - 1).forEach(internIndex -> {
                table.getInterns(internIndex, day);
            });
        });*/


        CpSolver solver = new CpSolver();
        solver.getParameters().setLinearizationLevel(0);
        // Tell the solver to enumerate all solutions.
//        solver.getParameters().setEnumerateAllSolutions(true);


        CpSolverStatus status = solver.solve(table.getModel(), onSolutionCallback);


    }


    private void distributeShifts(TableIntVars table) {
        // Try to distribute the shifts evenly, so that each nurse works
        // minShiftsPerNurse shifts. If this is not possible, because the total
        // number of shifts is not divisible by the number of nurses, some nurses will
        // be assigned one more shift.
        int minShiftsPerNurse = 9 / table.getInterns().size();
        int maxShiftsPerNurse;
        if (9 % table.getInterns().size() == 0) {
            maxShiftsPerNurse = minShiftsPerNurse;
        } else {
            maxShiftsPerNurse = minShiftsPerNurse + 1;
        }
        System.out.printf("min = %s", minShiftsPerNurse);
        System.out.printf("max = %s", maxShiftsPerNurse);


        List<LinearExprBuilder> list = IntStream.range(0, table.getInterns().size()).mapToObj(internIndex -> {
            LinearExprBuilder builder = LinearExpr.newBuilder();
            table.getInterns(internIndex).forEach(builder::add);
            return builder;
        }).toList();

        list.forEach(builder -> table.getModel().addLinearConstraint(builder, minShiftsPerNurse, maxShiftsPerNurse));

        IntVar minShifts = table.getModel().newIntVar(0, maxShiftsPerNurse, "minShiftsPerIntern");
        LinearArgument[] array = list.stream().map(LinearExprBuilder::build).toArray(LinearArgument[]::new);
        table.getModel().addMinEquality(minShifts, array);
        table.getModel().maximize(minShifts);


    }

}
