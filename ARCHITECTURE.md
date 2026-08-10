# Pattern Toolbox: Architecture

A SuperCollider quark that gives Pbind the thing Pbind has never had: named,
browsable, editable, reproducible musical objects.

The design is a deliberate translation of Paul Berg's **AC Toolbox** into
SuperCollider idiom. This document records what was taken, what was changed, and
why.

---

## 1. What AC Toolbox actually is

Stripped of Lisp and of its Mac GUI, the AC Toolbox rests on four ideas.

**1. A named global environment of objects.** Everything you make gets a name
and lives in one flat registry. The Objects dialog is a table over that
registry: filter by type, click a name, get its dialog back, edit it, remake it.

**2. Every object stores its input and its output.** From Tutorial 6:

> Each AC Toolbox object records the rules by which it was specified (input) and
> the specific results produced by using those rules (output). The general (the
> input) and the specific (the output) are both maintained. Composition
> sometimes requires both good design and good luck.

This is the load-bearing idea of the whole system. **Make** applies the rules.
**Variant** applies the same rules again under a new name. **Specify** stores
the rules without applying them.

**3. Every field is an expression.** Any slot of any dialog accepts a constant,
a list, a symbol, the name of another object, a generator, or a tool call,
uniformly. That uniformity is why the system composes: a stockpile goes
anywhere a list goes, a mask goes anywhere a number source goes.

**4. Generators are stateful closures.** `(random-value 1 100)` returns a thing
you pull successive values out of. That is exactly a SuperCollider `Stream`.

The `.acex` save format confirms the model. It is plain Lisp source:

```lisp
(define mask1 (make-instance 'mask :input '(draw-mask)
      :top '(100.0 100.0 99.242424 ...)
      :bottom '(...)))
```

Both the input rule and the realized data, in one readable text file.

## 2. The translation

| AC Toolbox | Pattern Toolbox |
| --- | --- |
| generator (closure) | `Pattern` → `.asStream` |
| stockpile | `PTStockpile`, realizes to an `Array` |
| shape / mask | `PTShape` / `PTMask`, breakpoint curves |
| section | `PTSection`, realizes to an `Array` of `Event`s |
| note structure | `PTNoteStructure`, a nested seq/par tree |
| controller | `PTController`, a stream living outside section scope |
| scheme | `PTScheme`, an ordered remake script |
| community | `PTCommunity`, sections grouped by name |
| Csound object, OSC score | gone: `\instrument` plus a SynthDef, and NRT export |
| stream (real time) | `PTBind`, backed by `Pdef` |
| environment file (.acex) | archive file (.ptx), sclang source |

The critical mapping is this: **an AC Toolbox generator is a SuperCollider
Stream, and an AC Toolbox object is a Pbind that has been frozen into events.**
Pbind alone is only the input half. Pattern Toolbox supplies the output half,
the name, and the browser.

## 3. Core model

Three classes carry the system.

### PTSpec: the input half

An ordered set of named slots whose values are **always Strings**: the literal
text a user typed. Text is the truth. Nothing compiled is ever stored, so a spec
round-trips to source without loss and diffs cleanly.

```
spec.at(\pitch)          -> "Prand(cmajor, inf)"
spec.value(\pitch)       -> compiles and runs it in the toolbox environment
spec.valueAsList(\pitch) -> the same, but a bare token run is read as a list
```

### PTObject: the object

```
name     spec     value     seed     madeAt     comment
```

| method | meaning |
| --- | --- |
| `make` | apply the rules with a fresh seed. This is the Make button. |
| `make(seed)` | apply the rules with a given seed |
| `reproduce` | re-apply with the stored seed. Identical output. |
| `specify` | store the rules, discard any realization |
| `remake` | apply again in place. This is what a scheme does. |
| `variant(name)` | same rules, new name, new result |
| `realize` | subclass hook. Does the work, returns the value. |

### PT: the registry and the environment

`PT(\name)` looks up. `PT.names(\section)` filters by type. `PT.save`,
`PT.load`, `PT.eval`. The class also holds `PT.envir`, the `Environment` in
which every spec is compiled, so any registered object is reachable as `~name`.

## 4. Name resolution: the one genuinely hard problem

