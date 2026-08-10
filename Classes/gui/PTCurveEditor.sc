// PTCurveEditor - draw a shape or a mask with the mouse.
//
// One class for both, because a shape is a mask with one line. Drag left to
// right to draw. For a mask, choose which line you are drawing first; the
// selected line is drawn thicker, as in the original.
//
// Accepting writes the point array back into the object's spec as a literal, so
// a drawn curve is still an ordinary text specification: it saves, diffs and
// reloads like everything else.

PTCurveEditor {
	var <object, <window, userView, nameField, lineMenu, statusView;
	var <curves;             // Array of Array: one per line
	var lineIndex = 0;
	var lastIndex, lastValue;

	classvar <views;

	*initClass { views = IdentityDictionary.new }

	*new { |object, bounds|
		var existing = views[object.name];
		if(existing.notNil and: { existing.window.isClosed.not }) {
			existing.window.front;
			^existing
		};
		^super.new.init(object, bounds)
	}

	init { |argObject, bounds|
		object = argObject;
		views[object.name] = this;
		curves = this.prReadCurves;

		window = PTGUI.window(
			"draw % : %".format(object.type, object.name),
			bounds ?? { Rect(240, 200, 640, 400) }
		);

		userView = UserView()
			.background_(PTGUI.color(\field))
			.drawFunc_({ |view| this.draw(view) })
			.resize_(5)
			.mouseDownAction_({ |view, x, y| lastIndex = nil; this.prTouch(view, x, y) })
			.mouseMoveAction_({ |view, x, y| this.prTouch(view, x, y) })
			.mouseUpAction_({ lastIndex = nil });

		nameField = PTGUI.field(object.name.asString);

		lineMenu = PTGUI.popUp(if(curves.size > 1) { ["top", "bottom"] } { ["line"] })
			.action_({ |menu| lineIndex = menu.value; userView.refresh })
			.enabled_(curves.size > 1);

		statusView = StaticText()
			.font_(PTGUI.mono(11))
			.stringColor_(PTGUI.color(\dim))
			.string_(this.statusString);

		window.layout = VLayout(
			HLayout(PTGUI.label("name", 46), nameField, lineMenu),
			[userView, stretch: 1],
			statusView,
			HLayout(
				PTGUI.button("flat", { this.flatten }),
				PTGUI.button("invert", { this.invert }),
				PTGUI.button("smooth", { this.smooth }),
				PTGUI.button("normalise", { this.normalise }),
				nil,
				PTGUI.button("make", { this.accept })
			)
		).margins_(8).spacing_(5);

		window.onClose_({ views.removeAt(object.name) });
		window.front;
		^this
	}

	// -------------------------------------------------------------- the curves

	prReadCurves {
		var resolution = PTCurve.drawResolution;
		if(object.isMade.not) {
			^if(object.isKindOf(PTMask)) {
				[PTCurve.flat(75, resolution), PTCurve.flat(25, resolution)]
			} {
				[PTCurve.flat(50, resolution)]
			}
		};
		^if(object.isKindOf(PTMask)) {
			[PTCurve.sample(object.top, resolution), PTCurve.sample(object.bottom, resolution)]
		} {
			[PTCurve.sample(object.points, resolution)]
		}
	}

	// Screen point to curve point, filling in any gap since the last one so a
	// fast drag does not leave holes.
	prTouch { |view, x, y|
		var bounds = view.bounds;
		var resolution = curves[lineIndex].size;
		var index = (x / bounds.width * resolution).floor.asInteger.clip(0, resolution - 1);
		var value = (1 - (y / bounds.height)).clip(0, 1) * 100;
		curves[lineIndex] = PTCurve.interpolateInto(
			curves[lineIndex], lastIndex, lastValue, index, value
		);
		lastIndex = index;
		lastValue = value;
		statusView.string_(this.statusString);
		view.refresh;
	}

	// ---------------------------------------------------------------- actions

	flatten { curves[lineIndex] = PTCurve.flat(50, curves[lineIndex].size); this.prRedraw }

	invert { curves[lineIndex] = curves[lineIndex].collect { |v| 100 - v }; this.prRedraw }

	smooth {
		var points = curves[lineIndex];
		curves[lineIndex] = points.collect { |v, i|
			[points[(i - 1).clip(0, points.size - 1)], v, points[(i + 1).clip(0, points.size - 1)]].mean
		};
		this.prRedraw;
	}

	// Stretch the selected line, or both lines of a mask together, to fill the
	// full height. The AC Toolbox Resize.
	normalise {
		var all = if(object.isKindOf(PTMask)) { curves.flat } { curves[lineIndex] };
		var min = all.minItem, max = all.maxItem;
		if(min == max) { ^this.prRedraw };
		if(object.isKindOf(PTMask)) {
			curves = curves.collect { |line| line.collect { |v| v.linlin(min, max, 0, 100) } };
		} {
			curves[lineIndex] = curves[lineIndex].collect { |v| v.linlin(min, max, 0, 100) };
		};
		this.prRedraw;
	}

	// Write the curves back into the spec as literal arrays, then make.
	accept {
		var wanted = nameField.string.stripWhiteSpace.asSymbol;
		var rounded = curves.collect { |line| line.collect { |v| v.round(0.01) } };
		if(wanted != object.name and: { wanted.asString.isEmpty.not }) {
			var fresh = object.class.new(wanted);
			fresh.spec_(object.spec.copy);
			fresh.comment_(object.comment);
			views.removeAt(object.name);
			object = fresh;
			views[object.name] = this;
			window.name = "draw % : %".format(object.type, object.name);
		};
		if(object.isKindOf(PTMask)) {
			object.spec.put(\top, rounded[0].asCompileString);
			object.spec.put(\bottom, rounded[1].asCompileString);
		} {
			object.spec.put(\source, rounded[0].asCompileString);
		};
		object.spec.put(\number, "");
		object.make;
		statusView.string_(this.statusString);
		^object
	}

	// ---------------------------------------------------------------- display

	statusString {
		var line = curves[lineIndex];
		^"% points   % %   range %  to %".format(
			line.size,
			curves.size,
			if(curves.size > 1) { "lines" } { "line" },
			line.minItem.round(0.1), line.maxItem.round(0.1)
		)
	}

	prRedraw { statusView.string_(this.statusString); userView.refresh }

	draw { |view|
		var bounds = view.bounds;
		var toPoint = { |line, i|
			Point(
				i / (line.size - 1) * bounds.width,
				(1 - (line[i] / 100)) * bounds.height
			)
		};
		// horizontal guides at 0, 25, 50, 75, 100
		(0, 25 .. 100).do { |percent|
			var y = (1 - (percent / 100)) * bounds.height;
			Pen.strokeColor = if(percent == 50) {
				PTGUI.color(\gridStrong)
			} {
				PTGUI.color(\grid)
			};
			Pen.line(Point(0, y), Point(bounds.width, y));
			Pen.stroke;
		};

		// a mask is a field, so fill between its lines
		if(curves.size > 1) {
			Pen.fillColor = PTGUI.color(\accentSoft);
			Pen.moveTo(toPoint.value(curves[0], 0));
			curves[0].size.do { |i| Pen.lineTo(toPoint.value(curves[0], i)) };
			(curves[1].size - 1, curves[1].size - 2 .. 0).do { |i|
				Pen.lineTo(toPoint.value(curves[1], i))
			};
			Pen.fill;
		};

		curves.do { |line, index|
			Pen.strokeColor = PTGUI.color(if(index == lineIndex) { \accent } { \dim });
			Pen.width = if(index == lineIndex) { 2 } { 1 };
			Pen.moveTo(toPoint.value(line, 0));
			line.size.do { |i| Pen.lineTo(toPoint.value(line, i)) };
			Pen.stroke;
		};
	}

	close { window.close }
}
