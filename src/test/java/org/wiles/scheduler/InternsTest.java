package org.wiles.scheduler;

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.wiles.scheduler.boolvars.SchedulerSolverBoolVars;
import org.wiles.scheduler.boolvars.TableBoolVars;
import org.wiles.scheduler.intvars.SchedulerSolverIntVars;
import org.wiles.scheduler.intvars.TableIntVars;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class InternsTest {

    static {
        Loader.loadNativeLibraries();
    }

    final int numInterns = 4;
    final int numDays = 7;


    Faker faker = new Faker();

    @Test
    public void internalSchedule() {
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

    @Test
    public void internalBoolVarsSchedule() {
        IntPredicate isWeekEndOfPublicHoliday = day -> {
            Calendar instance = Calendar.getInstance();
            instance.set(2024, Calendar.JULY, day + 1);
            int i = instance.get(Calendar.DAY_OF_WEEK);
            return i < 2 || i > 6;
        };


        var table = new TableBoolVars(Stream.generate(() ->
                faker.name().lastName()
        ).limit(numInterns).collect(Collectors.toList()), numDays, isWeekEndOfPublicHoliday);

        SchedulerSolverBoolVars schedulerSolverIntVars = new SchedulerSolverBoolVars(table);


        CpSolverSolutionCallback onSolutionCallback = new CpSolverSolutionCallback() {
            @Override
            public void onSolutionCallback() {
//                System.out.println(solver.getSolutionInfo());
                System.out.println("on solution callback");
                Arrays.stream(table.getDayRange()).mapToObj(table::getDay).forEach(this::showDayOn);
            }

            private void showDayOn(Literal[] day) {
                Arrays.stream(day).filter(a -> value(a) == 1).map(BoolVar.class::cast).forEach(x -> System.out.println(x.getName()));
            }
        };
        CpSolverStatus status = schedulerSolverIntVars.solve(onSolutionCallback);
        assertTrue(status == CpSolverStatus.FEASIBLE || status == CpSolverStatus.OPTIMAL, "No solution");


    }
}