The AC Toolbox gets bare names for free because it is Lisp with a global symbol
table. sclang has no dynamic global identifiers, so a spec field containing
`cmajor` will simply not compile.

**Solution.** Specs are evaluated inside a dedicated `Environment`, so the
canonical syntax is `~cmajor`, which is ordinary SuperCollider and needs no
magic. On top of that, `PT.resolve` runs a source rewrite before compilation
(controlled by `PT.sugar`, default on):

| written | becomes | rule |
| --- | --- | --- |
| `cmajor` | `~cmajor` | matches a registered object name |
| `c4` `cs4` `c#4` `ef4` `bf-1` | `60` `61` `61` `63` `22` | matches a pitch name |
| `mf` `ff` `ppp` | `64` `96` `16` | matches a dynamic (2 characters or more) |
| `Pwhite` `array.c4` `\default` `"c4"` `round: 1` | unchanged | class name, method call, symbol literal, string literal, keyword argument |

The rewriter is a hand-written character scanner, not a regular expression,
because it has to know when it is inside a string or a symbol literal. Single
character `p` and `f` are deliberately **not** rewritten, since they are far too
common as variable names; write `\p` and `\f`, which the velocity slot
understands anyway.

`PTObject.asPTValue` decides what `~name` yields. The default is the object
itself; `PTStockpile` overrides it to yield its `Array`, which is what makes
`Prand(cmajor, inf)` read naturally. The binding is refreshed on every `make`.

## 5. Two things we do better than the original

**Reproducible variants.** Every realization runs inside a `Routine` whose
`randSeed` is set and stored. The AC Toolbox can make a variant but can never
get an old one back. Here `make` gives you a new result every time (matching the
Make button), `reproduce` gives you the stored one exactly, and a saved archive
regenerates identical output because the seed travels with the spec. Students
can hand in a seed.

**A live layer.** AC Toolbox sections are frozen blocks fired at MIDI.
SuperCollider gives us `Pdef`, so a section plays through a `Pdef` named after
itself and hot-swaps when you press Make while it is sounding. `PTBind` will be
the purely live object: spec only, no frozen output, plays forever, with
`capture` to freeze it into a section and `asBind` to go the other way.

## 6. Representation decisions

**A section realizes to a flat `Array` of `Event`s.** Not to a Pattern. The
whole AC Toolbox editing model (slice, join, filter, transform, plot, inspect)
needs random access to the realized notes.

**Durations are in seconds**, and playback uses a `TempoClock` at 1 beat per
second (`PT.clock`). The clock unit of a section is in milliseconds, as in the
original, and `rhythm` is a multiple of it. `PT.bpm(160)` and `PT.mm(120)` are
available for the clock slot.

**Rests are kept as events** of `type: \rest`, rather than discarded. The
original stores only sounding notes, which loses information; keeping them means
timing survives, `numNotes` stays computable, and `\rest` is already a valid
SuperCollider event type.

**A flat pitch array is a sequence; a nested array is a chord.** `[c4, e4, g4]`
gives three successive notes, `[[c4, e4, g4]]` gives one triad. This matches the
AC Toolbox convention, where a list of chords is a list of lists.

**A drawn curve is still a text specification.** Accepting a drawing writes the
point array back into the spec as a literal, exactly as `.acex` stores
`:input '(draw-mask)` alongside the literal line data. Nothing in the system has
to know that a curve came from a mouse, and a drawn shape still saves, diffs and
reloads like everything else.

**`edit` and `draw` are different verbs.** `edit` always opens the text dialog,
for every object type, so the interface stays predictable; `draw` opens the mouse
editor and only shapes and masks answer it. The browser and the dialog show a
draw button only when the selected object responds to it.

**\dur is a delta, \sustain is a length.** A single section has them equal. A
combination does not: two voices sounding at the same instant produce one event
with `\dur: 0` and a full `\sustain`. Separating the two is what lets voices
overlap at all, and it means the piano roll must take its bar width from
`\sustain` and advance its clock by `\dur`.

**A transformation is an object, not a mutation.** The AC Toolbox applies a
transformer to an object in place, which quietly breaks its own central
invariant: afterwards the object's rules no longer describe its result. Here a
transformed section is a `PTDerived` whose rules name a source and a list of
transforms. Remake it and the transformation runs again; remake the source first
and the whole chain follows. This also settles the open question about editing
realized output: you never edit output, you derive from it.

