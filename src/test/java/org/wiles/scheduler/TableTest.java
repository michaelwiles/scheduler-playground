package org.wiles.scheduler;

import static org.assertj.core.api.Assertions.*;

import com.google.ortools.Loader;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.wiles.scheduler.boolvars.TableBoolVars;

import java.util.Calendar;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TableTest {

    static {
        Loader.loadNativeLibraries();
    }

    private final Faker faker = new Faker();


    @Test
    public void GetWeekDaysJuly2024() {

        IntPredicate isWeekEndOfPublicHoliday = day -> {
            Calendar instance = Calendar.getInstance();
            instance.set(2024, Calendar.JULY, day + 1);
            int i = instance.get(Calendar.DAY_OF_WEEK);
            return i < 2 || i > 6;
        };

        Function<Integer, TableBoolVars> generator = days -> new TableBoolVars(Stream.generate(() ->
                faker.name().lastName()
        ).limit(1).collect(Collectors.toList()), days, isWeekEndOfPublicHoliday);


        assertThat(generator.apply(10).getWeekDays()).containsOnly(/*week*/0, 1, 2, 3, 4, /*week*/ 7, 8, 9);
        assertThat(generator.apply(15).getWeekDays()).containsOnly(0, 1, 2, 3, 4, /*week*/ 7, 8, 9, 10, 11,/*week*/ 14);
        assertThat(generator.apply(30).getWeekDays()).containsOnly(/*week*/0, 1, 2, 3, 4, /*week*/ 7, 8, 9, 10, 11,/*week*/  14, 15, 16, 17, 18,/*week*/ 21, 22, 23, 24, 25, /*week*/ 28, 29/*week*/);
        assertThat(generator.apply(21).getWeekDays()).containsOnly(/*week*/ 0, 1, 2, 3, 4, /*week*/ 7, 8, 9, 10, 11,/*week*/ 14, 15, 16, 17, 18);

    }

    @Test
    public void GetWeekEndsJuly2024() {

        IntPredicate isWeekEndOfPublicHoliday = day -> {
            Calendar instance = Calendar.getInstance();
            instance.set(2024, Calendar.JULY, day + 1);
            int i = instance.get(Calendar.DAY_OF_WEEK);
            return i < 2 || i > 6;
        };

        Function<Integer, TableBoolVars> generator = days -> new TableBoolVars(Stream.generate(() ->
                faker.name().lastName()
        ).limit(1).collect(Collectors.toList()), days, isWeekEndOfPublicHoliday);

        assertThat(generator.apply(10).getWeekEndDays()).containsOnly(5, 6);
        assertThat(generator.apply(15).getWeekEndDays()).containsOnly(5, 6, 12, 13);
        assertThat(generator.apply(30).getWeekEndDays()).containsOnly(5, 6, 12, 13, 19, 20, 26, 27);
        assertThat(generator.apply(21).getWeekEndDays()).containsOnly(5, 6, 12, 13, 19, 20);

    }

    @Test
    public void GetWeekDaysFebruary2024() {

        IntPredicate isWeekEndOfPublicHoliday = day -> {
            Calendar instance = Calendar.getInstance();
            instance.set(2024, Calendar.FEBRUARY, day + 1);
            int i = instance.get(Calendar.DAY_OF_WEEK);
            return i < 2 || i > 6;
        };

        Function<Integer, TableBoolVars> generator = days -> new TableBoolVars(Stream.generate(() ->
                faker.name().lastName()
        ).limit(1).collect(Collectors.toList()), days, isWeekEndOfPublicHoliday);


        assertThat(generator.apply(10).getWeekDays()).containsOnly(/*week*/0, 1,  /*week*/ 4, 5, 6, 7, 8);
        assertThat(generator.apply(15).getWeekDays()).containsOnly(0, 1,  /*week*/ 4, 5, 6, 7, 8,/*week*/ 11, 12, 13, 14);
        assertThat(generator.apply(30).getWeekDays()).containsOnly(/*week*/0, 1, /*week*/ 4, 5, 6, 7, 8,/*week*/11, 12, 13, 14, 15,/*week*/18, 19, 20, 21, 22,/*week*/25, 26, 27, 28, 29);
        assertThat(generator.apply(21).getWeekDays()).containsOnly(/*week*/ 0, 1, /*week*/ 4, 5, 6, 7, 8,/*week*/11, 12, 13, 14, 15,/*week*/18, 19, 20);

    }

    @Test
    public void GetWeekEndsFebruary2024() {

        IntPredicate isWeekEndOfPublicHoliday = day -> {
            Calendar instance = Calendar.getInstance();
            instance.set(2024, Calendar.FEBRUARY, day + 1);
            int i = instance.get(Calendar.DAY_OF_WEEK);
            return i < 2 || i > 6;
        };

        Function<Integer, TableBoolVars> generator = days -> new TableBoolVars(Stream.generate(() ->
                faker.name().lastName()
        ).limit(1).collect(Collectors.toList()), days, isWeekEndOfPublicHoliday);

        assertThat(generator.apply(10).getWeekEndDays()).containsOnly(2, 3, 9);
        assertThat(generator.apply(15).getWeekEndDays()).containsOnly(2, 3, 9, 10);
        assertThat(generator.apply(30).getWeekEndDays()).containsOnly(2, 3, 9, 10, 16, 17, 23, 24);
        assertThat(generator.apply(21).getWeekEndDays()).containsOnly(2, 3, 9, 10, 16, 17);

    }


}
