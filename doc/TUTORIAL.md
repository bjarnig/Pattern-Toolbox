# Using the Pattern Toolbox

## A tutorial

---

## Contents

- [Introduction](#introduction)
- [Basics](#basics)
- [Tutorial 1: Making data sections](#tutorial-1-making-data-sections)
- [Tutorial 2: Combining sections](#tutorial-2-combining-sections)
- [Tutorial 3: Using stockpiles, making choices](#tutorial-3-using-stockpiles-making-choices)
- [Tutorial 4: Shapes](#tutorial-4-shapes)
- [Tutorial 5: Masks](#tutorial-5-masks)
- [Tutorial 6: Chords, rests, and transpositions](#tutorial-6-chords-rests-and-transpositions)
- [Tutorial 7: Making variants, editing objects](#tutorial-7-making-variants-editing-objects)
- [Tutorial 8: Transforming objects](#tutorial-8-transforming-objects)
- [Tutorial 9: More opportunities to filter](#tutorial-9-more-opportunities-to-filter)
- [Tutorial 10: Filling time: density sections](#tutorial-10-filling-time-density-sections)
- [Tutorial 11: Note structures](#tutorial-11-note-structures)
- [Tutorial 12: Making note sections](#tutorial-12-making-note-sections)
- [Tutorial 13: A level higher: controllers and schemes](#tutorial-13-a-level-higher-controllers-and-schemes)
- [Tutorial 14: Communities](#tutorial-14-communities)
- [Tutorial 15: Playing live](#tutorial-15-playing-live)
- [Tutorial 16: Getting out](#tutorial-16-getting-out)
- [Index of objects](#index-of-objects)
- [Index of tools](#index-of-tools)

---

## Introduction

Computer-aided algorithmic composition is the broad area where computer tools are
used to assist in the composition of musical material. The material could be a
handful of values useful for one parameter. It could be a framework of time and
pitch points to serve as a basis for realizing a composition. It could be a
complete description of a composer's model for a particular composition.

The Pattern Toolbox is a collection of tools for those purposes, built on
SuperCollider's patterns. Several models for defining musical events are
included. They are used by defining objects such as sections, shapes, masks, or
note structures.

An object has a name, a set of rules, and one specific result of applying those
rules. Both are kept. This allows material to be specified in general terms while
a particular instance of it is held on to. Composition sometimes requires both
good design and good luck.

The design of the Toolbox follows Paul Berg's **AC Toolbox** closely. What is new
here belongs to SuperCollider: objects are sounded by SynthDefs rather than by
Midi alone, every result can be reproduced exactly from the seed that made it, and
material can be played live and altered while it sounds.

### About this documentation

Each tutorial is accompanied by a file in the `examples` folder containing the
objects discussed in the text. Requests to perform an action are preceded by a
`•` symbol:

> • Evaluate the block and listen to the section.

This documentation is not a general tutorial on algorithmic composition.

---

## Basics

### Installing

Place the `Pattern-Toolbox` folder in the SuperCollider `Extensions` folder and
recompile the class library. Nothing else is required; there are no dependencies.

### Objects

Various types of object can be defined. They include sections, stockpiles, shapes,
masks, note structures, combinations, communities, controllers, schemes, and
binds. These objects are used to define musical material.

| object | what it is |
| --- | --- |
| section | one or more notes: a note, a phrase, a voice, a movement, a composition |
| stockpile | a collection of values |
| shape | a curve: some motion over time |
| mask | two curves: a field that changes over time |
| note structure | notes whose pitch and rhythm belong together, nested in sequence and in parallel |
| community | a group of sections joined in name only |
| controller | a source of values that keeps its state outside any section |
| scheme | a script for remaking several objects in a fixed order |
| bind | rules with no result: material that plays live and can be changed while sounding |

Almost any group of time and pitch points is called a section. This avoids having
to make judgements about the material being produced before it exists.

### Making an object

Every object is made in the same way: a name, and a set of slots filled with text.

```supercollider
PTDataSection(\section1, (
	clock:  "100",
	number: "60",
	rhythm: "1",
	pitch:  "Pseries(c2, 1)"
)).make;
```

`PT(\section1)` finds it again afterwards. `PT.browse` opens the object list, from
which any object can be inspected, played, plotted, remade and edited.

### Input

Slots are filled with text. The same kinds of thing are accepted everywhere.

| you write | you get |
| --- | --- |
| `60` | a number |
| `c4` | a pitch name. Middle C is `c4` |
| `cs4` `c#4` `ef4` `bf-1` | sharps, flats, negative octaves |
| `mf` `ff` `ppp` | a dynamic, as a velocity |
| `1 2 3` or `c4 e4 g4` | a list, which cycles |
| `[[c4, e4, g4]]` | a chord |
| `Pwhite(60, 72)` | any SuperCollider pattern |
| `cmajor` | any object already made, by name |
| `-1` | in a rhythm slot: a rest of that length |
| `PT.bpm(160)` | in a clock slot |
| `PT.untilTime(10)` | in a number slot: fill ten seconds |

Pitches are Midi note numbers or symbols containing pitch class and octave. The
octave of middle C is 4, and can be changed with `PT.middleCOctave`.

Bare names are rewritten before the text is compiled: an object name becomes an
environment variable, a pitch name becomes a note number, a dynamic becomes a
velocity. To write plain SuperCollider instead, set `PT.sugar = false` and use
`~cmajor` and `60`.

### Generators

A generator returns the next value in some series each time it is used. In
SuperCollider a generator is a **pattern**. `Pwhite(1, 100)` produces a value
between 1 and 100; `Prand([1, 2, 3], inf)` chooses among three values;
`Pseries(60, 1)` counts upwards from 60.

Patterns whose name ends in a bandwidth produce values between two limits.
Patterns that take a list choose from an available supply of values.

If the input to a pattern is only integer values, an integer is produced.
Otherwise a real number is returned. `Pwhite(60, 72)` gives whole semitones,
`Pwhite(60.0, 72)` gives microtones.

### Tools and transformers

Tools are functions to do things that may be useful. `PT.convert` interprets a
shape or a mask in a range; `PT.readFrom` reads a collection in the order a curve
describes; `PT.attacks` builds a list of attack points. Transformers modify an
object or part of one: `PT.transpose`, `PT.stretch`, `PT.limit`.

When a tool is evaluated it produces a result. When a pattern is evaluated it
produces something that still has to be applied before there is a usable result.

### Errors

When the Toolbox catches an error, a message is printed naming the slot that
caused it. These include not filling a slot, referring to an object that has not
been made, and text that will not compile.

To stop everything that is sounding, evaluate `PT.stopAll`.

---

## Tutorial 1: Making data sections

*Example file: `examples/01-sections-and-stockpiles.scd`*

A section is a collection of one or more notes. A **data section** is a section
where the value of each parameter is specified separately. Rhythm, pitch and
velocity are calculated without necessarily referring to each other.

A clock unit is specified. This is the basic beat of a section, expressed in
milliseconds. Rhythm is expressed as a multiple of this clock unit: one times the
beat, two times the beat, 3.5 times the beat.

```
Name            section1
Clock unit      100
Number          60
Rhythm          1
Pitch           Pseries(c2, 1)
Velocity        mf
Channel         1
```

```supercollider
PTDataSection(\section1, (
	clock:    "100",
	number:   "60",
	rhythm:   "1",
	pitch:    "Pseries(c2, 1)",
	velocity: "mf",
	channel:  "1"
)).make;
```

The clock unit is 100 milliseconds. Sixty notes will be made. Each note lasts one
times 100 milliseconds. Pitch starts at c2 and adds one to each successive note.
Instead of `c2`, the Midi note number 36 could have been written.

- • Play the section with `PT(\section1).play`.
- • Plot the piano roll representation with `PT(\section1).plot`. High pitches are
  at the top and low pitches at the bottom.
- • Plot one parameter alone with `PT(\section1).plot(\rhythm)`.
- • Print a plain text score with `PT(\section1).textScore(20)`.

### Generating several parameters

It is possible to use a pattern for several parameters at once.

```
Name            section6
Clock unit      100
Number          200
Rhythm          Pbrown(0.1, 5, 0.5)
Pitch           Pbrown(c2, c6, 5)
Velocity        Pbrown(pp, ff, 5)
```

```supercollider
PTDataSection(\section6, (
	clock:    "100",
	number:   "200",
	rhythm:   "Pbrown(0.1, 5, 0.5)",
	pitch:    "Pbrown(c2, c6, 5)",
	velocity: "Pbrown(pp, ff, 5)"
)).make;
```

`Pbrown` takes a lower limit, an upper limit and a maximum step. Each value is a
step away from the one before it, and the limits are respected.

### Channels can also be changed

The channel number does not have to be constant. If the specification is a list,
the first note gets the first channel number, the second note the second, and so
on, cycling back to the beginning at the end of the list. If it is a pattern, the
pattern is applied for each note.

### Making notes until some amount of time has been filled

Instead of a number of notes, the number slot can be given
`PT.untilTime(10)`. Notes will be made until ten seconds have been filled.

```supercollider
PTDataSection(\section8, (
	clock:  "100",
	number: "PT.untilTime(10)",
	rhythm: "Pwhite(1.0, 2)",
	pitch:  "Pwhite(c3, c5)"
)).make;
```

- • Make section8 and notice how many notes were actually produced with
  `PT(\section8).length`.

### Rests

Rests are negative numbers. In a data section, rhythms are multiples of the clock
unit, and so are rests. The absolute value is the length of the rest.

A rest is not inserted between notes. A rest causes a note that is produced not to
be heard, and it counts as one of the number requested.

```supercollider
PTDataSection(\section2, (
	clock:  "150",
	number: "50",
	rhythm: "Prand([1, -1], inf)",
	pitch:  "c4 d4 e4 f4"
)).make;
```

`PT(\section2).length` counts every event, `PT(\section2).numNotes` counts only
the notes that sound.

### Adding comments

A comment can be added to any object and is saved with it.

```supercollider
PT(\section2).comment_("half of these are rests");
```

### Saving

`PT.save("~/Desktop/sketch.ptx")` writes every object to a file, and
`PT.load` reads one back. The file is plain SuperCollider source: readable,
comparable, and small. Because the random seed of each result is written out with
its rules, loading a file brings back the exact material rather than merely the
means of making it.

### Summary

**Objects** data section
**Methods** make, play, stop, plot, textScore, length, numNotes, comment
**Patterns** Pseries, Pwhite, Pbrown, Prand
**Tools** PT.untilTime, PT.bpm, PT.save, PT.load, PT.browse
**Miscellaneous** rests, comments, channels

---

## Tutorial 2: Combining sections

*Example file: `examples/04-combining-and-transforming.scd`*

Sections can be combined in three ways. In each, the sections are named in one
slot, and a number among the names says something about time.

Two sections to work with:

```supercollider
PTDataSection(\bass, (
	clock: "200", number: "16", rhythm: "2",
	pitch: "Pwhite(c1, c3)", velocity: "70"
)).make;

PTDataSection(\lead, (
	clock: "200", number: "32", rhythm: "1",
	pitch: "Pwhite(c4, c6)", velocity: "Pwhite(60, 110)"
)).make;
```

### In sequence

```supercollider
PTSequence(\pair, (sections: "bass lead")).make;
```

A number between two names is a delay in seconds between them.

```supercollider
PTSequence(\spaced, (sections: "bass 2 lead")).make;
```

### In parallel

```supercollider
PTParallel(\duo, (sections: "bass lead")).make;
```

A number before a name is that voice's offset in seconds from the beginning of the
whole combination.

```supercollider
PTParallel(\stagger, (sections: "bass 0.4 lead")).make;
```

### At given times

```supercollider
PTTimed(\entries, (sections: "bass lead", times: "0 1.5")).make;
```

Start times are counted from the beginning of the combination. They are not times
between objects.

A combination is a section like any other, so combinations can be combined.

```supercollider
PTParallel(\canon, (sections: "pair 1 pair 2 pair")).make;
```

- • Plot a parallel combination and notice that the voices overlap.

### Summary

**Objects** sequential section, parallel section, timed section
**Miscellaneous** combinations nest

---

## Tutorial 3: Using stockpiles, making choices

*Example file: `examples/01-sections-and-stockpiles.scd`*

Several patterns choose from a collection of values. That collection could be a
list. If frequent choices are made from the same collection, it is more convenient
to specify the collection separately and give it a name. Such a collection is
called a **stockpile**.

The idea behind a stockpile is that a composer may wish to gather or prepare
material before specifying its use or organization.

### Specifying a stockpile

Values are entered one after another, without brackets or commas.

```
Name            cmajor
Source          c4 d4 e4 f4 g4 a4 b4
```

```supercollider
PTStockpile(\cmajor, (source: "c4 d4 e4 f4 g4 a4 b4")).make;
```

Once made, the stockpile can be used as a parameter of a data section, or as the
supply for a pattern that chooses.

```supercollider
PTDataSection(\section5, (
	clock:  "150",
	number: "100",
	rhythm: "Prand([1, 2, 3], inf)",
	pitch:  "Prand(cmajor, inf)"
)).make;
```

### Generating a stockpile

A stockpile can be produced with a pattern, applied a given number of times.

```
Name            row
Source          Pshuf((60..71), 1)
Number          12
```

```supercollider
PTStockpile(\row, (source: "Pshuf((60..71), 1)", number: "12")).make;
```

`Pshuf` shuffles a list once, so each of the twelve pitch classes occurs exactly
once.

### Constructing a stockpile

Any expression producing a list may be used.

```supercollider
PTStockpile(\chromatic, (source: "(48..72)")).make;
PTStockpile(\evens, (source: "(48..72).select { |n| n.even }")).make;
PTStockpile(\fromSection, (source: "PT(\\section1).extract(\\pitch)")).make;
```

The last takes the pitches of a section that has already been made, which is a
way of using one object as material for another.

- • Look at the values of a stockpile with `PT(\row).postValues`.
- • Plot them with `PT(\row).plot`.

### Choosing among stockpiles

Because a stockpile is a list, ordinary pattern arithmetic applies to it. To
transpose the values on their way out without changing the stockpile:

```supercollider
PTDataSection(\section3, (
	number: "50",
	rhythm: "Prand([1, 2, 3], inf)",
	pitch:  "Prand(cmajor, inf) + Prand([-12, 0, 12], inf)"
)).make;
```

To choose which of several stockpiles is used, choose among the names:

```supercollider
PTDataSection(\section7, (
	number: "60",
	pitch:  "Pswitch([Pn(Prand(cmajor, 25), 1), Pn(Prand(row, 25), 1)], Pseq([0, 1], inf))"
)).make;
```

### Summary

**Objects** stockpile
**Methods** postValues, plot
**Patterns** Prand, Pshuf, Pswitch, Pn
**Miscellaneous** a stockpile is a list, so it can be added to and chosen from

---

## Tutorial 4: Shapes

*Example file: `examples/03-shapes-and-masks.scd`*

A **shape** is a curve, reflecting some motion over time. It carries no scale of
its own. Only its contour matters; its absolute values mean nothing until the
shape is converted into a range.

A shape can be specified as a series of evenly spaced numbers, generated with a
pattern, or drawn with the mouse.

### Specifying a shape

```
Name            arch
Source          0 40 80 100 80 40 0
```

```supercollider
PTShape(\arch, (source: "0 40 80 100 80 40 0")).make;
```

These two shapes are the same shape, because only the contour counts:

```supercollider
PTShape(\arch2, (source: "20 36 52 60 52 36 20")).make;
```

### Generating a shape

```supercollider
PTShape(\wobble, (source: "Pbrown(0, 100, 12)", number: "40")).make;
```

### Drawing a shape

```supercollider
PTShape.draw(\hand);
```

A grid appears. Drag from left to right to draw. `flat`, `invert`, `smooth` and
`normalise` alter what is there. `make` records the drawing into the object's
rules as a list of points, so a drawn shape is an ordinary specification and saves
like any other.

- • Draw a shape and use it for pitch in a data section.

### Converting a shape

Converting produces a number of values following the contour, mapped into a range.
Integer bounds give integer results, so pitches come out as note numbers.

```supercollider
PT(\arch).convert(20, \c3, \c5);   // pitch names work as bounds
```

Inside a section, `PT.fromNumber` asks how many notes are being made, so the shape
stretches to fit rather than repeating.

```supercollider
PTDataSection(\shapeSection, (
	clock:  "100",
	number: "120",
	rhythm: "1",
	pitch:  "arch.convert(PT.fromNumber, c3, c5)"
)).make;
```

A shape can drive any parameter.

```supercollider
PTDataSection(\shapeRhythm, (
	clock:    "100",
	number:   "80",
	rhythm:   "arch.convert(PT.fromNumber, 0.5, 4.0)",
	pitch:    "Pwhite(c3, c5)",
	velocity: "wobble.convert(PT.fromNumber, 30, 110)"
)).make;
```

### Using a shape to read from a stockpile

A curve can also be read as an order over a collection. The lowest point of the
curve reads the first element, the highest the last. The contour survives while
the pitch content is constrained.

```supercollider
PTDataSection(\readSection, (
	clock:  "120",
	number: "100",
	rhythm: "1",
	pitch:  "PT.readFrom(cmajor, arch, PT.fromNumber)"
)).make;
```

### Summary

**Objects** shape
**Methods** convert, draw, plot
**Tools** PT.convert, PT.readFrom, PT.fromNumber

---

## Tutorial 5: Masks

*Example file: `examples/03-shapes-and-masks.scd`*

A **mask** is an abstraction represented by two lines. It is a field that changes
over time. Events can happen in the area between the lines. This requires
interpretation. A mask, once interpreted, can represent the tendency of certain
events to happen.

A mask is related to G. M. Koenig's concept of a tendency mask, formulated for his
composition program Project 2 and since adapted by several composers.

### Specifying a mask

```
Name            mask1
Top             100 90 70 100 60
Bottom          0 20 40 30 55
```

```supercollider
PTMask(\mask1, (top: "100 90 70 100 60", bottom: "0 20 40 30 55")).make;
```

### Drawing a mask

```supercollider
PTMask.draw(\mask2);
```

Choose `top` or `bottom` in the popup, then drag. The line being drawn is thicker
than the other.

### Converting a mask

Converting a mask picks a value between its boundaries at each point in time. The
two lines are scaled together, so the lowest point anywhere in the mask becomes
the lower bound and the highest becomes the upper.

```supercollider
PTDataSection(\maskSection, (
	clock:  "100",
	number: "200",
	rhythm: "1",
	pitch:  "mask1.convert(PT.fromNumber, 48, 72)"
)).make;
```

### With a different generator

By default the choice inside the field is uniform. Another source may be given
instead, producing values between 0 and 100 that describe a position between the
lower boundary and the upper. `PTbeta` with small shape parameters produces values
near its two extremes, so the pitches follow the outline of the mask and few are
chosen in between.

```supercollider
PTDataSection(\maskEdges, (
	clock:  "100",
	number: "200",
	rhythm: "1",
	pitch:  "mask1.convert(PT.fromNumber, 48, 72, PTbeta(0.0, 100, 0.1, 0.1))"
)).make;
```

- • Plot both sections and compare them. The second is heard as two lines rather
  than as a filled band.

### Rounding the values

Float bounds and a rounding unit give quarter tones.

```supercollider
PT(\mask1).convert(20, 48, 72.0, nil, 0.5);
```

### Masking a stockpile

If the pitches should be limited to some collection, read the collection with the
mask. The shape of the field is followed, but only the available pitches sound.

```supercollider
PTDataSection(\readMask, (
	clock:  "120",
	number: "150",
	rhythm: "1",
	pitch:  "PT.readFrom(cmajor, mask1, PT.fromNumber)"
)).make;
```

### The width of the field

`spread` gives how open the mask is at each point, which is useful for relating
one parameter to another.

```supercollider
PTDataSection(\maskSpread, (
	clock:    "80",
	number:   "150",
	rhythm:   "1",
	pitch:    "mask1.convert(PT.fromNumber, 48, 84)",
	velocity: "mask1.spread(150, 30, 120)"
)).make;
```

### Summary

**Objects** mask
**Methods** convert, spread, draw, plot
**Patterns** PTbeta
**Tools** PT.convert, PT.readFrom

---

## Tutorial 6: Chords, rests, and transpositions

*Example file: `examples/04-combining-and-transforming.scd`*

### Chords

A flat list of pitches is a sequence of notes. A nested list is a chord.

```supercollider
PTDataSection(\section3b, (
	clock:  "150",
	number: "50",
	rhythm: "Prand([1, -1], inf)",
	pitch:  "[[c4, e4, g4], [c4, d4, e4, f4], [d4, g4, b4]]"
)).make;
```

That plays the three chords in turn. To choose among them:

```supercollider
PTDataSection(\section4, (
	clock:  "150",
	number: "50",
	rhythm: "Prand([1, -1], inf)",
	pitch:  "Prand([[c4, e4, g4], [c4, d4, e4, f4], [d4, g4, b4]], inf)"
)).make;
```

### Pitch classes and octaves

A pitch class is a pitch without an octave. Pitch and octave can be generated
separately and combined.

```supercollider
PTDataSection(\section8b, (
	clock:  "200",
	number: "50",
	rhythm: "1",
	pitch:  "(Prand(cmajor, inf) % 12) + (Pwhite(3, 5) * 12) + 12"
)).make;
```

### Per note

Each value of a list can be transposed by a different amount by adding a pattern
to it. This may diminish the coherence of the original list, depending on the
values chosen.

```supercollider
PTDataSection(\section10, (
	clock:  "120",
	number: "36",
	rhythm: "1",
	pitch:  "Pseq([c4, d4, f4], inf) + Pwhite(-12, 12)"
)).make;
```

### Per group

Another way is to transpose the whole group by one interval, then the whole group
by the next. The pattern is presented without transposition, then a semitone
higher, and so on.

```supercollider
PTDataSection(\section11, (
	clock:  "120",
	number: "36",
	rhythm: "1",
	pitch:  "Pseq([c4, d4, f4], inf) + Pstutter(3, Pseries(0, 1, 12))"
)).make;
```

`Pstutter` repeats each value of the second pattern as many times as the first
says, which is what keeps one interval in place for the length of the group.

### Summary

**Patterns** Prand, Pseq, Pwhite, Pstutter, Pseries
**Miscellaneous** a nested list is a chord; pattern arithmetic transposes

---

## Tutorial 7: Making variants, editing objects

*Example file: `examples/01-sections-and-stockpiles.scd`*

Toolbox objects are declared by specifying rules. The rules may be trivial or
evocative. If the rules involve any indeterminacy, applying them another time
could lead to a different result.

Each object records the rules by which it was specified and the specific result
produced by using them. The general and the specific are both maintained. This
allows objects to be specified in general terms while a particular instance of
those rules is kept.

A **variant** of an object is an object of the same type, with the same rules, and
possibly a different result.

```supercollider
PTDataSection(\noise1, (
	clock:    "150",
	number:   "64",
	rhythm:   "Pbrown(0.5, 2.0, 0.2)",
	pitch:    "Pbrown(c2, c5, 4)",
	velocity: "Pbrown(30, 90, 5)"
)).make;

PT(\noise1).variant;
```

The variant is called `noise1a`; the next is `noise1b`. Both were made using the
same rules and the results differ. Listening to them shows general similarities
and specific differences.

### The five things you can do to an object

| | |
| --- | --- |
| `make` | apply the rules. Each time gives a new result |
| `reproduce` | apply the rules with the stored seed. The same result, exactly |
| `specify` | store the rules without applying them |
| `remake` | apply them again in place, discarding the old result |
| `variant` | the same rules under a new name |

Every result records the random seed that produced it, so nothing you liked is
ever lost. This is the one place where the Toolbox does more than the AC Toolbox
rather than the same thing differently: a variant there cannot be recovered.

- • Make `noise1` several times, listening each time, then `PT(\noise1).reproduce`
  to return to the last one.

### Editing an object

`PT(\noise1).edit` opens a dialog. Every slot is a text field. Changing a slot and
pressing `make` remakes the object. Changing the **name** first and then pressing
`make` produces a **new** object with the same rules, which is how a family of
related objects is built up.

The `...` button beside a slot opens a larger editor with a parenthesis check,
which is worth having once expressions grow.

### Summary

**Methods** make, reproduce, specify, remake, variant, edit
**Miscellaneous** rules and result are both kept; seeds make results recoverable

---

## Tutorial 8: Transforming objects

*Example file: `examples/04-combining-and-transforming.scd`*

A transformer modifies an object or part of one. Transposing pitches, stretching
tempi, adding random deviation to a parameter are all transformations.

A transformed section is itself an object. Its rules name a source and a list of
transformations. The source is not touched. Remake the derived object and the
transformation runs again; remake the source first and the whole chain follows.

```supercollider
PTDerived(\leadUp, (source: "lead", transform: "PT.transpose(12)")).make;
```

Transformations apply from left to right, so their order matters.

```supercollider
PTDerived(\clipped, (source: "lead", transform: "[PT.transpose(24), PT.limit(c3, c6)]")).make;
PTDerived(\clipped2, (source: "lead", transform: "[PT.limit(c3, c6), PT.transpose(24)]")).make;
```

The amount may be a constant, a list or a pattern. With a pattern, each note is
transformed by a different amount.

```supercollider
PTDerived(\wander, (source: "lead", transform: "PT.transpose(Pwhite(-5, 5))")).make;
```

### The transformers

| values | `PT.add` `PT.multiply` `PT.set` `PT.transpose` `PT.louder` `PT.limit` `PT.fold` `PT.quantize` |
| --- | --- |
| time | `PT.stretch` |
| structure | `PT.reverse` `PT.slice` `PT.keep` `PT.drop` |
| conditional | `PT.transformIf` |

`PT.limit` clips values into a range; `PT.fold` reflects them back, which keeps
the contour where clipping flattens it against the ceiling.

```supercollider
PTDerived(\folded, (source: "lead", transform: "PT.fold(c4, c5)")).make;
PTDerived(\slow, (source: "lead", transform: "PT.stretch(2)")).make;
PTDerived(\back, (source: "lead", transform: "PT.reverse")).make;
PTDerived(\middle, (source: "lead", transform: "PT.slice(8, 24)")).make;
```

Several sources are joined in sequence first, then transformed.

```supercollider
PTDerived(\bothUp, (source: "bass lead", transform: "PT.transpose(7)")).make;
```

Velocity and loudness stay in step: a section stores velocity, and amplitude is
derived from it when the section is played, so `PT.louder(30)` is audible.

### Summary

**Objects** derived section
**Tools** PT.transpose, PT.limit, PT.fold, PT.quantize, PT.stretch, PT.reverse,
PT.slice, PT.keep, PT.drop, PT.add, PT.multiply, PT.set, PT.louder, PT.transformIf

---

## Tutorial 9: More opportunities to filter

*Example file: `examples/04-combining-and-transforming.scd`*

Filtering removes what is not wanted. There are two ways to do it and they are not
the same.

`PT.filter` keeps the events that pass a test and closes the gaps, so the section
becomes shorter. `PT.mute` silences them instead, leaving the timing untouched.

```supercollider
PTDerived(\highOnly, (source: "lead", transform: "PT.filter({ |e| e.midinote > 60 })")).make;
PTDerived(\highGone, (source: "lead", transform: "PT.mute({ |e| e.midinote > 60 })")).make;
```

- • Compare `PT(\highOnly).duration` with `PT(\highGone).duration`. The second is
  the same length as `lead`.

`PT.reject` is the inverse of `PT.filter`.

### Filtering repetitions

`PT.dedupe` drops the immediate repetition of a value.

```supercollider
PTDerived(\noRepeats, (source: "lead", transform: "PT.dedupe")).make;
```

### Meeting a condition

`PT.transformIf` applies a transformation only to the events that pass a test. The
others keep their place, so the timing is preserved.

```supercollider
PTDerived(\accented, (
	source:    "lead",
	transform: "PT.transformIf({ |e| e.velocity > 90 }, PT.transpose(12))"
)).make;
```

A test receives one event and returns true or false. `e.midinote`, `e.velocity`,
`e.dur` and `e.chan` are all available.

### Filtering while generating

Filtering can also happen before an object is made rather than after. `Pwhite`
with a rounding unit avoids values that are not wanted; `Pxrand` never repeats the
value it just gave; `Pshuf` uses everything once before repeating anything.

### Summary

**Tools** PT.filter, PT.reject, PT.mute, PT.dedupe, PT.transformIf
**Patterns** Pxrand, Pshuf
**Miscellaneous** filtering shortens, muting preserves

---

## Tutorial 10: Filling time: density sections

*Example file: `examples/06-note-structures-and-density.scd`*

A **density section** is a section where time and density are primary. The amount
of time available, the number of events to be placed in it, and a means of
deciding where those events fall are the essential ingredients.

A data section says "make this many notes and let the clock work out how long that
takes". A density section inverts it.

Time is expressed in seconds. Durations are expressed in milliseconds, since there
is no clock unit for them to be a multiple of.

### Using attack points

Attack points are given as a percentage of the total time. Zero per cent is the
beginning, 100 the end. An attack point says where a note begins, not how long it
lasts.

```
Name            even
Time            10
Attacks         0 10 20 30 40 50 60 70 80 90
Duration        200
Pitch           Pwhite(c3, c5)
```

```supercollider
PTDensity(\even, (
	time:     "10",
	attacks:  "0 10 20 30 40 50 60 70 80 90",
	duration: "200",
	pitch:    "Pwhite(c3, c5)"
)).make;
```

When a list is used, the number of values in the list decides the number of notes,
whatever the number slot says. The percentages above produce attacks one second
apart.

With a pattern, the number slot decides how many attacks are drawn, and they are
sorted. The density curve of the section is then the density curve of the pattern.

```supercollider
PTDensity(\edges, (
	time:     "10",
	number:   "120",
	attacks:  "PTbeta(0.0, 100, 0.2, 0.2)",
	duration: "150",
	pitch:    "Pwhite(c2, c6)"
)).make;
```

Because `PTbeta` with small parameters produces values near its extremes, the
attacks crowd both ends of the ten seconds and thin out in the middle.

- • Plot it. Vary the two shape parameters and notice the difference.

### Producing intervals

Another approach is to use a function to produce the intervals **between** attack
points, rather than their absolute positions. This is similar to what Xenakis did
in his Stochastic Music Program, where a probability function decided the interval
until the next attack.

`PT.attacks` returns a list of attack points made by adding one interval after
another, then mapping the result across the available range.

```supercollider
PT.attacks(1, number: 10);
```

That gives ten evenly spaced values from 0 to 100. With a pattern for the interval
the spacing varies while the order always ascends.

```supercollider
PTDensity(\intervals, (
	time:     "12",
	number:   "80",
	attacks:  "PT.attacks(PTbeta(0.0, 0.1, 1, 1))",
	duration: "120",
	pitch:    "Pwhite(c3, c6)"
)).make;
```

### Using a curve

In a density curve section the density of notes is determined by mapping a shape
or a mask between a minimum and a maximum number of notes per time unit. By
default the time unit is one second.

```supercollider
PTShape(\swell, (source: "1 4 20 8 2 14 30 6 1")).make;

PTDensityCurve(\swelling, (
	time:     "14",
	curve:    "swell",
	min:      "1",
	max:      "24",
	unit:     "1",
	duration: "120",
	pitch:    "Pwhite(c3, c6)"
)).make;
```

- • Plot it. The section is thick where the shape is high.

If a mask is given instead of a shape, the count at each step is chosen between
its two lines, so the density itself becomes a tendency rather than a fixed
contour.

```supercollider
PTMask(\loose, (top: "30 24 18 12", bottom: "2 6 10 14")).make;

PTDensityCurve(\loosening, (
	time:     "12",
	curve:    "loose",
	min:      "1",
	max:      "30",
	unit:     "1",
	duration: "150",
	pitch:    "Pwhite(c2, c5)"
)).make;
```

### Summary

**Objects** density section, density curve section
**Patterns** PTbeta
**Tools** PT.attacks
**Miscellaneous** time in seconds, durations in milliseconds; a list of attacks
sets the number itself

---

## Tutorial 11: Note structures

*Example file: `examples/06-note-structures-and-density.scd`*

A data section calculates rhythm, pitch and velocity apart from each other. That
is excellent for making material and useless for writing down music you already
know.

A **note structure** treats a note as one indivisible thing. Note structures allow
the specification of a known group of pitches, chords and rests, and they can be
combined in sequence and in parallel.

Rhythm inside a structure is in clock units. A section supplies the clock, so one
structure can be played at several tempi.

### Writing a melody

Rhythm comes first, then pitch. `r` is a rest, `.` is a delay, and a plus sign
makes a chord.

```supercollider
PTNoteStructure(\theme, (
	source: "PT.melody(\"2 c4 1 d4 1 e4 2 g4 1 f4 1 e4 2 d4 1 r 2 c4\")"
)).make;
```

- • Look at it with `PT(\theme).postValues`.
- • `PT(\theme).size` is how many notes; `PT(\theme).beats` is how long in clock
  units.

### Building a structure

For anything more involved, build the tree.

| | |
| --- | --- |
| `PT.note(rhythm, pitch)` | a note |
| `PT.rest(rhythm)` | a rest: silence that is kept as an event |
| `PT.delay(rhythm)` | time with nothing in it at all |
| `PT.seq(...)` | one after another |
| `PT.par(...)` | at the same time |

The AC Toolbox spellings `PT.aNote`, `PT.aRest`, `PT.aDelay`, `PT.inSequence` and
`PT.inParallel` do the same.

```supercollider
PTNoteStructure(\chords, (source: "PT.seq(
	PT.par(PT.note(4, c3), PT.seq(PT.note(2, e4), PT.note(2, g4))),
	PT.par(PT.note(4, f3), PT.seq(PT.note(1, a4), PT.note(1, g4), PT.note(2, f4)))
)")).make;
```

A low note is held while notes move above it. In sequence, durations add up; in
parallel, the group lasts as long as its longest member.

### Summary

**Objects** note structure
**Methods** postValues, size, beats
**Tools** PT.melody, PT.note, PT.rest, PT.delay, PT.seq, PT.par

---

## Tutorial 12: Making note sections

*Example file: `examples/06-note-structures-and-density.scd`*

In a **note section**, rhythm, pitch, velocity and channel are dealt with together
by using notes rather than separately.

### With note structures

Given a structure, the section plays it as written, parallelism and all. An empty
number slot means all of it.

```supercollider
PTNoteSection(\played, (clock: "300", notes: "theme")).make;
```

### With patterns

Given anything else that supplies notes, the structure is flattened and becomes
material to choose from. This is how a known melody is turned into raw material.

```supercollider
PTNoteSection(\scattered, (clock: "200", number: "60", notes: "Prand(theme, inf)")).make;
PTNoteSection(\permuted, (clock: "200", number: "60", notes: "Pxrand(theme, inf)")).make;
```

When a structure is used as material it is flattened: no notes are in parallel,
all are in sequence.

### With shapes

The order in which notes are chosen can follow a curve.

```supercollider
PTNoteSection(\shaped, (
	clock:  "200",
	number: "80",
	notes:  "PT.readFrom(theme, arch, PT.fromNumber)"
)).make;
```

### Interpolating between two structures

`PT.interpolate` performs a gradual change from one object to another. During each
step of the interpolation either an element of the first or an element of the
second is returned, and the probability that the second is chosen increases as the
interpolation proceeds. The idea is Rainer Boesch's.

```supercollider
PTNoteStructure(\other, (source: "PT.melody(\"1 c5 1 b4 1 gs4 1 fs4 1 e4\")")).make;

PTNoteSection(\morphing, (
	clock:  "180",
	number: "160",
	notes:  "PT.interpolate(theme, other, 160)"
)).make;
```

Instead of a straight line, any shape may be given for the interpolation.

```supercollider
PTNoteSection(\morphing2, (
	clock:  "180",
	number: "160",
	notes:  "PT.interpolate(theme, other, 160, arch)"
)).make;
```

### Using a section as material

A section can supply notes too. Its rhythms arrive in seconds, so a clock of 1000
reproduces the original timing. This is useful when a section has been captured or
imported and the intention is to rearrange what is in it.

```supercollider
PTNoteSection(\reordered, (
	clock:  "1000",
	number: "40",
	notes:  "Pxrand(PT.asList(lead), inf)"
)).make;
```

### Summary

**Objects** note section
**Tools** PT.readFrom, PT.interpolate, PT.asList, PT.fromNumber
**Patterns** Prand, Pxrand

---

## Tutorial 13: A level higher: controllers and schemes

*Example file: `examples/05-controllers-and-schemes.scd`*

Patterns used in the rules of a section have no history outside that section. Each
time the section is made, a new pattern is built that knows nothing about what
happened previously. No relationship between one section and the next can be
expressed with them.

A **controller** is an object that retains its state outside any section
specification. It remembers what it has done and knows what it should do next. It
is useful for expressing relationships at a level higher than one parameter of one
section.

### Using controllers

```supercollider
PTController(\howMany, (source: "2 10 20")).make;

PTDataSection(\bit, (clock: "150", number: "howMany", pitch: "Pwhite(c3, c5)")).specify;
```

- • Make `bit` three times. It has two notes, then ten, then twenty.

`PT(\howMany).history` is everything the controller has handed out.
`PT(\howMany).reset` starts it again. A given seed replays a given history exactly.

Note that `bit` was **specified**, not made. When an object is specified its rules
are stored but not applied, so the controller is not yet consulted.

### Take one

Used bare in a slot, a controller supplies a value every time that slot is
calculated: once per note. `PT.takeOne` pulls a single value and hands that one
value on, so it holds for the whole section.

```supercollider
PTController(\low, (source: "48 50 52 54 56")).make;
PTController(\high, (source: "72 74 76 78 80")).make;

PTDataSection(\band, (
	clock:  "120",
	number: "30",
	pitch:  "PTbeta(PT.takeOne(low), PT.takeOne(high), 0.3, 0.3)"
)).specify;
```

- • Make `band` several times. Each one sits inside a different band, and the
  bands walk outwards.

### Change on a level above the section

This is what controllers are for. Five variants of one section, each tending to be
faster and louder than the one before. No pattern inside the section can do this,
because the trend lives between the sections rather than within them.

```supercollider
PTController(\rhythmCeiling, (source: "Pseries(3.0, -0.375, 5)")).make;
PTController(\velocityFloor, (source: "Pseries(40, 7.5, 5)")).make;

PTDataSection(\gesture, (
	clock:    "100",
	number:   "24",
	rhythm:   "PTbeta(1.0, PT.takeOne(rhythmCeiling), 0.2, 0.2)",
	pitch:    "Pbrown(c2, c6, 6)",
	velocity: "PTbeta(PT.takeOne(velocityFloor), 90, 0.2, 0.2)"
)).specify;

PTCommunity(\gestures, (source: "gesture", number: "5")).make;
```

- • Compare the variants:
  `PT(\gestures).sections.collect { |s| s.extract(\rhythm).mean }`.

### Using the same value

Sometimes two parameters must be derived from the **same** value. If a controller
were used in both, it would be applied twice and give two different values.

The solution is to synchronise two controllers to a third. Whichever asks first
makes the third produce a value; the other sees that same value rather than a new
one.

```supercollider
PTController(\choose, (source: "Prand([1, 2, 3, 10], inf)")).make;
PTController(\forRhythm, (syncTo: "choose")).make;
PTController(\forVelocity, (syncTo: "choose")).make;

PTStockpile(\table, (source: "1 40 2 55 3 70 10 110")).make;

PTDataSection(\linked, (
	clock:    "120",
	number:   "40",
	rhythm:   "forRhythm",
	pitch:    "Pwhite(c3, c5)",
	velocity: "PT.lookup(forVelocity, table)"
)).make;
```

`PT.lookup` maps values through a table given as pairs. Short notes are quiet and
long ones are loud, note for note.

### Schemes

A **scheme** contains the names of objects that are to be remade in a certain
order. When the scheme is applied, those objects are made in that order. If an
object is made as part of a scheme, its previous result is lost.

The two reasons to use a scheme are convenience and design. The convenience is
that a section reading a generated shape needs the shape remade first, and doing
that by hand becomes tedious. The design is that the order of remaking is itself a
decision.

```supercollider
PTShape(\curve, (source: "Pbrown(0, 100, 20)", number: "16")).make;

PTDataSection(\theme2, (
	clock:    "110",
	number:   "48",
	pitch:    "curve.convert(PT.fromNumber, c3, c5)",
	velocity: "curve.convert(PT.fromNumber, 40, 110)"
)).make;

PTScheme(\round, (members: "curve theme2")).make;
PT(\round).apply;
```

Without a reset, a controller carries on from where it was, which is usually what
is wanted. Name the controllers in the `reset` slot when a pass should start over.

```supercollider
PTScheme(\fromTheTop, (
	members: "gestures",
	reset:   "rhythmCeiling velocityFloor"
)).make;

PT(\fromTheTop).apply;
PT(\fromTheTop).applyTimes(3);
```

### Summary

**Objects** controller, scheme
**Methods** history, reset, apply, applyTimes, specify
**Tools** PT.takeOne, PT.lookup
**Miscellaneous** synchronised controllers relate two parameters exactly

---

## Tutorial 14: Communities

*Example file: `examples/04-combining-and-transforming.scd`*

A **community** is a group of sections joined in name only. It holds names, not
music. Its use is that it gives a handle on a group.

A community can list its members, or generate them as variants of one section.

```supercollider
PTCommunity(\voices, (members: "bass lead")).make;
PTCommunity(\family, (source: "lead", number: "5")).make;
```

The second makes `lead1` to `lead5`, all sharing the rules of `lead` and each with
its own seed. Remaking the community regenerates them under the same names rather
than piling up new ones.

A community can be folded into something that can be heard.

```supercollider
PT(\family).asSequence(\familyChain, 0.5);
PT(\family).asParallel(\familyCloud, 0.25);
```

- • Make the family again and fold it again. The chain is new material on the same
  rules.

### Summary

**Objects** community
**Methods** names, sections, makeAll, asSequence, asParallel, postValues

---

## Tutorial 15: Playing live

*Example file: `examples/07-live-and-export.scd`*

Every object so far keeps rules and one result of applying them. A **bind** keeps
only the rules. It has no events at all, and it does not end.

A bind plays through a `Pdef` named after itself, so an edit takes effect on the
next cycle rather than restarting.

```supercollider
PTBind(\live, (
	clock:    "150",
	rhythm:   "Prand([1, 1, 2, -1], inf)",
	pitch:    "Prand(cmajor, inf)",
	velocity: "Pwhite(50, 110)"
)).make;
```

- • Play it, then change a slot and make it again while it sounds.

```supercollider
PT(\live).spec.put(\pitch, "Prand(cmajor, inf) + Prand([0, 12], inf)");
PT(\live).make;
```

Anything else a `Pbind` accepts goes in the `extra` slot, as an Event:
`(pan: Pwhite(-0.8, 0.8), legato: 0.4)`.

### Keeping a take

A bind cannot be plotted or transformed, because there is nothing there yet.
Capture it and an ordinary section results.

```supercollider
PT(\live).capture(\take1, 64);
```

A capture keeps its own rules, so remaking it takes a new stretch of the same live
material, while `reproduce` brings back the take that was wanted. From there it is
a section like any other: plot it, transform it, combine it, save it.

### Summary

**Objects** bind, capture
**Methods** play, stop, make, capture, reproduce
**Miscellaneous** a bind has rules and no result

---

## Tutorial 16: Getting out

*Example file: `examples/07-live-and-export.scd`*

### As code

An object can say what it is in plain SuperCollider. Pitch names become numbers
and a referenced stockpile is written out as its values, so the result stands on
its own with the Toolbox uninstalled.

```supercollider
PT(\section1).asPbindSource;
PT(\section1).asSource;
```

The first gives the rules as a `Pbind`, the second gives the events themselves,
exactly. This is worth knowing when the object list has been doing the thinking
and you want to see what an object actually is.

### As a Midi file

```supercollider
PT(\section1).writeMidi("~/Desktop/sketch.mid");
```

A format 0 file with one tick to the millisecond. Chords are written as
simultaneous note ons, rests leave their time empty, and channel and velocity are
carried through.

### As a sound file

```supercollider
PT(\section1).render("~/Desktop/sketch.wav");
```

Rendered offline, faster than real time. The SynthDefs travel with the score,
since an offline server has none of its own.

`PT(\section1).asScore` gives the score itself, if something is to be added to it
before rendering.

### To a Midi device

```supercollider
PT.midiOut_(MIDIOut(0));
PT(\section1).playMidi;
```

### Summary

**Methods** asSource, asPbindSource, writeMidi, render, asScore, playMidi

---

## Index of objects

| object | slots |
| --- | --- |
| `PTDataSection` | clock, number, rhythm, pitch, velocity, channel, instrument |
| `PTNoteSection` | clock, number, notes, instrument |
| `PTDensity` | time, number, attacks, duration, pitch, velocity, channel, instrument |
| `PTDensityCurve` | time, curve, min, max, unit, duration, pitch, velocity, channel, instrument |
| `PTSequence` | sections |
| `PTParallel` | sections |
| `PTTimed` | sections, times |
| `PTDerived` | source, transform |
| `PTCapture` | source, number |
| `PTBind` | clock, rhythm, pitch, velocity, channel, instrument, extra |
| `PTStockpile` | source, number |
| `PTShape` | source, number |
| `PTMask` | top, bottom, number |
| `PTNoteStructure` | source |
| `PTCommunity` | members, source, number |
| `PTController` | source, syncTo |
| `PTScheme` | members, reset |

Common to every object: `make`, `reproduce`, `specify`, `remake`, `variant`,
`edit`, `plot`, `postSpec`, `postInfo`, `comment_`.

Common to every section: `play`, `stop`, `notes`, `numNotes`, `duration`,
`extract`, `textScore`, `asSource`, `asPbindSource`, `writeMidi`, `render`,
`asScore`.

---

## Index of tools

| tool | what it does |
| --- | --- |
| `PT.browse` | open the object list |
| `PT.save`, `PT.load` | write or read an environment |
| `PT.post` | list every object with its type, length and seed |
| `PT.stopAll` | stop everything sounding |
| `PT.bpm`, `PT.mm` | beats per minute, or a metronome mark, as a clock unit |
| `PT.untilTime` | in a number slot: fill this many seconds |
| `PT.fromNumber` | how many notes the section is making |
| `PT.midinote`, `PT.dyn` | a pitch name or a dynamic, as a number |
| `PT.convert` | interpret a shape or a mask in a range |
| `PT.readFrom` | read a collection in the order a curve describes |
| `PT.attacks` | attack points from intervals |
| `PT.interpolate` | gradually turn one object into another |
| `PT.lookup` | map values through a table of pairs |
| `PT.takeOne` | one value from a controller, for the whole section |
| `PT.melody` | a melody written as rhythm and pitch |
| `PT.note`, `PT.rest`, `PT.delay`, `PT.seq`, `PT.par` | build a note structure |
| `PT.asList` | notes out of a structure or a section |

Transformers: `PT.transpose`, `PT.louder`, `PT.add`, `PT.multiply`, `PT.set`,
`PT.limit`, `PT.fold`, `PT.quantize`, `PT.stretch`, `PT.filter`, `PT.reject`,
`PT.mute`, `PT.dedupe`, `PT.reverse`, `PT.slice`, `PT.keep`, `PT.drop`,
`PT.transformIf`.

Patterns added by the Toolbox: `PTbeta`.

---

*The Pattern Toolbox is an independent reimplementation of the ideas of Paul
Berg's AC Toolbox, and carries no AC Toolbox code. The AC Toolbox is at
[actoolbox.net](https://www.actoolbox.net).*
