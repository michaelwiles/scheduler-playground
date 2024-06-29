package org.wiles.scheduler;

import com.google.common.collect.Multimap;
import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import org.wiles.scheduler.Table.Shift;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import java.util.stream.Stream;

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
        table.getWeekDays().mapToObj(x -> table.getDay(x, Shift.WEEK_DAY)).forEach(set -> table.getModel().addExactlyOne(set));
        table.getWeekEndDays().mapToObj(x -> table.getDay(x, Shift.WEEKEND)).forEach(set -> table.getModel().addExactlyOne(set));
        table.getWeekEndDays().mapToObj(x -> table.getDay(x, Shift.WEEKEND_SHORTCALL)).forEach(set -> table.getModel().addExactlyOne(set));


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

        addShiftEqualisation();

        CpSolver solver = new CpSolver();
        addRequests();
        solver.getParameters().setLinearizationLevel(0);

        return solver;
    }

    private void addShiftEqualisation() {
        int sum = Stream.of(Shift.values()).mapToInt(shift -> {
            List<Literal> literals = table.get(0, null, shift);
            System.out.println("shifts of type " + shift + " = " + literals.size());
            return literals.size() * shift.hours;
        }).sum();

        int hoursPerIntern = sum / table.getInterns().size();
        System.out.println("hours per intern: " + hoursPerIntern);
        CpModel model = table.getModel();

        Arrays.stream(table.allInterns).forEach(i -> {
            LinearExprBuilder builder = LinearExpr.newBuilder();

            Consumer<Shift> integerLinearExprBuilderShiftTriConsumer = (shift0) -> {
                Literal[] literals = table.getArray(i, null, shift0);
                builder.addWeightedSum(literals, LongStream.generate(() -> shift0.hours).limit(literals.length).toArray());
            };
            integerLinearExprBuilderShiftTriConsumer.accept(Shift.WEEK_DAY);
            integerLinearExprBuilderShiftTriConsumer.accept(Shift.WEEKEND);
            integerLinearExprBuilderShiftTriConsumer.accept(Shift.WEEKEND_SHORTCALL);

            IntVar intVar = model.newIntVar(0, Integer.MAX_VALUE, String.format("%s:%s hours", i, table.getInterns().get(i)));
            builder.add(-hoursPerIntern);
            model.addEquality(intVar, builder);
            table.addHoursTracking(intVar);
//            model.minimize(builder);
        });
    }

    /**
     * adds the requests to the model
     */
    private void addRequests() {
        var model = table.getModel();
        LinearExprBuilder obj = LinearExpr.newBuilder();
        table.maxRequests = model.newIntVar(0, Integer.MAX_VALUE, "max requests");

        for (int i : table.allInterns) {
            for (int d : table.allDays) {
                LinearExprBuilder dailyShiftExpr = LinearExpr.newBuilder();
                for (int s : table.allShifts) {
                    Literal shift = table.shifts[i][d][s];
                    if (shift != null) {
                        dailyShiftExpr.addTerm(shift, 1);
                    }
                }
                LinearExpr inversionExpression = LinearExpr.sum(new LinearArgument[]{LinearExpr.constant(1), LinearExpr.newBuilder().addTerm(dailyShiftExpr, -1)});
                if (table.getRequestMap().containsEntry(i, d)) {
                    System.out.printf("\nadding off request for iId: %s:%s and day: %d", i, table.getInterns().get(i), d);
                    obj.add(inversionExpression);
                }
            }
        }

        model.addEquality(table.maxRequests, obj);
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
