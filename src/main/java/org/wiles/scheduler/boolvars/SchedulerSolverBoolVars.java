package org.wiles.scheduler.boolvars;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

public class SchedulerSolverBoolVars {

    static {
        Loader.loadNativeLibraries();
    }

    private final TableBoolVars table;


    public SchedulerSolverBoolVars(TableBoolVars table) {
        this.table = table;
    }

    public CpSolverStatus solve(CpSolverSolutionCallback onSolutionCallback) {


        //setup week days
        table.getWeekDays().forEach(day -> {
            // exactly one intern on call on week days
            table.getModel().addEquality(LinearExpr.sum(table.getDay(day, TableBoolVars.Shift.WEEK_DAY)), 1);
            // no interns on week end or short calls on week days
            table.getModel().addEquality(LinearExpr.sum(table.getDay(day, TableBoolVars.Shift.WEEKEND_SHORTCALL, TableBoolVars.Shift.WEEKEND)), 0);
        });

        table.getWeekEndDays().forEach(day -> {
            // weekend shortcall + weekend call = 2 (short call and weekend calls covered)
            table.getModel().addEquality(LinearExpr.sum(table.getDay(day, TableBoolVars.Shift.WEEKEND_SHORTCALL, TableBoolVars.Shift.WEEKEND)), 2);
            table.getModel().addEquality(LinearExpr.sum(table.getDay(day, TableBoolVars.Shift.WEEK_DAY)), 0);
        });

        // at most one shift per day per intern
        table.getInternDayMap().asMap().values().stream().map(x -> x.toArray(new Literal[0])).forEach(internDay -> table.getModel().addLessOrEqual(LinearExpr.sum(internDay), 1));

//        addGaps(table);
        distributeShifts(table);
//        table.getModel().addImplication()

        prohibitConsecutiveShifts();

        CpSolver solver = new CpSolver();
        solver.getParameters().setLinearizationLevel(0);
        // Tell the solver to enumerate all solutions.
//        solver.getParameters().setEnumerateAllSolutions(true);


        return solver.solve(table.getModel(), onSolutionCallback);


    }

    private void prohibitConsecutiveShifts() {
        IntStream.range(0, table.getDayRange().length - 1).forEach(day -> {
            IntStream.range(0, table.getInterns().size()).forEach(internIndex -> {
                Collection<Literal> interns = table.getInterns(internIndex, day);

                interns.forEach(todayShift -> {
                    table.getInterns(internIndex, day + 1).forEach(tomorrowShift -> {
                        table.getModel().addImplication(todayShift, tomorrowShift.not());
                    });
                    /*if (day < table.getDayRange().length - 2) {
                        table.getInterns(internIndex, day + 2).forEach(tomorrowShift -> {
                            table.getModel().addImplication(todayShift, tomorrowShift.not());
                        });
                    }*/

                });

            });
        });
    }


    private void distributeShifts(TableBoolVars table) {
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
