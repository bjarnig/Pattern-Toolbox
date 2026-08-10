// Phase 0 test suite. Run with: TestPatternToolbox.run

TestPatternToolbox : UnitTest {

	setUp { PT.removeAll }
	tearDown { PT.removeAll }

	// ------------------------------------------------------------ conversions

	test_pitchNames {
		this.assertEquals(PT.note(\c4), 60, "c4 is middle C");
		this.assertEquals(PT.note(\c5), 72, "c5");
		this.assertEquals(PT.note(\cs4), 61, "cs4 is a semitone above c4");
		this.assertEquals(PT.note('c#4'), 61, "c#4 uses the AC Toolbox spelling");
		this.assertEquals(PT.note(\ef4), 63, "ef4 is e flat");
		this.assertEquals(PT.note(\f4), 65, "f4 is a note name, not a flat");
		this.assertEquals(PT.note('c-1'), 0, "c-1 is the lowest pitch");
		this.assertEquals(PT.note(\g9), 127, "g9 is the highest pitch");
		this.assertEquals(PT.note(60), 60, "numbers pass through");
		this.assertEquals(PT.note([\c4, \e4, \g4]), [60, 64, 67], "chords convert elementwise");
	}

	test_dynamics {
		this.assertEquals(PT.dyn(\mf), 64, "mf is 64, as in the AC Toolbox");
		this.assertEquals(PT.dyn(90), 90, "numbers pass through");
	}

	// -------------------------------------------------------------- name sugar

	test_resolveLeavesCodeAlone {
		this.assertEquals(PT.resolve("Pwhite(0, 10)"), "Pwhite(0, 10)", "class names untouched");
		this.assertEquals(PT.resolve("\\default"), "\\default", "symbol literals untouched");
		this.assertEquals(PT.resolve("\"c4 is a pitch\""), "\"c4 is a pitch\"",
			"string literals untouched");
		this.assertEquals(PT.resolve("Pwhite(0, 10, round: 1)"), "Pwhite(0, 10, round: 1)",
			"keyword arguments untouched");
	}

	test_resolvePitchNames {
		this.assertEquals(PT.resolve("c4"), "60", "a bare pitch name becomes a number");
		this.assertEquals(PT.resolve("Pwhite(c2, c5)"), "Pwhite(36, 72)", "inside a call too");
		this.assertEquals(PT.resolve("array.c4"), "array.c4", "method calls untouched");
	}

	test_resolveObjectNames {
		PTStockpile.specify(\cmajor, "c4 d4 e4");
		this.assertEquals(PT.resolve("Prand(cmajor, inf)"), "Prand(~cmajor, inf)",
			"a registered name becomes an environment variable");
		this.assertEquals(PT.resolve("Prand(~cmajor, inf)"), "Prand(~cmajor, inf)",
			"an explicit ~name is left alone");
	}

	test_sugarCanBeDisabled {
		PT.sugar = false;
		this.assertEquals(PT.resolve("c4"), "c4", "no rewriting when sugar is off");
		PT.sugar = true;
	}

	// -------------------------------------------------------------- stockpiles

	test_stockpileSpecified {
		var s = PTStockpile.specify(\cmajor, "c4 d4 e4 f4 g4 a4 b4");
		this.assertEquals(s.value, [60, 62, 64, 65, 67, 69, 71], "bare pitch names become a list");
		this.assertEquals(s.size, 7, "size");
	}

	test_stockpileFromExpression {
		var s = PTStockpile.specify(\chrom, "(60..71)");
		this.assertEquals(s.value, (60..71), "an expression producing a list");
	}

	test_stockpileGenerated {
		var s = PTStockpile.generate(\pitches, "Pwhite(60, 72)", "12");
		this.assertEquals(s.size, 12, "a pattern drawn the requested number of times");
		this.assert(s.value.every { |v| v.inclusivelyBetween(60, 72) }, "values within range");
	}

	test_stockpileBindsToItsValues {
		PTStockpile.specify(\cmajor, "60 62 64");
		this.assertEquals(PT.eval("cmajor"), [60, 62, 64],
			"~name inside another spec yields the list, not the object");
	}

	// ----------------------------------------------------------- data sections

	test_dataSectionBasics {
		var s = PTDataSection(\s1, (clock: "100", number: "10", rhythm: "1",
			pitch: "60", velocity: "\\mf", channel: "1")).make;
		this.assertEquals(s.length, 10, "ten events");
		this.assertEquals(s.asEvents[0][\dur], 0.1, "rhythm 1 at clock 100 is 0.1 seconds");
		this.assertEquals(s.asEvents[0][\midinote], 60, "pitch");
		this.assertEquals(s.asEvents[0][\velocity], 64, "mf");
		this.assertFloatEquals(s.duration, 1.0, "total duration");
	}

	test_dataSectionCyclesLists {
		var s = PTDataSection(\s1, (number: "6", pitch: "c4 d4 e4")).make;
		this.assertEquals(s.extract(\pitch), [60, 62, 64, 60, 62, 64], "lists cycle");
	}

	test_dataSectionRests {
		var s = PTDataSection(\s1, (number: "4", rhythm: "1 -1 2 -3")).make;
		this.assertEquals(s.length, 4, "rests are kept as events");
		this.assertEquals(s.numNotes, 2, "but do not count as notes");
		this.assertArrayFloatEquals(s.extract(\rhythm), [0.1, 0.1, 0.2, 0.3],
			"rest length is the absolute value");
		this.assertEquals(s.asEvents[1][\type], \rest, "second event is a rest");
	}

	test_dataSectionChords {
		var s = PTDataSection(\s1, (number: "2", pitch: "[[c4, e4, g4]]")).make;
		this.assertEquals(s.asEvents[0][\midinote], [60, 64, 67],
			"a nested array is a chord; a flat one is a sequence of pitches");
	}

	test_dataSectionUntilTime {
		var s = PTDataSection(\s1, (clock: "100", number: "PT.untilTime(1)", rhythm: "1")).make;
		this.assertEquals(s.length, 10, "fills one second with 0.1 second notes");
	}

	test_dataSectionUsesStockpile {
		PTStockpile.specify(\cmajor, "c4 d4 e4 f4 g4 a4 b4");
		PTDataSection(\s1, (number: "40", pitch: "Prand(cmajor, inf)")).make;
		this.assert(PT(\s1).extract(\pitch).every { |p| [60,62,64,65,67,69,71].includes(p) },
			"pitches all come from the stockpile");
	}

	test_bpm {
		this.assertEquals(PT.bpm(120), 500, "120 bpm is a 500 ms beat");
	}

	// ------------------------------------------------------ make, seed, variant

	test_seedReproduces {
		var s = PTDataSection(\s1, (number: "20", pitch: "Pwhite(40, 80)")).make;
		var first = s.extract(\pitch);
		s.make;
		this.assert(s.extract(\pitch) != first, "make gives a new result");
		s.reproduce;
		this.assertEquals(s.extract(\pitch), s.extract(\pitch), "reproduce is stable");
		this.assertEquals(PTDataSection(\s2, s.spec.asEvent).make(s.seed).extract(\pitch),
			s.extract(\pitch), "the same spec and seed give the same result");
	}

	test_variant {
		var s = PTDataSection(\noise1, (number: "20", pitch: "Pwhite(40, 80)")).make;
		var v = s.variant;
		this.assertEquals(v.name, \noise1a, "variants get a suffixed name");
		this.assertEquals(v.spec.at(\pitch), s.spec.at(\pitch), "same rules");
		this.assert(v.seed != s.seed, "different seed");
		this.assert(v.extract(\pitch) != s.extract(\pitch), "different result");
	}

	test_specifyDoesNotMake {
		var s = PTDataSection(\s1, (number: "5"));
		this.assert(s.isMade.not, "a new object is not made");
		s.make;
		this.assert(s.isMade, "make realizes it");
		s.specify;
		this.assert(s.isMade.not, "specify discards the realization");
	}

	// ---------------------------------------------------------------- registry

	test_registry {
		PTStockpile.specify(\a1, "1 2 3");
		PTDataSection(\b1, (number: "2"));
		this.assertEquals(PT.names, [\a1, \b1], "creation order is kept");
		this.assertEquals(PT.names(\stockpile), [\a1], "filtered by type");
		this.assert(PT(\a1).notNil, "lookup by name");
		PT.remove(\a1);
		this.assert(PT(\a1).isNil, "removal");
	}

	// --------------------------------------------------------- gui, pure parts

	test_browserFilter {
		var all = [\section1, \section2, \cmajor, \Noise1];
		this.assertEquals(PTBrowser.filterNames(all, ""), all, "an empty filter shows everything");
		this.assertEquals(PTBrowser.filterNames(all, "sec"), [\section1, \section2], "substring");
		this.assertEquals(PTBrowser.filterNames(all, "NOISE"), [\Noise1], "case insensitive");
		this.assertEquals(PTBrowser.filterNames(all, "zz"), [], "no match");
	}

	test_parenBalance {
		this.assertEquals(PTObjectView.parenBalance("Prand([1, 2], inf)"), 0, "balanced");
		this.assertEquals(PTObjectView.parenBalance("Prand([1, 2], inf"), 1, "one unclosed");
		this.assertEquals(PTObjectView.parenBalance("Pseq([1])))"), -2, "two too many");
		this.assertEquals(PTObjectView.parenBalance("\"a ( string\""), 0,
			"parentheses inside a string do not count");
	}

	test_pianoRollLayout {
		var section = PTDataSection(\s1, (clock: "100", number: "4",
			rhythm: "1 -1 2 1", pitch: "c4 d4 e4 f4")).make;
		var layout = PTPianoRoll.computeLayout(section.asEvents, 800, 400);
		this.assertFloatEquals(layout[\duration], 0.5, "total duration");
		this.assertEquals(layout[\rects].size, 4, "one rectangle per event");
		this.assertEquals(layout[\rects][1][4], true, "the second event is drawn as a rest");
		this.assertEquals(layout[\loNote], 60, "lowest pitch");
		this.assert(layout[\hiNote] - layout[\loNote] >= 12, "at least an octave is shown");
		this.assert(layout[\rects][0][0] < layout[\rects][2][0], "time runs to the right");
		this.assert(layout[\rects][0][1] > layout[\rects][3][1], "pitch runs upwards");
	}

	test_pianoRollLayoutHandlesChordsAndEmpty {
		var events = [(dur: 1, midinote: [60, 64, 67], velocity: 64)];
		var layout = PTPianoRoll.computeLayout(events, 800, 400);
		this.assertEquals(layout[\rects].size, 3, "a chord draws one rectangle per pitch");
		this.assertEquals(PTPianoRoll.computeLayout([], 800, 400)[\rects], [],
			"an empty section does not blow up");
	}

	// ----------------------------------------------------------------- archive

	test_archiveRoundTrip {
		var path = PathName.tmp +/+ "pt-test-archive.ptx";
		var before;
		PTStockpile.specify(\cmajor, "c4 d4 e4 f4 g4 a4 b4");
		PTDataSection(\s1, (number: "20", pitch: "Prand(cmajor, inf)")).make;
		before = PT(\s1).extract(\pitch);
		PTArchive.save(path);

		PT.removeAll;
		this.assert(PT(\s1).isNil, "environment cleared");

		PTArchive.load(path);
		this.assertEquals(PT.names, [\cmajor, \s1], "both objects returned, in order");
		this.assertEquals(PT(\cmajor).value, [60, 62, 64, 65, 67, 69, 71], "stockpile restored");
		this.assertEquals(PT(\s1).extract(\pitch), before,
			"the section is byte-identical, because the seed was saved");
		File.delete(path);
	}
}
