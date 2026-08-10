// Combinations - sections joined in sequence, in parallel, or at given times.
//
//   PTSequence  section1 3 section2      the number is a delay between them
//   PTParallel  section1 .1 section2     the number is an offset from the start
//   PTTimed     sections + start times
//
// All three flatten to the same thing: one Array of Events in absolute order,
// where \dur is the time to the next event and \sustain is how long the note
// sounds. That separation is what lets voices overlap.

PTCombination : PTSection {

	*ptType { ^\section }

	// The sections slot is a bare token run: names, optionally with numbers
	// between them. It evaluates to a mixed array of objects and numbers.
	prMembers { |key = \sections|
		var value = spec.valueAsList(key);
		if(value.isNil) { ^[] };
		if(value.isKindOf(PTObject)) { ^[value] };
		if(value.isNumber) { ^[value] };
		^value.asArray
	}

	// Turn a list of [startTime, event] pairs into a flat event array. A gap
	// becomes a long \dur on the preceding event rather than a rest, so nothing
	// has to be invented; only a leading offset needs a real rest.
	*flatten { |timed|
		var sorted, events, first;
		if(timed.isNil or: { timed.isEmpty }) { ^[] };
		sorted = timed.sort { |a, b| a[0] <= b[0] };
		events = Array.new(sorted.size + 1);
		first = sorted[0][0];
		if(first > 1e-9) {
			events = events.add((dur: first, sustain: first, type: \rest));
		};
		sorted.do { |pair, i|
			var event = pair[1];
			var next = if(i + 1 < sorted.size) { sorted[i + 1][0] } { nil };
			event[\dur] = if(next.notNil) { max(next - pair[0], 0) } { event[\sustain] ? 0 };
			events = events.add(event);
		};
		^events
	}

	prRequireSection { |member|
		if(member.isKindOf(PTSection).not) {
			Error("PT: % is not a section".format(member)).throw
		};
		^member
	}
}


// One after another. A number between two names delays the next one by that
// many seconds.
PTSequence : PTCombination {

	*slotSpecs {
		^[[\sections, "", "section names, optionally with a delay in seconds between them"]]
	}

	*of { |name ... sections|
		^this.new(name, (sections: sections.collect(_.asString).join(" "))).make
	}

	realize {
		var timed = Array.new;
		var cursor = 0, pending = 0;
		this.prMembers.do { |member|
			if(member.isNumber) {
				pending = pending + member;
			} {
				this.prRequireSection(member);
				cursor = cursor + pending;
				pending = 0;
				timed = timed ++ member.asTimedEvents(cursor);
				cursor = cursor + member.duration;
			};
		};
		^PTCombination.flatten(timed)
	}
}


// At the same time. A number between two names starts the next one that many
// seconds after the beginning of the whole combination.
PTParallel : PTCombination {

	*slotSpecs {
		^[[\sections, "", "section names, optionally with a start offset in seconds"]]
	}

	*of { |name ... sections|
		^this.new(name, (sections: sections.collect(_.asString).join(" "))).make
	}

	realize {
		var timed = Array.new;
		var offset = 0;
		this.prMembers.do { |member|
			if(member.isNumber) {
				offset = member;
			} {
				this.prRequireSection(member);
				timed = timed ++ member.asTimedEvents(offset);
			};
		};
		^PTCombination.flatten(timed)
	}
}


// At given times, counted from the beginning of the combination. Times may be a
// list, a generator, or anything else that supplies numbers.
PTTimed : PTCombination {

	*slotSpecs {
		^[
			[\sections, "", "section names"],
			[\times, "", "start times in seconds, from the beginning"]
		]
	}

	realize {
		var members = this.prMembers.reject(_.isNumber);
		var times = PT.asStream(spec.valueAsList(\times, 0));
		var timed = Array.new;
		members.do { |member|
			this.prRequireSection(member);
			timed = timed ++ member.asTimedEvents(times.next ? 0);
		};
		^PTCombination.flatten(timed)
	}
}
