// Pattern Toolbox
// PT - the object registry, the evaluation environment, and the facade.
//
// Every Pattern Toolbox object has a name and lives in one flat registry.
// Specifications are text; PT.eval compiles that text inside PT.envir, where
// every registered object is reachable as ~name.

PT {
	classvar <registry;          // Symbol -> PTObject
	classvar <order;             // Array of Symbol, creation order (registry has none)
	classvar <envir;             // Environment that spec text is evaluated in
	classvar clock;              // TempoClock at 1 bps: event dur is in seconds

	classvar <>sugar = true;     // rewrite bare object names, pitch names, dynamics
	classvar <>middleCOctave = 4;// c4 == 60, as in the AC Toolbox default
	classvar <>verbose = true;

	classvar <currentNumber;     // dynamic binding read by PT.fromNumber
	classvar dynamics, letters;

	*initClass {
		registry = IdentityDictionary.new;
		order = Array.new;
		envir = Environment.new;
		dynamics = IdentityDictionary[
			\ppp -> 16, \pp -> 32, \p -> 48, \mp -> 56,
			\mf -> 64, \f -> 80, \ff -> 96, \fff -> 112
		];
		letters = IdentityDictionary[
			\c -> 0, \d -> 2, \e -> 4, \f -> 5, \g -> 7, \a -> 9, \b -> 11
		];
	}

	// ---------------------------------------------------------------- registry

	*clock { ^clock ?? { clock = TempoClock(1) } }

	*new { |name| ^this.at(name) }

	*at { |name| ^registry[name.asSymbol] }

	*includes { |name| ^registry.includesKey(name.asSymbol) }

	*register { |object|
		var key = object.name;
		if(registry.includesKey(key).not) { order = order.add(key) };
		registry[key] = object;
		this.refresh(object);
		this.changed(\objects);
		^object
	}

	*refresh { |object|
		// push the object's current binding value into the spec environment
		envir[object.name] = object.asPTValue;
	}

	*remove { |name|
		name = name.asSymbol;
		registry.removeAt(name);
		envir.removeAt(name);
		order = order.reject { |n| n == name };
		this.changed(\objects);
	}

	*removeAll {
		registry.clear; envir.clear; order = Array.new;
		this.changed(\objects);
	}

	*names { |type|
		^order.select { |n|
			type.isNil or: { registry[n].class.ptType == type.asSymbol }
		}
	}

	*all { |type| ^this.names(type).collect { |n| registry[n] } }

	*uniqueName { |base|
		var candidate, i = 0;
		base = base.asString;
		while { candidate = (base ++ ($a.ascii + i).asAscii).asSymbol;
			registry.includesKey(candidate) and: { i < 25 } } { i = i + 1 };
		^candidate
	}

	// ------------------------------------------------------------- evaluation

	*eval { |code|
		var src = this.resolve(code.asString);
		if(src.stripWhiteSpace.isEmpty) { ^nil };
		^envir.use { src.interpret }
	}

	// Evaluate as a list. A bare token run such as "60 62 64" or "c4 d4 e4" is a
	// list in the AC Toolbox sense, not an sclang expression, so wrap it first.
	*evalList { |code|
		var s = code.asString.stripWhiteSpace;
		if(s.isEmpty) { ^[] };
		if(this.prIsBareTokenList(s)) {
			s = "[" ++ s.split($ ).reject(_.isEmpty).join(", ") ++ "]";
		};
		^this.eval(s)
	}

	*prIsBareTokenList { |s|
		if(s.includes($[) or: { s.includes($() or: { s.includes($,) } }) { ^false };
		^s.split($ ).reject(_.isEmpty).size > 1
	}

	// Rewrite bare identifiers: registered object -> ~name, pitch name -> midi
	// note number, dynamic name -> velocity. Skips string literals, symbol
	// literals, method calls and keyword arguments.
	*resolve { |code|
		var out, i = 0, n, ch, start, ident, prev, next, sub;
		if(sugar.not) { ^code };
		n = code.size;
		out = String.new(n);
		while { i < n } {
			ch = code[i];
			case
			{ ch == $" } {
				out = out.add(ch); i = i + 1;
				while { (i < n) and: { code[i] != $" } } {
					if(code[i] == $\\) { out = out.add(code[i]); i = i + 1 };
					if(i < n) { out = out.add(code[i]); i = i + 1 };
				};
				if(i < n) { out = out.add(code[i]); i = i + 1 };
			}
			{ ch == $' } {
				out = out.add(ch); i = i + 1;
				while { (i < n) and: { code[i] != $' } } { out = out.add(code[i]); i = i + 1 };
				if(i < n) { out = out.add(code[i]); i = i + 1 };
			}
			{ ch == $\\ } {
				out = out.add(ch); i = i + 1;
				while { (i < n) and: { code[i].isAlphaNum or: { code[i] == $_ } } } {
					out = out.add(code[i]); i = i + 1;
				};
			}
			{ ch.isAlpha or: { ch == $_ } } {
				start = i;
				while { (i < n) and: { code[i].isAlphaNum or: { code[i] == $_ } } } { i = i + 1 };
				ident = code.copyRange(start, i - 1);
				prev = if(start > 0) { code[start - 1] } { $  };
				next = if(i < n) { code[i] } { $  };
				sub = ident;
				if((prev != $~) and: { prev != $. } and: { next != $: }) {
					sub = this.prSubstitute(ident);
				};
				out = out ++ sub;
			}
			{ out = out.add(ch); i = i + 1 };
		};
		^out
	}

	*prSubstitute { |ident|
		var midi;
		if(registry.includesKey(ident.asSymbol)) { ^"~" ++ ident };
		midi = this.prParsePitch(ident);
		if(midi.notNil) { ^midi.asString };
		// single-character p and f are far too common as variable names
		if((ident.size > 1) and: { dynamics.includesKey(ident.asSymbol) }) {
			^dynamics[ident.asSymbol].asString
		};
		^ident
	}

	// ------------------------------------------------------------- conversion

	// c4 cs4 c#4 ef4 bf-1 ... -> midi note number, or nil if not a pitch name
	*prParsePitch { |str|
		var s, base, acc = 0, i = 1, sign = 1, oct = 0, digits = 0;
		s = str.asString.toLower;
		if(s.size < 2) { ^nil };
		base = letters[s[0].asString.asSymbol];
		if(base.isNil) { ^nil };
		while { (i < s.size) and: { (s[i] == $s) or: { s[i] == $# } } } { acc = acc + 1; i = i + 1 };
		if(acc == 0) {
			while { (i < s.size) and: { (s[i] == $f) and: { i + 1 < s.size } } } { acc = acc - 1; i = i + 1 };
		};
		if((i < s.size) and: { s[i] == $- }) { sign = -1; i = i + 1 };
		while { (i < s.size) and: { s[i].isDecDigit } } {
			oct = (oct * 10) + s[i].digit; i = i + 1; digits = digits + 1;
		};
		if((digits == 0) or: { i < s.size }) { ^nil };
		^((oct * sign) + (5 - middleCOctave)) * 12 + base + acc
	}

	// A String is a SequenceableCollection, so names must be tested before lists
	// or "c4" gets collected over its characters and quietly comes back unchanged.
	*midinote { |x|
		if(x.isNumber) { ^x };
		if(x.isKindOf(Symbol) or: { x.isKindOf(String) }) {
			^this.prParsePitch(x) ?? { Error("PT: not a pitch name: %".format(x)).throw }
		};
		if(x.isKindOf(SequenceableCollection)) { ^x.collect { |i| this.midinote(i) } };
		^x
	}

	*velocity { |x|
		if(x.isNumber) { ^x };
		if(x.isKindOf(Symbol) or: { x.isKindOf(String) }) {
			^dynamics[x.asSymbol] ?? { Error("PT: not a dynamic: %".format(x)).throw }
		};
		if(x.isKindOf(SequenceableCollection)) { ^x.collect { |i| this.velocity(i) } };
		^dynamics[x.asSymbol] ?? { Error("PT: not a dynamic: %".format(x)).throw }
	}

	// Anything that can supply a series of values becomes a Stream.
	*asStream { |x|
		if(x.isNil) { ^Pseq([nil], inf).asStream };
		if(x.isKindOf(PTObject)) { ^x.asStream };
		if(x.isKindOf(Stream)) { ^x };
		if(x.isKindOf(Pattern)) { ^x.asStream };
		if(x.isKindOf(Function)) { ^Pfunc(x).asStream };
		if(x.isKindOf(SequenceableCollection)) {
			if(x.isEmpty) { ^Pseq([nil], inf).asStream };
			^Pseq(x, inf).asStream
		};
		^Pseq([x], inf).asStream
	}

	// Draw values out of anything. n is required for open-ended sources.
	*collectValues { |source, n|
		if(source.isKindOf(PTObject)) { source = source.asPTValue };
		if(source.isKindOf(SequenceableCollection) and: { source.isKindOf(Pattern).not }) {
			^if(n.isNil) { source } { Pseq(source, inf).asStream.nextN(n) }
		};
		if(n.isNil) {
			Error("PT: a number of values is required for source %".format(source.class)).throw
		};
		^this.asStream(source).nextN(n)
	}

	// ------------------------------------------------------------------ tools

	*untilTime { |seconds| ^PTUntilTime(seconds) }
	*bpm { |beats| ^60000 / beats }
	*mm { |beats| ^60000 / beats }
	*fromNumber { ^currentNumber }
	*dyn { |name| ^this.velocity(name) }

	// Interpret a shape or a mask in a range. The AC Toolbox convert and
	// convert2, in one place. The method form, shape1.convert(...), is equivalent.
	*convert { |curve, n, lo = 0, hi = 1, positions, round|
		if(curve.isKindOf(Symbol)) { curve = this.at(curve) };
		if(curve.isKindOf(PTMask)) { ^curve.convert(n, lo, hi, positions, round) };
		if(curve.isKindOf(PTShape)) { ^curve.convert(n, lo, hi, round) };
		if(curve.isKindOf(SequenceableCollection)) {
			^PTCurve.convertShape(curve, n ?? { 100 }, lo, hi, round)
		};
		Error("PT.convert: not a shape or a mask: %".format(curve.class)).throw
	}

	// Read from a collection in the order described by a shape or a mask. The
	// lowest point of the curve reads the first element, the highest the last.
	*readFrom { |collection, curve, n, positions|
		var values, indices;
		if(collection.isKindOf(Symbol)) { collection = this.at(collection) };
		if(curve.isKindOf(Symbol)) { curve = this.at(curve) };
		if(collection.isKindOf(PTObject)) { collection = collection.asPTValue };
		values = collection.asArray;
		n = n ?? { this.fromNumber ? 100 };
		indices = case
			{ curve.isKindOf(PTMask) } { curve.indices(values.size, n, positions) }
			{ curve.isKindOf(PTShape) } { curve.indices(values.size, n) }
			{ PTCurve.readIndices(curve, values.size, n, positions) };
		^indices.collect { |i| values[i] }
	}

	// One value, where a slot needs a single number rather than a series. A
	// controller advances by one; a pattern or a list gives up its first value.
	*scalar { |x|
		if(x.isKindOf(PTController)) { ^x.next };
		if(x.isKindOf(Stream) or: { x.isKindOf(Pattern) }) { ^this.asStream(x).next };
		if(x.isKindOf(SequenceableCollection) and: { x.isString.not }) { ^x.first };
		^x
	}

	// The AC Toolbox take-one: pull one value and hand that single value on, so a
	// generator built from it uses the same value throughout the section.
	*takeOne { |x|
		if(x.isKindOf(Symbol)) { x = this.at(x) };
		if(x.isKindOf(PTController)) { ^x.takeOne };
		^this.scalar(x)
	}

	// ------------------------------------------------------------------- notes

	*note { |rhythm = 1, pitch = 60, velocity = 64, channel = 1|
		^PTNote(rhythm, pitch, velocity, channel, \note)
	}

	*rest { |rhythm = 1| ^PTNote(rhythm, 60, 0, 1, \rest) }

	// Time with nothing in it at all, not even a rest event.
	*delay { |rhythm = 1| ^PTNote(rhythm, 60, 0, 1, \delay) }

	*seq { |... items| ^PTNoteSeq(items) }

	*par { |... items| ^PTNotePar(items) }

	// the AC Toolbox spellings, for people arriving from it
	*aNote { |rhythm = 1, pitch = 60, velocity = 64, channel = 1|
		^this.note(rhythm, pitch, velocity, channel)
	}
	*aRest { |rhythm = 1| ^this.rest(rhythm) }
	*aDelay { |rhythm = 1| ^this.delay(rhythm) }
	*inSequence { |... items| ^PTNoteSeq(items) }
	*inParallel { |... items| ^PTNotePar(items) }

	// A melody as alternating rhythm and pitch: "1 c4 1 d4 2 e4 1 r".
	// r or rest is a rest, . is a delay, and c4+e4+g4 is a chord.
	*melody { |string, velocity = 64, channel = 1|
		var tokens = string.asString.split($ ).reject { |t| t.stripWhiteSpace.isEmpty };
		var items = Array.new(tokens.size div: 2);
		var i = 0;
		while { i + 1 < tokens.size } {
			var rhythm = tokens[i].asFloat;
			var token = tokens[i + 1].toLower;
			items = items.add(
				case
					{ (token == "r") or: { token == "rest" } } { this.rest(rhythm) }
					{ token == "." } { this.delay(rhythm) }
					{ token.includes($+) } {
						this.note(rhythm,
							token.split($+).collect { |p| this.midinote(p) },
							velocity, channel)
					}
					{ this.note(rhythm, this.midinote(token), velocity, channel) }
			);
			i = i + 2;
		};
		^PTNoteSeq(items)
	}

	// Anything at all, read as a note tree.
	*asNoteTree { |x|
		if(x.isKindOf(Symbol)) { x = this.at(x) ?? { x } };
		if(x.isKindOf(PTNoteStructure)) { ^x.tree };
		if(x.isKindOf(PTNoteTree)) { ^x };
		if(x.isKindOf(PTSection)) { ^PTNoteSeq(x.asNotes) };
		if(x.isKindOf(String)) { ^this.melody(x) };
		if(x.isKindOf(SequenceableCollection)) { ^PTNoteSeq(x) };
		^PTNoteSeq([this.asNote(x)])
	}

	*asNote { |x|
		if(x.isKindOf(PTNote)) { ^x };
		if(x.isKindOf(Event)) {
			^PTNote(x[\rhythm] ? 1, x[\midinote] ? 60, x[\velocity] ? 64, x[\chan] ? 1,
				if(x[\type] == \rest) { \rest } { \note })
		};
		if(x.isNumber or: { x.isKindOf(Symbol) }) { ^this.note(1, x) };
		if(x.isKindOf(SequenceableCollection)) { ^this.note(1, x) };
		Error("PT: cannot read % as a note".format(x.class)).throw
	}

	// A supply of notes out of whatever was given: a structure, a section, a list.
	*asList { |x|
		if(x.isKindOf(Symbol)) { x = this.at(x) ?? { x } };
		if(x.isKindOf(PTNoteStructure)) { ^x.notes };
		if(x.isKindOf(PTNoteTree)) { ^x.notes };
		if(x.isKindOf(PTSection)) { ^x.asNotes };
		if(x.isKindOf(PTObject)) { x = x.asPTValue };
		if(x.isKindOf(PTNoteStructure)) { ^x.notes };
		^x.asArray
	}

	// ------------------------------------------------------------------- density

	// A list of attack points made by adding one interval after another, then
	// mapped into low..high. The Xenakis approach: the function decides the
	// distance to the next attack rather than the absolute position.
	*attacks { |interval = 1, low = 0.0, high = 100, number|
		var stream = this.asStream(interval);
		var count = (number ?? { this.fromNumber ? 20 }).asInteger;
		var running = 0;
		var raw = Array.new(count);
		count.do {
			raw = raw.add(running);
			running = running + (stream.next ? 1);
		};
		if(count < 2) { ^raw };
		^PTCurve.scale(raw, low, high)
	}

	// Rainer Boesch's interpolation: step by step, take an element either from the
	// first object or from the second, with the second becoming steadily more
	// likely. A shape can replace the straight line.
	*interpolate { |object1, object2, number = 40, shape|
		var one = this.asList(object1), two = this.asList(object2);
		var probabilities = if(shape.notNil) {
			this.convert(shape, number, 0.0, 1.0)
		} {
			number.collect { |i| i / max(number - 1, 1) }
		};
		if(one.isEmpty) { ^two.keep(number) };
		if(two.isEmpty) { ^one.keep(number) };
		^number.collect { |i|
			if(1.0.rand < (probabilities[i] ? 0)) { two.wrapAt(i) } { one.wrapAt(i) }
		}
	}

	// Map values through a lookup table given as pairs: key, value, key, value.
	// The AC Toolbox way of relating one parameter to another, usually with two
	// controllers synchronised to a third so both see the same incoming value.
	*lookup { |source, table, default|
		var stream = this.asStream(source);
		var pairs = Dictionary.new;
		if(table.isKindOf(PTObject)) { table = table.asPTValue };
		table.asArray.pairsDo { |key, item| pairs[key] = item };
		^Pfunc({
			var incoming = stream.next;
			pairs[incoming] ?? { default ?? incoming }
		})
	}

	// ----------------------------------------------------------- transformers

	// Thin forwarders onto PTTransform. PT. reads as a namespace and keeps slot
	// text short: "PT.transpose(12)" rather than "PTTransform.transpose(12)".

	*add { |key, amount| ^PTTransform.add(key, amount) }
	*multiply { |key, factor| ^PTTransform.multiply(key, factor) }
	*set { |key, source| ^PTTransform.set(key, source) }
	*transpose { |interval| ^PTTransform.transpose(interval) }
	*louder { |amount| ^PTTransform.louder(amount) }
	*limit { |lo, hi, key = \midinote| ^PTTransform.limit(lo, hi, key) }
	*fold { |lo, hi, key = \midinote| ^PTTransform.fold(lo, hi, key) }
	*quantize { |unit, key = \midinote| ^PTTransform.quantize(unit, key) }
	*stretch { |factor| ^PTTransform.stretch(factor) }
	*filter { |test| ^PTTransform.filter(test) }
	*reject { |test| ^PTTransform.reject(test) }
	*mute { |test| ^PTTransform.mute(test) }
	*dedupe { |key = \midinote| ^PTTransform.dedupe(key) }
	*reverse { ^PTTransform.reverse }
	*slice { |from = 0, to| ^PTTransform.slice(from, to) }
	*keep { |n| ^PTTransform.keep(n) }
	*drop { |n| ^PTTransform.drop(n) }
	*transformIf { |test, transform| ^PTTransform.transformIf(test, transform) }

	*prSetCurrentNumber { |n| currentNumber = n }

	*newSeed { ^1000000.rand }

	// --------------------------------------------------------------- lifecycle

	*def { |name, class, slots, seed, comment|
		var object = class.new(name, slots);
		object.comment_(comment);
		if(seed.notNil) { object.make(seed) };
		^object
	}

	*make { |name| ^this.at(name).make }
	*play { |name| ^this.at(name).play }
	*stop { |name| ^this.at(name).stop }
	*stopAll { Pdef.all.do(_.stop) }

	*browse { ^PTBrowser.new }
	*edit { |name| ^this.at(name).edit }

	*save { |path, names| ^PTArchive.save(path, names) }
	*load { |path| ^PTArchive.load(path) }

	*post {
		"Pattern Toolbox: % object(s)".format(order.size).postln;
		order.do { |n| registry[n].postInfo };
		^this
	}
}

// A marker for "keep going until this many seconds have been filled",
// the AC Toolbox until-time.
PTUntilTime {
	var <seconds;
	*new { |seconds| ^super.newCopyArgs(seconds) }
	printOn { |stream| stream << "PTUntilTime(" << seconds << ")" }
}
