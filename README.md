# Pattern Toolbox

Named, browsable, reproducible musical objects for SuperCollider.

A composition environment in the spirit of Paul Berg's **AC Toolbox**. You make an
object, it gets a name, and you can look at it, hear it, edit its rules, remake it,
make a variant of it, and refer to it from any other object. An object keeps both
its rules and one specific result of applying them.

![an object and its rules](doc/images/object-dialog.png)

## Install

Clone into your SuperCollider `Extensions` folder and recompile the class library.
There are no dependencies.

```
~/Library/Application Support/SuperCollider/Extensions/Pattern-Toolbox
```

## A first object

```supercollider
PTStockpile(\cmajor, (source: "c4 d4 e4 f4 g4 a4 b4")).make;

PTDataSection(\section1, (
	clock:    "150",                    // the beat, in milliseconds
	number:   "50",                     // how many notes
	rhythm:   "Prand([1, 2, -1], inf)", // multiples of the beat, negative for a rest
	pitch:    "Prand(cmajor, inf)",     // the stockpile, by name
	velocity: "mf"
)).make;

PT(\section1).play;
PT(\section1).plot;

PT(\section1).variant;      // same rules, new result, called section1a
PT(\section1).reproduce;    // the result you had before, exactly

PT.browse;                  // the object list
PT.save("~/Desktop/sketch.ptx");
```

Every slot is text, and every slot accepts the same kinds of thing: a number, a
pitch name, a list, any SuperCollider pattern, or the name of another object.

## Documentation

`doc/TUTORIAL.md` is the tutorial, in sixteen short chapters. The same thing as a
single page is at `doc/tutorial.html`.

Worked examples for each chapter are in `examples/`.

## Credit

The AC Toolbox is by Paul Berg, at [actoolbox.net](https://www.actoolbox.net).
This is an independent reimplementation of its ideas in SuperCollider, and carries
no AC Toolbox code.

GPL-3.0. See `LICENSE`.
