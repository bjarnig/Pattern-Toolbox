// The note tree: the material of a note structure.
//
// A data section calculates rhythm, pitch and velocity independently of each
// other, which is excellent for making material and useless for writing down a
// melody you already know. A note tree treats a note as one indivisible thing,
// and lets notes be nested in sequence and in parallel.
//
// Rhythm here is in clock units, not seconds. A section supplies the clock.
//
//   PT.note(1, \c4)                    a note
//   PT.rest(2)                         two units of silence, kept as an event
//   PT.delay(1)                        one unit of nothing at all
//   PT.seq(a, b, c)                    one after another
//   PT.par(a, b)                       at the same time
//   PT.melody("1 c4 1 d4 2 e4 1 r")    the same, written out

PTNoteTree {

	// Every leaf in order of appearance, rests included, delays not.
	notes { ^this.subclassResponsibility(thisMethod) }

	// [startInClockUnits, note] pairs.
	asTimed { |offset = 0| ^this.subclassResponsibility(thisMethod) }

	// Length in clock units.
	duration { ^this.subclassResponsibility(thisMethod) }

	soundingNotes { ^this.notes.reject(_.isRest) }

	length { ^this.notes.size }

	size { ^this.length }

	at { |index| ^this.notes[index] }

	do { |func| ^this.notes.do(func) }

	collect { |func| ^this.notes.collect(func) }

	asArray { ^this.notes }

	isEmpty { ^this.notes.isEmpty }

	// Flatten the tree: everything in sequence, nothing in parallel. This is what
	// happens when a note structure is used as a supply of notes to choose from.
	flat { ^PTNoteSeq(this.notes) }
}


PTNote : PTNoteTree {
	var <>rhythm, <>pitch, <>velocity, <>channel, <>kind;

	*new { |rhythm = 1, pitch = 60, velocity = 64, channel = 1, kind = \note|
		^super.newCopyArgs(rhythm, PT.midinote(pitch), PT.velocity(velocity), channel, kind)
	}

	isRest { ^kind == \rest }
	isDelay { ^kind == \delay }

	notes { ^if(this.isDelay) { [] } { [this] } }

	asTimed { |offset = 0| ^if(this.isDelay) { [] } { [[offset, this]] } }

	duration { ^rhythm.abs }

	copy { ^PTNote(rhythm, pitch, velocity, channel, kind) }

	// One event, with the section's clock applied.
	asEvent { |unit = 0.1, instrument = \default|
		var event = (
			dur: this.duration * unit,
			sustain: this.duration * unit,
			midinote: pitch,
			velocity: velocity,
			chan: channel,
			instrument: instrument
		);
		if(this.isRest) { event[\type] = \rest };
		^event
	}

	printOn { |stream|
		stream << "PTNote(" << rhythm << ", ";
		if(this.isRest) { stream << "rest" } { stream << pitch };
		stream << ")";
	}
}


PTNoteSeq : PTNoteTree {
	var <>items;

	*new { |items| ^super.newCopyArgs(items.asArray.collect { |i| PT.asNoteTree(i) }) }

	notes { ^items.collect(_.notes).flatten }

	duration { ^items.sum(_.duration) }

	asTimed { |offset = 0|
		var cursor = offset, out = Array.new;
		items.do { |item|
			out = out ++ item.asTimed(cursor);
			cursor = cursor + item.duration;
		};
		^out
	}

	printOn { |stream| stream << "PTNoteSeq" << items }
}


PTNotePar : PTNoteTree {
	var <>items;

	*new { |items| ^super.newCopyArgs(items.asArray.collect { |i| PT.asNoteTree(i) }) }

	notes { ^items.collect(_.notes).flatten }

	duration { ^items.collect(_.duration).maxItem ? 0 }

	asTimed { |offset = 0|
		^items.collect { |item| item.asTimed(offset) }.flatten
	}

	printOn { |stream| stream << "PTNotePar" << items }
}
