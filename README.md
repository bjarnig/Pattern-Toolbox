# Pattern Toolbox

Named, browsable, reproducible musical objects for SuperCollider.

A composition environment in the spirit of Paul Berg's **AC Toolbox**, built
over SuperCollider patterns. You make an object, it gets a name, you can look at
it, hear it, edit its rules, remake it, make a variant of it, and refer to it
from any other object. The rules and one specific result are both kept.

Status: **phase 1**. The object model, the stockpile, the data section, the
archive format, the browser, the object dialogs and the piano roll all work and
are covered by tests. Shapes and masks are next.

## Install

Clone into your Extensions folder and recompile:

```
~/Library/Application Support/SuperCollider/Extensions/Pattern-Toolbox
```

## Five minutes

```supercollider
// a named collection of values
PTStockpile.specify(\cmajor, "c4 d4 e4 f4 g4 a4 b4");

// a section: each parameter calculated independently
PTDataSection(\section1, (
	clock:    "150",                      // the beat, in milliseconds
	number:   "50",                       // how many notes
	rhythm:   "Prand([1, 2, 3], inf)",    // multiples of the beat
	pitch:    "Prand(cmajor, inf)",       // the stockpile, by name
	velocity: "\\mf"
)).make;

PT(\section1).play;
PT(\section1).plot;
PT(\section1).textScore;

// same rules, new result, new name
PT(\section1).variant(\section2).play;

// exactly the result you had before
PT(\section1).reproduce;

// save and reload the whole environment
PT.save("~/Desktop/sketch.ptx");
PT.load("~/Desktop/sketch.ptx");
```

## What you can write in a slot

Every slot is text, and every slot accepts the same kinds of thing.

| you write | you get |
| --- | --- |
| `60` | a constant |
| `c4` | a pitch name, middle C is `c4` |
| `cs4` `c#4` `ef4` `bf-1` | sharps, flats, negative octaves |
| `mf` `ff` `ppp` | dynamics as velocities |
| `1 2 3` or `c4 e4 g4` | a list, which cycles |
| `[[c4, e4, g4]]` | a chord (a flat array is a sequence, a nested one is a chord) |
| `Pwhite(60, 72)` | any SuperCollider pattern |
| `cmajor` | any object you have already made, by name |
| `PT.untilTime(10)` | in the number slot: fill ten seconds |
| `PT.bpm(160)` | in the clock slot |
| `-1` | in the rhythm slot: a rest of that length |

Bare names are rewritten before compiling: an object name becomes `~name`, a
pitch name becomes a note number, a dynamic becomes a velocity. Turn it off with
`PT.sugar = false` and write `~cmajor` and `60` yourself.

## The browser

```supercollider
PT.browse;          // the object list: filter, select, act
PT(\section1).edit; // one object's dialog
PT(\section1).plot; // piano roll
```

In the list: double click to open a dialog, drag a name into any field of any
other dialog, and the `*` marks objects that have been made. In a dialog: type
into the fields and press make, or change the name first and press make to get a
new object with the same rules. The `...` button opens a bigger editor with a
parenthesis check.

Playback goes through a `Pdef` named after the object, so you can edit and press
make while it is sounding and the next cycle picks it up.

## Object lifecycle

| | |
| --- | --- |
| `make` | apply the rules, fresh result |
| `reproduce` | apply the rules with the stored seed, identical result |
| `specify` | store the rules without applying them |
| `variant(name)` | same rules, new name, new result |
| `remake` | apply again in place |

Every realization stores the random seed that produced it, so nothing you liked
is ever lost.

## Tests

```supercollider
TestPatternToolbox.run;
```

## Documentation

`ARCHITECTURE.md` explains the design, what was taken from the AC Toolbox, what
was changed and why, and the phase plan.

## Credit

The AC Toolbox is by Paul Berg. This is an independent reimplementation of its
ideas in SuperCollider, not a port, and it carries no AC Toolbox code.
