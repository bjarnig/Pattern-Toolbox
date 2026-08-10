// PTDerived - a section made by transforming another section.
//
// The AC Toolbox applies a transformer to an object in place, which quietly
// breaks its own invariant: afterwards the object's rules no longer describe its
// result. Here a transformation is itself an object whose rules name a source
// and a list of transforms. Remake it and the transformation runs again; remake
// the source first and the whole chain follows.
//
//   PTDerived(\higher, (source: "opening", transform: "PT.transpose(12)")).make;
//   PTDerived(\tail, (
//       source:    "opening",
//       transform: "[PT.slice(30), PT.stretch(2), PT.limit(c2, c4)]"
//   )).make;

PTDerived : PTSection {

	*ptType { ^\section }

	*slotSpecs {
		^[
			[\source, "", "the section to transform, or several joined in sequence"],
			[\transform, "", "one transform, or a list of them applied left to right"]
		]
	}

	*of { |name, source, transform|
		^this.new(name, (source: source.asString, transform: transform)).make
	}

	realize {
		var source = spec.valueAsList(\source);
		var members, events;

		if(source.isNil) { ^[] };
		members = case
			{ source.isKindOf(PTObject) } { [source] }
			{ source.isKindOf(SequenceableCollection) } { source.asArray.reject(_.isNumber) }
			{ [source] };

		members.do { |member|
			if(member.isKindOf(PTSection).not) {
				Error("PT: % is not a section".format(member)).throw
			};
		};

		// several sources are joined in sequence first, then transformed
		events = if(members.size == 1) {
			members[0].asEvents.collect { |event|
				var out = event.copy;
				out[\sustain] = out[\sustain] ?? { out[\dur] };
				out
			}
		} {
			var timed = Array.new, cursor = 0;
			members.do { |member|
				timed = timed ++ member.asTimedEvents(cursor);
				cursor = cursor + member.duration;
			};
			PTCombination.flatten(timed)
		};

		^PTTransform.apply(events, spec.value(\transform))
	}
}
