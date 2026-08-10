// PTTransform - a function from an event list to an event list.
//
// The AC Toolbox transformers, filters and order-changing tools, unified. There
// is one class rather than a hierarchy, because every one of them is a small
// function over an array of events; the named constructors are what a user sees.
//
// Reach for them through the PT facade, which is shorter to type in a slot:
//
//   PT.transpose(12)
//   PT.transpose(Pwhite(-3, 3))        a generator: a different interval per note
//   [PT.stretch(2), PT.limit(c3, c6)]  applied left to right
//
// A transform never mutates its input. The AC Toolbox applies transformers in
// place, which breaks the link between an object's rules and its result; here a
// transformed section is a PTDerived object whose rules say what was done.

PTTransform {
	var <label, <function;

	*new { |label, function| ^super.newCopyArgs(label.asSymbol, function) }

	value { |events| ^function.value(events) }

	printOn { |stream| stream << "PTTransform(" << label << ")" }

	// Apply one transform, or a list of them, left to right.
	*apply { |events, transforms|
		if(transforms.isNil) { ^events };
		transforms.asArray.do { |transform|
			if(transform.isNil.not) { events = transform.value(events) };
		};
		^events
	}

	// ------------------------------------------------------------- the engine

	// Map one key of every sounding event. func receives the old value and the
	// whole event, so a transform can relate one parameter to another.
	*mapKey { |label, key, func, includeRests = false|
		^this.new(label, { |events|
			events.collect { |event|
				var out = event.copy;
				if(includeRests or: { event[\type] != \rest }) {
					out[key] = func.value(out[key], out);
				};
				out
			}
		})
	}

	// ----------------------------------------------------------------- values

	// Add to a parameter. The amount may be a constant, a list or a generator,
	// in which case a different amount is used for each note.
	*add { |key, amount|
		var stream = PT.asStream(amount);
		^this.mapKey(\add, key.asSymbol, { |value| value + (stream.next ? 0) })
	}

	*multiply { |key, factor|
		var stream = PT.asStream(factor);
		^this.mapKey(\multiply, key.asSymbol, { |value| value * (stream.next ? 1) })
	}

	// Replace a parameter outright, from any source.
	*set { |key, source|
		var stream = PT.asStream(source);
		^this.mapKey(\set, key.asSymbol, { |value| stream.next ? value })
	}

	*transpose { |interval| ^this.add(\midinote, interval).label_(\transpose) }

	*louder { |amount| ^this.add(\velocity, amount).label_(\louder) }

	*limit { |lo, hi, key = \midinote|
		^this.mapKey(\limit, key, { |value| value.clip(lo, hi) })
	}

	// Fold back into the range instead of clipping, so the contour survives.
	*fold { |lo, hi, key = \midinote|
		^this.mapKey(\fold, key, { |value| value.fold(lo, hi) })
	}

	*quantize { |unit, key = \midinote|
		^this.mapKey(\quantize, key, { |value| value.round(unit) })
	}

	// ------------------------------------------------------------------- time

	// Scale every duration. Rests stretch too, so the shape of the section holds.
	*stretch { |factor|
		var stream = PT.asStream(factor);
		^this.new(\stretch, { |events|
			events.collect { |event|
				var out = event.copy;
				var amount = stream.next ? 1;
				out[\dur] = (out[\dur] ? 0) * amount;
				if(out[\sustain].notNil) { out[\sustain] = out[\sustain] * amount };
				out
			}
		})
	}

	// -------------------------------------------------------------- structure

	// Keep the events the test accepts and close the gaps, so the section gets
	// shorter. Use mute instead to keep the timing.
	*filter { |test|
		^this.new(\filter, { |events| events.select { |event| test.value(event) } })
	}

	*reject { |test|
		^this.new(\reject, { |events| events.reject { |event| test.value(event) } })
	}

	// Silence the events the test accepts, leaving the timing untouched.
	*mute { |test|
		^this.new(\mute, { |events|
			events.collect { |event|
				var out = event.copy;
				if(test.value(event)) { out[\type] = \rest };
				out
			}
		})
	}

	// Drop immediate repetitions of a parameter. The AC Toolbox filtering of
	// repetitions.
	*dedupe { |key = \midinote|
		^this.new(\dedupe, { |events|
			var last;
			events.select { |event|
				var value = event[key];
				var keep = (event[\type] == \rest) or: { value != last };
				if(event[\type] != \rest) { last = value };
				keep
			}
		})
	}

	*reverse { ^this.new(\reverse, { |events| events.reverse }) }

	// Event indices, inclusive of from and exclusive of to.
	*slice { |from = 0, to|
		^this.new(\slice, { |events|
			events.copyRange(from, (to ?? { events.size }) - 1) ? []
		})
	}

	*keep { |n| ^this.new(\keep, { |events| events.keep(n) }) }

	*drop { |n| ^this.new(\drop, { |events| events.drop(n) }) }

	// ------------------------------------------------------------ conditional

	// Apply a transform to only the events the test accepts. The untouched
	// events keep their place, so timing is preserved.
	*transformIf { |test, transform|
		^this.new(\transformIf, { |events|
			var chosen = events.select { |event| test.value(event) };
			var changed = PTTransform.apply(chosen, transform);
			var index = -1;
			events.collect { |event|
				if(test.value(event)) { index = index + 1; changed[index] ? event } { event }
			}
		})
	}

	label_ { |newLabel| label = newLabel.asSymbol; ^this }
}