**Velocity is stored, amp is derived.** Events carry `\velocity` (0 to 127) and
`asPattern` computes `\amp` from it at playback time. Storing both invited a
whole class of bug where a transform raised the velocity and nothing got louder.

**A community that generates variants must be defined before them in an
archive.** `PTCommunity` realization has a side effect: it creates the variant
objects. Creation order guarantees the right order in a saved file, since the
variants come into existence during the community's own first make. Worth knowing
before hand-editing a `.ptx`.

**A controller is the one object whose value changes outside make.** Its `value`
is its history, and `make` resets it. That is a deliberate exception to the
spec-to-value invariant, because a controller's whole purpose is to hold state
that outlives any single realization. The history is still reproducible: values
are pulled inside a `Routine` whose `randSeed` was set at reset, so a given seed
replays a given history exactly.

**A controller cycles a finite source.** A list source already cycled, so a
finite pattern must too. A controller is asked for values an unknown number of
times and must never run dry; the first version returned `nil` and fed it into
arithmetic several objects downstream, which was unfindable from the error.

**Generated members are frozen when saved.** A `PTCommunity` that makes variants
writes its resolved member names into the archive alongside the generative rule.
Without that, loading regenerates the variants and then their own definitions
remake them, so every controller in play is asked for twice as many values as it
was originally and nothing reloads faithfully. This was measured, not assumed:
before the fix a five-member community consumed ten controller values and came
back with different lengths.

**Stockpile has one source slot, not three dialogs.** The original separates
specify, generate and construct. Here the mode is inferred from what the source
evaluates to: a bare token run is a list, an expression producing a list is a
list, a pattern plus a number is drawn that many times. Less to teach, and more
SuperCollider-idiomatic.

## 7. Class tree

```
PTObject                     name, spec, value, seed; make / specify / variant
├── PTStockpile              a named collection of values                    [done]
├── PTShape                  one curve over time; drawn, specified, generated [done]
├── PTMask                   two curves: a tendency field                    [done]
├── PTNoteStructure          aNote / aRest / aDelay / inSeq / inPar tree
├── PTSection (abstract)     output is an Array of Events                    [done]
│   ├── PTDataSection        per parameter, calculated independently         [done]
│   ├── PTNoteSection        driven by note structures
│   ├── PTDensitySection     driven by time, filled by a function or a shape
│   ├── PTDerived            another section, transformed                    [done]
│   └── PTCombination        PTSequence | PTParallel | PTTimed              [done]
├── PTController             a stream with state, history and reset       [done]
├── PTScheme                 an ordered remake list                        [done]
├── PTCommunity              sections grouped in name only                   [done]
├── PTBind                   live, Pdef-backed; no frozen output
└── PTCode                   a saved sclang snippet
```

## 8. Repository layout

```
Pattern-Toolbox/
  quark.json
  ARCHITECTURE.md   README.md
  Classes/
    core/         PT PTObject PTSpec
    objects/      PTStockpile PTSection PTDataSection ...
    generators/   PTbeta PTfractal1f PTkoenigAlea PThenon PTlorenz ...
    tools/        conversion, filtering, ordering
    transformers/ PTTransform and its subclasses
    gui/          PTBrowser PTObjectView PTShapeEditor PTMaskEditor PTPianoRoll
    io/           PTArchive PTMidiFile PTScoreExport PTSourceExport
  HelpSource/Classes/*.schelp
  tests/          TestPatternToolbox.sc
  examples/
```

Generators are written as real `Pattern` subclasses rather than as ad-hoc
closures, so they work in a plain Pbind outside the toolbox. That matters
pedagogically: the generator library stays useful to a student who graduates off
the GUI.

## 9. The archive format

A `.ptx` file is plain sclang source, one `PT.def` per object in creation order,
mirroring what `.acex` does with Lisp `define` forms.

