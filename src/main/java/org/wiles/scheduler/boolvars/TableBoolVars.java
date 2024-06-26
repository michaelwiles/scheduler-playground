package org.wiles.scheduler.boolvars;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.Literal;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TableBoolVars {

    private final CpModel model = new CpModel();
    private final List<String> interns;
    private final IntPredicate isWeekEndOrPublicHoliday;


    private final int[] dayRange;
    private final Multimap<Integer, Literal> dayMap = ArrayListMultimap.create();
    private final Multimap<Integer, Literal> internsMap = ArrayListMultimap.create();
    private final Multimap<Pair<Integer, Shift>, Literal> dayShiftMap = ArrayListMultimap.create();
    // Pair<InternId, Day> -> IntVar
    private final Multimap<Pair<Integer, Integer>, Literal> internDayMap = ArrayListMultimap.create();
    private final int days;


    public enum Shift {
        WEEK_DAY, WEEKEND, WEEKEND_SHORTCALL
    }


    public TableBoolVars(List<String> interns, int days, IntPredicate isWeekEndOrPublicHoliday) {
        this.interns = interns;
        this.isWeekEndOrPublicHoliday = isWeekEndOrPublicHoliday;
        int[] internsRange = IntStream.range(0, this.interns.size()).toArray();
        this.dayRange = IntStream.range(0, days).toArray();
        this.days = days;
//
//
//        shifts[n][d][s] = model.newIntVar(0L, 1L, "shifts_n" + n + "d" + d + "s" + s);

        for (int i : internsRange) {
            for (int d : dayRange) {
                for (Shift s : Shift.values()) {
                    BoolVar intVar = model.newBoolVar("day=" + d + " " + this.interns.get(i) + " shift=" + s);
                    dayMap.put(d, intVar);
                    dayShiftMap.put(Pair.of(d, s), intVar);
                    internsMap.put(i, intVar);
                    internDayMap.put(Pair.of(i, d), intVar);
                }
            }
        }
    }

    public IntStream getWeekDays() {
        return IntStream.range(0, days).filter(isWeekEndOrPublicHoliday.negate());
    }

    public IntStream getWeekEndDays() {
        return IntStream.range(0, days).filter(isWeekEndOrPublicHoliday);
    }


    public int[] getDayRange() {
        return dayRange;
    }

    /**
     * Get all Cells for a specific day (all interns and all shifts)
     *
     */
    public Literal[] getDay(int i) {
        return this.dayMap.get(i).toArray(new Literal[0]);
    }

    public Collection<Literal> getInterns(int internIndex) {
        return internsMap.get(internIndex);
    }

    public Collection<Literal> getInterns(int internIndex, int day) {
        return internDayMap.get(Pair.of(internIndex, day));
    }

    public Multimap<Pair<Integer, Integer>, Literal> getInternDayMap() {
        return internDayMap;
    }

    public Literal[] getDay(int i, Shift... shift) {
        Stream<Literal> literalStream = Arrays.stream(shift).flatMap(s -> this.dayShiftMap.get(Pair.of(i, s)).stream());
        return literalStream.toArray(Literal[]::new);
    }


    public List<String> getInterns() {
        return interns;
    }

    public CpModel getModel() {
        return model;
    }


}
