// Pattern Toolbox, example 2
// Working by clicking rather than by typing.

// Open the browser. Leave it open: it is the AC Toolbox Objects dialog.
PT.browse;

// Make a few things and watch them appear in the list.
(
PTStockpile.specify(\cmajor, "c4 d4 e4 f4 g4 a4 b4");
PTStockpile.generate(\row, "Pshuf((60..71), 1)", "12");

PTDataSection(\section1, (
	clock:    "150",
	number:   "50",
	rhythm:   "Prand([1, -1, 2], inf)",
	pitch:    "Prand(cmajor, inf)",
	velocity: "Pwhite(40, 100)"
)).make;
)

// In the browser:
//
//   double click a name        open its dialog
//   drag a name into a field   insert it into another object's rules
//   make                       run the rules again, new result
//   variant                    same rules, new name (section1a, section1b ...)
//   plot                       piano roll for sections, a curve for stockpiles
//   play / stop                Pdef-backed, so it hot swaps while sounding
//   the * in the list          this object has been made
//
// In an object dialog:
//
//   type in any field, then make
//   change the name, then make    a NEW object with the same rules
//   specify                       keep the rules, throw the result away
//   ...                           a bigger editor with a parenthesis check

// Open a dialog straight from code.
PT(\section1).edit;

// The piano roll on its own.
PT(\section1).plot;
PT(\section1).plot(\velocity);      // a single parameter instead

// Editing while it plays: press play, then change pitch in the dialog and press
// make. The Pdef is named after the object, so the next cycle picks it up.
PT(\section1).play(inf);
PT(\section1).stop;

// Try this by hand, it is the whole point of the system:
//   1. select section1 in the browser and press variant a few times
//   2. compare section1a, section1b, section1c by selecting and pressing play
//   3. keep the one you like, remove the others
//   4. press save

PT.stopAll;
