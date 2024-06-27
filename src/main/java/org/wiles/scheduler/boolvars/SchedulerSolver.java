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


    public CpSolver solve() {


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

        CpSolver solver = new CpSolver();
        addRequests();
        solver.getParameters().setLinearizationLevel(0);

        // Tell the solver to enumerate all solutions.
//        solver.getParameters().setEnumerateAllSolutions(true);


        return solver;


    }

/*    private void addRequests(CpSolver solver) {
        BoolVar[][] isOff = new BoolVar[table.allInterns.length][table.days];  // Auxiliary variable representing if nurse 'n' is off on day 'd'

        LinearExprBuilder objectiveExpressionBuilder = LinearExpr.newBuilder();
        for (int i : table.allInterns) {
            for (int d : table.allDays) {
                LinearExprBuilder shiftSum = LinearExpr.newBuilder();
                for (int s : table.allShifts) {
                    var shift = table.shifts[i][d][s];
                    if (shift != null) {
                        shiftSum.add(table.shifts[i][d][s]);
                    }
                }

                Constraint constraint = table.getModel().addEquality(shiftSum, isOff[i][d]);

                // Assume offRequests[n][d] is the `reward` for nurse n being off on day d
                table.getModel().addObjectiveTerm(offRequests[n][d], isOff[n][d]);
                //IntVar shiftSum = LinearExpr.sum(table.allShifts.stream().map(s -> table.shifts[i][d][s]).collect(Collectors.toList())).toVar();
                // isOff[n][d] is 1 if nurse 'n' is off on day 'd', 0 otherwise
                isOff[i][d] = table.getModel().newBoolVar(String.format("isOff[%s][%s]", i, d));
                // isOff[n][d] is 1 if nurse 'n' is off on day 'd', 0 otherwise

            }
        }
        model.maximize(objectiveExpressionBuilder.build());
    }*/

    /**
     * adds the requests to the model
     */
    private void addRequests() {


        var model = table.getModel();
        LinearExprBuilder obj = LinearExpr.newBuilder();
        for (int i : table.allInterns) {
            for (int d : table.allDays) {
                LinearExprBuilder dailyShiftExpr = LinearExpr.newBuilder();
                for (int s : table.allShifts) {
                    Literal shift = table.shifts[i][d][s];
                    if (shift != null) {
                        dailyShiftExpr.addTerm(shift, 1);
                    }
                }
                BoolVar off = model.newBoolVar(String.format("isOff[%s][%s]", table.getInterns().get(i), d));
                table.isOff[i][d] = off;
                table.isOffList.add(off);
                LinearExpr inversionExpression = LinearExpr.sum(new LinearArgument[]{LinearExpr.constant(1), LinearExpr.newBuilder().addTerm(dailyShiftExpr, -1)});
                table.getModel().addEquality(off, inversionExpression);
                if (table.getRequestMap().containsEntry(i, d)) {
                    System.out.printf("\nadding off request for iId: %s:%s and day: %d", i, table.getInterns().get(i), d);
                    obj.add(inversionExpression);
                }
            }
        }
        table.getModel().maximize(obj);
    }

    private void addLeave() {
        Multimap<Integer, Integer> leaveMap = table.getLeaveMap();
        leaveMap.asMap().forEach((internIdx, days) -> days.stream().map(day -> table.getInterns(internIdx, day)).
                forEach(shifts -> {
                    LinearExprBuilder builder = LinearExpr.newBuilder();
                    builder.addSum(shifts.toArray(new Literal[0]));
                    System.out.printf("\nadded leave for %s on days %s", table.getInterns().get(internIdx), shifts);
                    table.getModel().addEquality(builder, 0);
                }));
    }

    private void prohibitConsecutiveShifts() {
        range(0, table.getDayRange().length - 1).forEach(day ->
                range(0, table.getInterns().size()).forEach(internIndex -> {
                    Collection<Literal> interns = table.getInterns(internIndex, day);

                    interns.forEach(todayShift -> {
                        table.getInterns(internIndex, day + 1).forEach(tomorrowShift ->
                                table.getModel().addImplication(todayShift, tomorrowShift.not()));
                /*if (day < table.getDayRange().length - 2) {
                    table.getInterns(internIndex, day + 2).forEach(tomorrowShift -> {
                        table.getModel().addImplication(todayShift, tomorrowShift.not());
                    });
                }*/

                    });

                }));
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
