# Changelog

## 1.0.0

First release.

A composition environment in the spirit of Paul Berg's AC Toolbox, built over
SuperCollider patterns. An object has a name, a set of rules, and one specific
result of applying them; both are kept, and every result records the random seed
that produced it, so nothing you liked is ever lost.

### Objects

Seventeen types. Sections calculated per parameter (`PTDataSection`), from note
structures (`PTNoteSection`), or by filling time (`PTDensity`,
`PTDensityCurve`). Sections combined in sequence, in parallel, or at given times
(`PTSequence`, `PTParallel`, `PTTimed`), and transformed into new named objects
(`PTDerived`). Material as collections (`PTStockpile`), curves (`PTShape`),
tendency fields (`PTMask`) and note trees (`PTNoteStructure`). Groups
(`PTCommunity`), state that outlives a section (`PTController`), remake scripts
(`PTScheme`), and a live layer (`PTBind`, `PTCapture`).

### Interface

An object browser, dialogs generated from each class's slot declarations, mouse
drawing for shapes and masks, and a piano roll. Light palette throughout.

### In and out

An archive format that is plain SuperCollider source, so environments are
readable and comparable and reload to the exact material. Export as a Pbind or
as events, as a standard MIDI file, or rendered offline to a sound file.

### Beyond the original

Results are reproducible from their seed. Transformation is an object rather than
a mutation, so a chain can be remade from its source. Material can be played
through a `Pdef` and altered while it sounds.

### Verified

300 assertions. Every code block in the tutorial has been run in order. MIDI
files were parsed back outside SuperCollider; offline renders were checked by
measuring the samples rather than the file size.

### Not in this release

The extended generator library (Koenig's selection principles, chaos, 1/f,
transition tables, mutations), MIDI input, XY density sections, and most of the
`.schelp` reference. `PT.midiOut` exists but is untested against hardware.
