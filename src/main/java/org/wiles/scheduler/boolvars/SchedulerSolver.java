package org.wiles.scheduler.boolvars;

import com.google.common.collect.Multimap;
import com.google.ortools.Loader;
import com.google.ortools.sat.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.stream.IntStream.range;

public class SchedulerSolver {

    static {
        Loader.loadNativeLibraries();
    }

    private final Table table;


    public SchedulerSolver(Table table) {
        this.table = table;
    }

    public CpSolverStatus solve(CpSolverSolutionCallback onSolutionCallback) {


        //

        // at most one shift per day per intern
        table.getWeekDays().mapToObj(x -> table.getDay(x, Table.Shift.WEEK_DAY)).forEach(set -> table.getModel().addExactlyOne(set));
        table.getWeekEndDays().mapToObj(x -> table.getDay(x, Table.Shift.WEEKEND)).forEach(set -> table.getModel().addExactlyOne(set));
        table.getWeekEndDays().mapToObj(x -> table.getDay(x, Table.Shift.WEEKEND_SHORTCALL)).forEach(set -> table.getModel().addExactlyOne(set));


        // make sure that onle one intern is on call for one day (and not on call twice on the same day)
        //noinspection ALL
        range(0, table.getInterns().size()).forEach(internIndex -> {
            Arrays.stream(table.getDayRange()).mapToObj(weekendDay ->
                    table.getInterns(internIndex, weekendDay)
            ).forEach(elements -> table.getModel().addAtMostOne(elements));
        });
//        addGaps(table);
        distributeShifts(table);
//        table.getModel().addImplication()

        prohibitConsecutiveShifts();

        addLeave();
        addRequests();

        CpSolver solver = new CpSolver();
        solver.getParameters().setLinearizationLevel(0);
        // Tell the solver to enumerate all solutions.
//        solver.getParameters().setEnumerateAllSolutions(true);


        return solver.solve(table.getModel(), onSolutionCallback);


    }

    /**
     * adds the requests to the model
     */
    private void addRequests() {

        LinearExprBuilder obj = LinearExpr.newBuilder();
        for (int i : table.allInterns) {
            for (int d : table.allDays) {
                for (int s : table.allShifts) {
                    Literal shift = table.table[i][d][s];
                    if (shift != null) {
                        if (table.getRequestMap().get(i).contains(d)) {
                            System.out.printf("\nadded request to be OFF for %s on days %s", i, shift);
                            obj.addTerm(shift.not(), 1);
                        } else {
                            System.out.printf("\nadded request to be ON for %s on days %s", i, shift);
                            obj.addTerm(shift, 0);
                        }
                    }
                }
            }
        }
        System.out.println(obj);
        table.getModel().maximize(obj);
    }

    private void addRequests0() {
        Multimap<Integer, Integer> leaveMap = table.getRequestMap();
        LinearExprBuilder builder = LinearExpr.newBuilder();

        leaveMap.asMap().forEach((internIdx, days) -> {
            days.stream().map(day -> table.getInterns(internIdx, day)).
                    forEach(shifts -> {
                        shifts.forEach(shift -> {
                            builder.addTerm(shift.not(), 1);
                        });
                        System.out.printf("\nadded request to be off for %s on days %s", table.getInterns().get(internIdx), shifts);
                    });
        });
        table.getModel().maximize(builder);
    }

    private void addLeave() {
        Multimap<Integer, Integer> leaveMap = table.getLeaveMap();
        leaveMap.asMap().forEach((internIdx, days) -> {
            days.stream().map(day -> table.getInterns(internIdx, day)).
                    forEach(shifts -> {
                        LinearExprBuilder builder = LinearExpr.newBuilder();
                        builder.addSum(shifts.toArray(new Literal[0]));
                        System.out.printf("\nadded leave for %s on days %s", table.getInterns().get(internIdx), shifts);
                        table.getModel().addEquality(builder, 0);
                    });
        });
    }

    private void prohibitConsecutiveShifts() {
        range(0, table.getDayRange().length - 1).forEach(day -> {
            range(0, table.getInterns().size()).forEach(internIndex -> {
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


    private void distributeShifts(Table table) {
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


        List<LinearExprBuilder> list = range(0, table.getInterns().size()).mapToObj(internIndex -> {
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
