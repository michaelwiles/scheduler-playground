package org.wiles.scheduler.intvars;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.ortools.sat.CpModel;
import com.google.ortools.sat.IntVar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TableIntVars {

    private final CpModel model = new CpModel();
    private final List<String> interns;


    private final int[] dayRange;
    private final Multimap<Integer, IntVar> dayMap = ArrayListMultimap.create();
    private final Multimap<Integer, IntVar> internsMap = ArrayListMultimap.create();
    private final Multimap<Pair<Integer, Shift>, IntVar> dayShiftMap = ArrayListMultimap.create();
    // Pair<InternId, Day> -> IntVar
    private final Multimap<Pair<Integer, Integer>, IntVar> internDayMap = ArrayListMultimap.create();

    public enum Shift {
        WEEK_DAY, WEEKEND, WEEKEND_SHORTCALL
    }


    public int[] getDayRange() {
        return dayRange;
    }

    public TableIntVars(List<String> interns, int days) {
        this.interns = interns;
        int[] internsRange = IntStream.range(0, this.interns.size()).toArray();
        this.dayRange = IntStream.range(0, days).toArray();
//
//
//        shifts[n][d][s] = model.newIntVar(0L, 1L, "shifts_n" + n + "d" + d + "s" + s);

        for (int i : internsRange) {
            for (int d : dayRange) {
                for (Shift s : Shift.values()) {
                    IntVar intVar = model.newIntVar(0L, 1L, " day=" + d + " " + this.interns.get(i) + " shift=" + s);
                    dayMap.put(d, intVar);
                    dayShiftMap.put(Pair.of(d, s), intVar);
                    internsMap.put(i, intVar);
                    internDayMap.put(Pair.of(i, d), intVar);
                }
            }
        }
    }

    /**
     * Get all Cells for a specific day (all interns and all shifts)
     *
     */
    public IntVar[] getDay(int i) {
        return this.dayMap.get(i).toArray(new IntVar[0]);
    }

    public Collection<IntVar> getInterns(int internIndex) {
        return internsMap.get(internIndex);
    }

    public Multimap<Pair<Integer, Integer>, IntVar> getInternDayMap() {
        return internDayMap;
    }

    public IntVar[] getDay(int i, Shift... shift) {
        Stream<IntVar> intVarStream = Arrays.stream(shift).flatMap(s -> this.dayShiftMap.get(Pair.of(i, s)).stream());
        return intVarStream.toArray(IntVar[]::new);
    }


    public List<String> getInterns() {
        return interns;
    }

    public CpModel getModel() {
        return model;
    }


}
