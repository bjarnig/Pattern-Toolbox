// PTNoteSection - a section whose parameters arrive together.
//
// In a data section rhythm, pitch and velocity are calculated separately. Here
// they come as whole notes, from a note structure or from anything that supplies
// notes. The AC Toolbox splits this into two dialogs, a structured section and a
// note section; the difference is only whether the input is a structure or a
// stream of notes, so one class detects which and does the right thing:
//
//   notes: "theme"                     played as written, parallelism kept
//   notes: "Pxrand(theme, inf)"        flattened, then chosen from
//   notes: "PT.readFrom(theme, arch)"  flattened, read in the order of a shape
//   notes: "PT.interpolate(a, b, 200)" gradually turning one into the other
//
// A section can serve as a supply of notes too, via PT.asList. Its rhythms then
// arrive in seconds, so a clock of 1000 reproduces the original timing.

PTNoteSection : PTSection {

	*ptType { ^\section }

	*slotSpecs {
		^[
			[\clock, "100", "the basic beat in milliseconds"],
			[\number, "", "how many notes, or PT.untilTime(seconds). Empty means all of them."],
			[\notes, "", "a note structure, or anything that supplies notes"],
			[\instrument, "\\default", "SynthDef name used for playback"]
		]
	}

	*of { |name, notes, clock = 100, number|
		^this.new(name, (notes: notes, clock: clock.asString,
			number: if(number.isNil) { "" } { number.asString })).make
	}

	realize {
		var unit = PT.scalar(spec.value(\clock, 100)) / 1000;
		var instrument = PT.scalar(spec.value(\instrument, \default)).asSymbol;
		var numberSpec = if(spec.isEmptyAt(\number)) { nil } { PT.scalar(spec.value(\number)) };
		var count, limit, source;

		if(numberSpec.isKindOf(PTUntilTime)) {
			limit = numberSpec.seconds;
			count = inf;
		} {
			count = numberSpec !? (_.asInteger);
		};

		PT.prSetCurrentNumber(if(count == inf) { nil } { count });
		source = spec.value(\notes);
		PT.prSetCurrentNumber(nil);

		if(source.isNil) { ^[] };

		// a structure is played as written; anything else is laid out in sequence
		if(source.isKindOf(PTNoteStructure)) { source = source.tree };

		// an empty number means all of them, which only a finite source can answer
		if(count.isNil and: { limit.isNil }) {
			count = case
				{ source.isKindOf(PTNoteTree) } { source.length }
				{ source.isKindOf(Pattern) or: { source.isKindOf(Stream) } } { nil }
				{ source.isKindOf(SequenceableCollection) } { source.size }
				{ 1 };
			if(count.isNil) {
				Error(
					"PT: note section % needs a number, because its notes come from a generator"
					.format(name)
				).throw
			};
		};
		^if(source.isKindOf(PTNoteTree) and: { source.isKindOf(PTNote).not }) {
			this.prRealizeTree(source, unit, count, limit, instrument)
		} {
			this.prRealizeStream(source, unit, count, limit, instrument)
		}
	}

	// Written as it stands: absolute positions from the tree, so notes that were
	// specified in parallel stay in parallel.
	prRealizeTree { |tree, unit, count, limit, instrument|
		var timed = tree.asTimed(0).sort { |a, b| a[0] <= b[0] };
		if(count.notNil and: { count != inf }) { timed = timed.keep(count) };
		if(limit.notNil) { timed = timed.select { |pair| (pair[0] * unit) < limit } };
		^PTCombination.flatten(
			timed.collect { |pair| [pair[0] * unit, pair[1].asEvent(unit, instrument)] }
		)
	}

	// A supply of notes, one after another.
	prRealizeStream { |source, unit, count, limit, instrument|
		var stream = PT.asStream(source);
		var events = Array.new(if(count == inf or: { count.isNil }) { 256 } { count });
		var elapsed = 0, index = 0;
		var top = count ?? { inf };
		while {
			(index < top) and: { limit.isNil or: { elapsed < (limit - 1e-9) } }
		} {
			var note = stream.next;
			var event;
			if(note.isNil) { ^events };
			note = PT.asNote(note);
			event = note.asEvent(unit, instrument);
			events = events.add(event);
			elapsed = elapsed + event[\dur];
			index = index + 1;
		};
		^events
	}
}
