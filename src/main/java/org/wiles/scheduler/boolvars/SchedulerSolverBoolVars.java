package org.wiles.scheduler.boolvars;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.util.Arrays;
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


        //

        // at most one shift per day per intern
        table.getWeekDays().mapToObj(x -> table.getDay(x, TableBoolVars.Shift.WEEK_DAY)).forEach(set -> table.getModel().addExactlyOne(set));
        table.getWeekEndDays().mapToObj(x -> table.getDay(x, TableBoolVars.Shift.WEEKEND)).forEach(set -> table.getModel().addExactlyOne(set));
        table.getWeekEndDays().mapToObj(x -> table.getDay(x, TableBoolVars.Shift.WEEKEND_SHORTCALL)).forEach(set -> table.getModel().addExactlyOne(set));


        // make sure that onle one intern is on call for one day (and not on call twice on the same day)
        IntStream.range(0, table.getInterns().size()).forEach(internIndex -> {
            Arrays.stream(table.getDayRange()).mapToObj(weekendDay ->
                    table.getInterns(internIndex, weekendDay)
            ).forEach(elements -> table.getModel().addAtMostOne(elements));
        });



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
        int minShiftsPerNurse = table.getNumberOfShifts() / table.getInterns().size();
        int maxShiftsPerNurse;
        if (table.getNumberOfShifts() % table.getInterns().size() == 0) {
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