```supercollider
PT.def('cmajor', PTStockpile, (
	source: "c4 d4 e4 f4 g4 a4 b4",
	number: "",
), seed: 481293, comment: "");

PT.def('section1', PTDataSection, (
	clock: "150",
	number: "50",
	rhythm: "Prand([1, 2, 3], inf)",
	pitch: "Prand(cmajor, inf)",
	velocity: "\\mf",
	channel: "1",
	instrument: "\\default",
), seed: 730114, comment: "first sketch");
```

Human readable, diffable, version controllable, and loading it reproduces the
exact realization rather than merely the rules.

## 10. GUI plan

SuperCollider Qt only, so the whole thing installs as a plain quark with no
external dependency.

All of the following exist except `PTIndex`. One class, `PTCurveEditor`, covers
both shape and mask drawing, because a shape is a mask with one line.

- **`PTBrowser`**, the persistent window. Type popup, filter field, sortable
  table of name / type / length / made. Buttons: Make, Specify, Play, Stop,
  Plot, Info, Input, Variant, Remove. Names drag into any spec field.
- **`PTObjectView`**, a generic form builder. A subclass declares `slotSpecs`
  and the dialog is generated from it. One view class covers most object types.
- **`PTCurveEditor`**, `UserView` mouse drawing for shapes and masks, as in the
  original Draw dialogs, with flat, invert, smooth and normalise.
- **`PTPianoRoll`**, plot for anything with `asEvents`, plus per-parameter plots.
- **`PTIndex`**, a searchable palette of generators, tools and transformers,
  drag to insert, backed by the schelp documentation so there is one source of
  documentation truth.

The GUI holds no state. It only reads and writes `spec` and calls `make`.

Two mechanisms keep it live. First, `PT` and every `PTObject` post `changed`
notifications (`\objects` when the registry changes, `\made` when a realization
changes), and the browser and the open dialogs are dependants, so nothing needs
manual refreshing. Second, the parts of the GUI that contain real logic are pure
class methods, so they are testable without a display:
`PTBrowser.filterNames`, `PTObjectView.parenBalance`, `PTPianoRoll.computeLayout`.

One behaviour is worth naming because it is lifted directly from the tutorial:
typing a new name into a dialog's name field and pressing Make creates a **new**
object with the same rules and the dialog follows it. That is how the AC Toolbox
tutorial asks you to make section2 out of section1.

## 11. Build order

| Phase | Content | Gate | Status |
| --- | --- | --- | --- |
| 0 | `PT`, `PTObject`, `PTSpec`, `PTStockpile`, `PTDataSection`, archive | Tutorials 1 and 2 reproducible from the interpreter, under test | **done** |
| 1 | `PTBrowser`, generic object view, Make / Variant / Play / Plot, piano roll | Tutorials 1 and 2 reproducible by clicking | **done** |
| 2 | `PTShape`, `PTMask`, drawing editors, `convert` and `readFrom` | Tutorials 3 and 4 | **done** |
| 3 | Combinations, transformers, filters, `PTCommunity` | Tutorials 5, 7, 23, 25 | **done** |
| 4 | `PTController`, `PTScheme` | Tutorial 13, the level higher | **done** |
| 5 | `PTNoteStructure`, note and density sections | Tutorials 8, 9, 10 | next |
| 6 | `PTBind`, MIDI in and out, NRT and MIDI file export, Pbind source export | new ground | |
| 7 | Generator library: Koenig selection principles, chaos, 1/f, transition tables, mutations | Tutorial 24 and the Annotated Index | |

## 12. Open questions

1. ~~**Editing realized output.**~~ Settled in phase 3: output stays read-only and
   `PTDerived` covers the need. You transform a section into a new named object
   rather than editing its events, which keeps every object's rules honest.
2. **How far to carry MIDI.** The original is a MIDI program with sound synthesis
   bolted on. This is a SuperCollider program, so the SynthDef is primary and
   MIDI is an export target. `\chan` and `\velocity` are carried on every event
   so that export stays lossless.
3. **Naming for the teaching context.** Class prefix `PT` was checked against the
   class library and the installed extensions and is free.

## 13. Sources

- `AC_Toolbox_Tutorial.pdf`, Paul Berg, version of 23 March 2015, 261 pages.
- `cwa 2 - ACT intro.pdf`, Paul Berg, Composing with Algorithms class 2.
- `.acex` example files from the Composing with Algorithms course material.
- <https://www.actoolbox.net/documentation/>
