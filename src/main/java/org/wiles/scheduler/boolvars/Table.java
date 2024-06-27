package org.wiles.scheduler.boolvars;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
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

public class Table {

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
    private final int numberOfShifts;
    private final Multimap<Integer, Integer> leaveMap = ArrayListMultimap.create();

    public int getNumberOfShifts() {
        return this.numberOfShifts;
    }


    public enum Shift {
        WEEK_DAY, WEEKEND, WEEKEND_SHORTCALL
    }


    public Table(List<String> interns, int days, IntPredicate isWeekEndOrPublicHoliday) {
        this.interns = interns;
        this.isWeekEndOrPublicHoliday = isWeekEndOrPublicHoliday;
        int[] internsRange = IntStream.range(0, this.interns.size()).toArray();
        this.dayRange = IntStream.range(0, days).toArray();
        this.days = days;
//
//
//        shifts[n][d][s] = model.newIntVar(0L, 1L, "shifts_n" + n + "d" + d + "s" + s);

        int shifts = 0;
        for (int d : dayRange) {
            Shift[] shiftTypes;
            if (isWeekEndOrPublicHoliday.negate().test(d)) {
                shiftTypes = new Shift[]{Shift.WEEK_DAY};
            } else {
                shiftTypes = new Shift[]{Shift.WEEKEND, Shift.WEEKEND_SHORTCALL};
            }
            for (Shift s : shiftTypes) {
                shifts++;
                for (int i : internsRange) {
                    BoolVar intVar = model.newBoolVar("day=" + d + " " + this.interns.get(i) + " shift=" + s);
                    dayMap.put(d, intVar);
                    dayShiftMap.put(Pair.of(d, s), intVar);
                    internsMap.put(i, intVar);
                    internDayMap.put(Pair.of(i, d), intVar);
                }
            }
        }
        this.numberOfShifts = shifts;
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

    public void addLeaveDays(int internIdx, int... dayIndices) {
        leaveMap.putAll(internIdx, Ints.asList(dayIndices));
    }

    public Multimap<Integer, Integer> getLeaveMap() {
        return leaveMap;
    }


}
