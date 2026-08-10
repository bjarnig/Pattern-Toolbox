// PTPianoRoll - piano roll display for anything that answers asEvents.
//
// Pitch runs upwards, time runs to the right, chords appear as stacked bars,
// rests as thin dim marks. The layout is computed by a pure class method so it
// can be tested without a display.

PTPianoRoll {
	var <object, <window, <userView, <infoView;
	var <events, <layout;

	classvar <>defaultBounds;

	*initClass { defaultBounds = Rect(200, 120, 820, 420) }

	*new { |object, bounds|
		^super.new.init(object, bounds)
	}

	// ------------------------------------------------------------------ layout

	// Returns (rects: [[x, y, w, h, isRest, velocity] ...], loNote:, hiNote:,
	// duration:). Pure: no GUI involved, so it is testable.
	*computeLayout { |events, width = 800, height = 400, margin = 24|
		var notes, loNote, hiNote, span, total, time = 0, rects, rowHeight;
		var plotWidth = width - (margin * 2);
		var plotHeight = height - (margin * 2);

		if(events.isNil or: { events.isEmpty }) {
			^(rects: [], loNote: 0, hiNote: 0, duration: 0, margin: margin)
		};

		notes = events.collect { |event| event[\midinote] }.flat.select(_.isNumber);
		if(notes.isEmpty) { notes = [60] };
		loNote = notes.minItem.floor;
		hiNote = notes.maxItem.ceil;
		// always show at least an octave, so a monotone section is not a flat wall
		span = max(hiNote - loNote, 12);
		hiNote = loNote + span;

		total = events.sum { |event| event[\dur] ? 0 };
		if(total <= 0) { total = 1 };

		rowHeight = max(plotHeight / (span + 1), 2);

		rects = Array.new(events.size);
		events.do { |event|
			// dur advances the clock, sustain is how long the note sounds. In a
			// combination they differ: coincident voices have a dur of zero.
			var duration = event[\dur] ? 0;
			var length = event[\sustain] ? duration;
			var isRest = event[\type] == \rest;
			var pitches = (event[\midinote] ? loNote).asArray;
			var x = margin + (time / total * plotWidth);
			var w = max(length / total * plotWidth, 1);
			if(isRest) {
				rects = rects.add([
					x, margin + plotHeight - (rowHeight * 0.5),
					max(duration / total * plotWidth, 1), 2, true, 0
				]);
			} {
				pitches.do { |pitch|
					var y = margin + plotHeight - ((pitch - loNote + 1) / (span + 1) * plotHeight);
					rects = rects.add([x, y, w, rowHeight * 0.85, false, event[\velocity] ? 64]);
				};
			};
			time = time + duration;
		};

		^(rects: rects, loNote: loNote, hiNote: hiNote, duration: total,
			margin: margin, rowHeight: rowHeight)
	}

	// --------------------------------------------------------------------- gui

	init { |argObject, bounds|
		object = argObject;
		events = object.asEvents;
		window = PTGUI.window("piano roll: %".format(object.name), bounds ?? { defaultBounds });

		userView = UserView()
			.background_(PTGUI.color(\field))
			.drawFunc_({ |view| this.draw(view) })
			.resize_(5);

		infoView = StaticText()
			.string_(this.infoString)
			.font_(PTGUI.mono(11))
			.stringColor_(PTGUI.color(\dim));

		// stretch 1 on the drawing area, so the plot grows and the caption does not
		window.layout = VLayout([userView, stretch: 1], infoView).margins_(6).spacing_(4);
		window.front;
		^this
	}

	infoString {
		var info = PTPianoRoll.computeLayout(events, 100, 100);
		^"% events   % notes   % s   pitch % to %".format(
			events.size,
			events.count { |e| e[\type] != \rest },
			info[\duration].round(0.01),
			info[\loNote], info[\hiNote]
		)
	}

	refresh {
		events = object.asEvents;
		{ infoView.string_(this.infoString); userView.refresh }.defer;
		^this
	}

	draw { |view|
		var bounds = view.bounds;
		var info = PTPianoRoll.computeLayout(events, bounds.width, bounds.height);
		var margin = info[\margin];
		var loNote = info[\loNote], hiNote = info[\hiNote];
		var plotHeight = bounds.height - (margin * 2);

		// octave lines
		(loNote.ceil.asInteger .. hiNote.asInteger).do { |pitch|
			if(pitch % 12 == 0) {
				var y = margin + plotHeight
					- ((pitch - loNote + 1) / (hiNote - loNote + 1) * plotHeight);
				Pen.strokeColor = PTGUI.color(\gridStrong);
				Pen.line(Point(margin, y), Point(bounds.width - margin, y));
				Pen.stroke;
				Pen.stringAtPoint(
					"c%".format((pitch / 12).asInteger - 1 + (PT.middleCOctave - 4)),
					Point(2, y - 7),
					PTGUI.mono(9), PTGUI.color(\dim)
				);
			};
		};

		info[\rects].do { |rect|
			var x = rect[0], y = rect[1], w = rect[2], h = rect[3];
			var isRest = rect[4], velocity = rect[5];
			if(isRest) {
				Pen.fillColor = PTGUI.color(\rest);
			} {
				Pen.fillColor = PTGUI.color(\accent).alpha_((velocity / 127).clip(0.2, 1.0));
			};
			Pen.fillRect(Rect(x, y, w, h));
		};
	}

	close { window.close }
}
