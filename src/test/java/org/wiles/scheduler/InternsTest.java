package org.wiles.scheduler;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.wiles.scheduler.boolvars.SchedulerSolver;
import org.wiles.scheduler.boolvars.Table;
import org.wiles.scheduler.intvars.SchedulerSolverIntVars;
import org.wiles.scheduler.intvars.TableIntVars;

import java.util.Arrays;
import java.util.Calendar;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InternsTest {

    static {
        Loader.loadNativeLibraries();
    }

    final int numInterns = 4;
    final int numDays = 7;


    Faker faker = new Faker();

    @Test
    public void internalScheduleWithIntVars() {
        var table = new TableIntVars(Stream.generate(() ->
                faker.name().lastName()
        ).limit(numInterns).collect(Collectors.toList()), numDays);

        SchedulerSolverIntVars schedulerSolverIntVars = new SchedulerSolverIntVars(table);


        CpSolverSolutionCallback onSolutionCallback = new CpSolverSolutionCallback() {
            @Override
            public void onSolutionCallback() {
//                System.out.println(solver.getSolutionInfo());
                System.out.println("on solution callback");
                Arrays.stream(table.getDayRange()).mapToObj(table::getDay).forEach(this::showDayOn);
            }

            private void showDayOn(IntVar[] day) {
                Arrays.stream(day).filter(a -> value(a) == 1).forEach(x -> System.out.println(x.getName()));
            }
        };
        schedulerSolverIntVars.solve(onSolutionCallback);
    }

    private final IntPredicate isWeekEndOfPublicHoliday = day -> {
        Calendar instance = Calendar.getInstance();
        instance.set(2024, Calendar.JULY, day + 1);
        int i = instance.get(Calendar.DAY_OF_WEEK);
        return i < 2 || i > 6;
    };

    @Test
    public void scheduleWithLeave() {

    }

    @Test
    public void internalSchedule() {
        var table = new Table(Stream.generate(() ->
                faker.name().lastName()
        ).limit(7).collect(Collectors.toList()), 31, isWeekEndOfPublicHoliday);

        table.addLeaveDays(0, 0, 1, 2, 3, 4, 5, 6, 7);

        table.addLeaveDays(0, 8, 9, 10, 11, 12, 13, 14);

        SchedulerSolver schedulerSolverIntVars = new SchedulerSolver(table);


        CpSolverSolutionCallback onSolutionCallback = new CpSolverSolutionCallback() {
            @Override
            public void onSolutionCallback() {
//                System.out.println(solver.getSolutionInfo());
                System.out.println("on solution callback");
                Arrays.stream(table.getDayRange()).mapToObj(table::getDay).forEach(this::showDayOn);

                Arrays.stream(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14}).forEach(day -> {
                    Stream<Long> actual = table.getInterns(0, day).stream().map(this::value);
                    assertThat(actual).allMatch(x -> x == 0);
                });
            }

            private void showDayOn(Literal[] day) {
                Arrays.stream(day).filter(a -> value(a) == 1).map(BoolVar.class::cast).forEach(x -> System.out.println(x.getName()));
            }
        };
        CpSolverStatus status = schedulerSolverIntVars.solve(onSolutionCallback);
        assertTrue(status == CpSolverStatus.FEASIBLE || status == CpSolverStatus.OPTIMAL, "No solution");


    }
}
