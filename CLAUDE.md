# Pattern Toolbox

A SuperCollider quark: named, browsable, reproducible musical objects, in the spirit
of Paul Berg's AC Toolbox. `ARCHITECTURE.md` has the full design and the reasoning
behind each decision; `doc/TUTORIAL.md` is the user-facing tutorial. Read those
before changing anything structural.

## Commands

```bash
SC=/Applications/SuperCollider.app/Contents/MacOS/sclang

# the suite: 300 assertions, must be 0 failures
echo '( TestPatternToolbox.run(false, false); 0.exit; )' > /tmp/run.scd
timeout 300 $SC /tmp/run.scd 2>&1 | grep -cE '^PASS'

# regenerate every screenshot in doc/images
echo '( "'"$PWD"'/doc/make-screenshots.scd".load; { Window.allWindows.do(_.close); 0.exit }.defer(15); )' > /tmp/shoot.scd
timeout 180 $SC /tmp/shoot.scd 2>&1 | grep -c '^wrote'
```

A `timeout` is not optional and every scratch file must end with `0.exit`, or sclang
sits forever. Skills: `supercollider-verify` for the language side,
`supercollider-gui` for anything that draws.

## Invariants

Break one of these and something goes silently wrong rather than raising.

- **Slot values are always Strings.** The text a user typed is the truth. Nothing
  compiled is ever stored, so a spec round-trips to source without loss.
- **An object keeps its rules and one result.** `make` applies the rules with a fresh
  seed, `reproduce` replays the stored one, `specify` stores rules without applying
  them. Every realization records its seed; that is what makes results recoverable.
- **`\dur` is the time to the next event, `\sustain` is how long a note sounds.**
  Equal in one voice, different the moment voices overlap: coincident notes give one
  event a `\dur` of zero. Advance clocks by `\dur`; draw and export lengths from
  `\sustain`.
- **Events store `\velocity`, not `\amp`.** Amp is derived in `asPattern`, so any
  transform touching velocity is automatically audible.
- **A flat pitch array is a sequence; a nested one is a chord.**
- **Rests are events** of `type: \rest`, kept rather than discarded.
- **Rhythm in a note tree is in clock units**, not seconds, until a section supplies
  the clock.
- **GUI capabilities are declared, not probed.** `canDraw`, `canApply`, `canReset` on
  `PTObject`. `respondsTo` is useless here because `Object:reset` exists.

## Conventions

- Class prefix `PT`, checked free across the class library and installed quarks.
- One new object type = a class with `*ptType`, `*slotSpecs` and `realize`. The
  dialog builds itself from `slotSpecs`; no view code needed.
- Anything in the GUI with real logic goes in a **pure class method** so it can be
  tested headless (`PTBrowser.filterNames`, `PTPianoRoll.computeLayout`,
  `PTObjectView.parenBalance`, `PTCurve.*`).
- Colours come from `PTGUI.color`; never a `Color(...)` literal in a view.
- Generators are real `Pattern` subclasses, so they work in a plain Pbind outside the
  toolbox.

## Working rhythm

Each phase: build, unit tests, **regenerate the screenshots and look at them**, update
`ARCHITECTURE.md` and `README.md`, add an example under `examples/`, commit, push.

Looking at the screenshots is not decoration. Six real bugs have come out of it that
the test suite could not see, because layout and colour have no assertions. Doing it
at the end of every phase is why they were caught early.

Verify tutorial and example code by extracting the blocks and running them **in
order** in one session, which is what catches a forward reference to an object the
text has not introduced yet.

## Where it stands

Phases 0 to 6 are done: object model, stockpiles, data sections, note structures and
note sections, density sections, shapes and masks with drawing editors, combinations,
transformers and filters, communities, controllers and schemes, a live `Pdef`-backed
layer with capture, MIDI file and offline render export, source export, the archive
format, the browser and dialogs, the piano roll.

Phase 7 is the extended generator library: Koenig's selection principles, chaos,
1/f, transition tables, mutations. Also outstanding: MIDI **input** (deferred because
there is no device here to verify it against — MIDI output is written but likewise
unverified against hardware, and marked as such in the source), and XY density
sections, which need the chaos generators first.
