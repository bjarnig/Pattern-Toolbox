// PTNoteStructure - a named note tree.
//
// This is where a melody you already know goes, or any group of notes whose
// pitches and rhythms belong together rather than being calculated apart.
//
//   PTNoteStructure(\theme, (source: "PT.melody(\"1 c4 1 d4 2 e4 1 r 1 c4\")")).make;
//   PTNoteStructure(\chords, (source: "PT.seq(PT.par(PT.note(2, c4), PT.note(2, e4)))")).make;
//
// Used bare in a note section it is played as written, parallelism and all. Used
// through a generator it is flattened first and becomes a supply of notes to
// choose from, which is how the AC Toolbox turns a known melody into material.

PTNoteStructure : PTObject {
	var <tree;

	*ptType { ^\notes }

	*slotSpecs {
		^[[\source, "", "a note tree, a list of notes, a melody string, or a section"]]
	}

	*of { |name, source| ^this.new(name, (source: source)).make }

	realize {
		var source = spec.value(\source);
		if(source.isNil) { tree = PTNoteSeq([]); ^[] };
		tree = PT.asNoteTree(source);
		^tree.notes
	}

	// ------------------------------------------------------------- the material

	notes { ^this.prRequireMade.value }

	soundingNotes { ^this.notes.reject(_.isRest) }

	// Length in clock units, which a section turns into seconds.
	beats { ^this.prRequireMade.tree.duration }

	// A note structure resolves to itself, so a note section can see the
	// structure. The collection protocol below is what lets it also be used
	// directly by Prand and friends, which want something list-like.
	asPTValue { ^this }

	size { ^this.notes.size }
	at { |index| ^this.notes[index] }
	do { |func| ^this.notes.do(func) }
	collect { |func| ^this.notes.collect(func) }
	reverse { ^this.notes.reverse }
	isEmpty { ^this.notes.isEmpty }
	asArray { ^this.notes }

	asStream { ^PT.asStream(this.notes) }

	// ------------------------------------------------------------------ display

	plot { ^this.notes.collect(_.pitch).plot(name.asString) }

	postValues {
		"% : % notes, % beats".format(name, this.size, this.beats).postln;
		this.notes.clump(8).do { |row| row.join("  ").postln };
		^this
	}
}
