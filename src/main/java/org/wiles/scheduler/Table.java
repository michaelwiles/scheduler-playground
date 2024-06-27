package org.wiles.scheduler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;
import com.google.ortools.sat.BoolVar;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import com.google.ortools.sat.Literal;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Table {

    private final CpModel model = new CpModel();
    private final List<String> interns;
    private final IntPredicate isWeekEndOrPublicHoliday;


    private final int[] dayRange;


    final Literal[][][] shifts;
    private final Multimap<Integer, Literal> dayMap = ArrayListMultimap.create();
    private final Multimap<Integer, Literal> internsMap = ArrayListMultimap.create();
    private final Multimap<Pair<Integer, Shift>, Literal> dayShiftMap = ArrayListMultimap.create();
    // Pair<InternId, Day> -> IntVar
    private final Multimap<Pair<Integer, Integer>, Literal> internDayMap = ArrayListMultimap.create();
    final int days;
    private final int numberOfShifts;
    private final Multimap<Integer, Integer> leaveMap = ArrayListMultimap.create();
    private final Multimap<Integer, Integer> requestMap = ArrayListMultimap.create();

    final int[] allInterns;// = IntStream.range(0, numNurses).toArray();
    final int[] allDays;// = IntStream.range(0, numDays).toArray();
    final int[] allShifts;// = IntStream.range(0, numShifts).toArray();
    public IntVar maxRequests;


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
        allInterns = IntStream.range(0, interns.size()).toArray();
        allDays = IntStream.range(0, days).toArray();
        allShifts = IntStream.range(0, Shift.values().length).toArray();


        int shifts = 0;
        this.shifts = new Literal[interns.size()][days][Shift.values().length];
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
                    BoolVar var = model.newBoolVar("day=" + d + " " + this.interns.get(i) + " shift=" + s);
                    dayMap.put(d, var);
                    dayShiftMap.put(Pair.of(d, s), var);
                    internsMap.put(i, var);
                    internDayMap.put(Pair.of(i, d), var);
                    this.shifts[i][d][s.ordinal()] = var;
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

    public void addRequests(int internIdx, int... leaveRequests) {
        this.requestMap.putAll(internIdx, Ints.asList(leaveRequests));
    }

    public Multimap<Integer, Integer> getLeaveMap() {
        return leaveMap;
    }

    public Multimap<Integer, Integer> getRequestMap() {
        return requestMap;
    }

}