# scheduler-playground

This is a project to work on generating the intern on call schedule for QNRH hosptial in Northen KZN South Africa.

## Getting Started

Run the `InternsTest.internalBoolVarsSchedule`

I started using IntVars and then realised that I could migrate to boolvars and things are a little easier.

It is generating schedules with the correct shifts.

1. There are weekday shifts (16 hours)
2. There are weekend shifts (24 hours)
3. and there are also short calls on the weekend (6 hours).

So given the set of interns and the dates we need a roster which schedules the interns on call.

Apart from the basics, each shift with only one intern and the correct interns on the correct days, so far we have following extra conditions:

1. Tries to allocate a similar amount of shifts to each intern. It does this by making sure each intern does the minimum number and does not exceed the maxmium number
2. It also does not schedule interns on consecutive days

## Further Required Constraints/Conditions

* requests - interns request specific weekends/days off
* If a person is scheduled on friday they should always be scheduled for short call on Sunday - this is because they have to be in town anyway so they might as well get that weekend short call out of the way.
* distribute the calls across the month as much as possible
* weigth the various calls - ensure that each person works a similar amouhnt of short calls, weekend calls and week days calls such that their total hours add up to a similar amount

## Comments

Java is not a good candidate for doing this work. I've half a mind to migrate to using python. Java is _very_ verbose and not intuitive for this kind of thing.

