// PTGUI - shared look and small helpers for the Pattern Toolbox windows.
//
// One place for the palette, the type scale and the metrics, so the browser,
// the object dialogs and the plots read as one piece of software.

PTGUI {
	// Qt here will not resolve a named Light face: "Helvetica Neue Light",
	// "HelveticaNeue-Light" and "Avenir-Light" all render identically to the
	// fallback, measured by ink coverage. So the lighter look comes from a
	// genuinely lighter family rather than from a weight that is not available.
	// Avenir Next lays down about a fifth less ink than Helvetica Neue at the
	// same size, and PT Mono is appreciably lighter than Menlo.
	// Resolved on first use from these preferences, so the look degrades to the
	// nearest available family instead of to whatever Qt picks. Set either name
	// before opening a window to override.
	classvar <>fontName, <>monoName;
	classvar <>fontSize = 12;

	classvar sansPreference, monoPreference, namesResolved = false;
	classvar palette;

	*initClass {
		sansPreference = ["Avenir Next", "Avenir", "Helvetica Neue", "Inter", "Helvetica"];
		monoPreference = ["PT Mono", "Menlo", "DejaVu Sans Mono", "Consolas", "Monaco"];
		// A light palette. Cool near-white ground, one clean blue accent, and grid
		// lines as black at low alpha so they sit under content rather than on it.
		palette = IdentityDictionary[
			\background   -> Color(0.965, 0.969, 0.976),
			\panel        -> Color(0.925, 0.933, 0.945),
			\field        -> Color(1.0, 1.0, 1.0),
			// ink a shade off black, which reads lighter without losing contrast
			\text         -> Color(0.18, 0.20, 0.24),
			\dim          -> Color(0.49, 0.52, 0.57),
			\accent       -> Color(0.13, 0.42, 0.85),
			\accentSoft   -> Color(0.13, 0.42, 0.85, 0.16),
			// the far end of the accent ramp: velocity is shown as colour depth
			// rather than transparency, which stays crisp against white
			\accentPale   -> Color(0.74, 0.82, 0.94),
			\made         -> Color(0.09, 0.53, 0.33),
			\unmade       -> Color(0.72, 0.45, 0.05),
			\rest         -> Color(0.62, 0.65, 0.70),
			\grid         -> Color(0, 0, 0, 0.06),
			\gridStrong   -> Color(0, 0, 0, 0.17)
		];
	}

	*color { |key| ^palette[key] ?? { Color.gray } }

	// Qt is only asked for the font list once, and lazily: calling it during
	// initClass would run before the GUI is ready.
	*prResolveNames {
		var available;
		if(namesResolved) { ^this };
		namesResolved = true;
		available = try { Font.availableFonts.collect(_.asString) } { [] };
		fontName = fontName ?? { this.prFirstAvailable(available, sansPreference) };
		monoName = monoName ?? { this.prFirstAvailable(available, monoPreference) };
	}

	*prFirstAvailable { |available, wanted|
		// includesEqual, not includes: the latter compares by identity, so two
		// equal Strings never match and every preference silently falls through
		^wanted.detect { |name| available.includesEqual(name) } ?? { wanted.last }
	}

	*font { |size, bold = false|
		this.prResolveNames;
		^Font(fontName, size ? fontSize, bold)
	}

	*mono { |size|
		this.prResolveNames;
		^Font(monoName, size ? fontSize)
	}

	// ------------------------------------------------------------- components

	*label { |string, width|
		var view = StaticText()
			.string_(string)
			.font_(this.font)
			.stringColor_(this.color(\dim))
			.align_(\right);
		if(width.notNil) { view.fixedWidth_(width) };
		^view
	}

	*heading { |string|
		^StaticText()
			.string_(string)
			.font_(this.font(14, true))
			.stringColor_(this.color(\text))
	}

	*field { |string, action|
		^TextField()
			.string_(string ? "")
			.font_(this.mono)
			.stringColor_(this.color(\text))
			.background_(this.color(\field))
			.action_(action)
	}

	*popUp { |items, action|
		^PopUpMenu()
			.items_(items)
			.font_(PTGUI.font)
			.stringColor_(this.color(\text))
			.background_(this.color(\panel))
			.action_(action)
	}

	*button { |string, action, width|
		var view = Button()
			.states_([[string, this.color(\text), this.color(\panel)]])
			.font_(this.font)
			.action_(action);
		if(width.notNil) { view.fixedWidth_(width) };
		^view
	}

	// Make a text field accept a name dragged from the browser, inserting it
	// rather than replacing what is already there.
	*acceptNameDrops { |textField|
		textField
			.canReceiveDragHandler_({ View.currentDrag.isKindOf(Symbol)
				or: { View.currentDrag.isKindOf(String) } })
			.receiveDragHandler_({ |view|
				var current = view.string ? "";
				var dropped = View.currentDrag.asString;
				view.string_(
					if(current.stripWhiteSpace.isEmpty) { dropped } { current ++ " " ++ dropped }
				);
				view.doAction;
			});
		^textField
	}

	*window { |name, bounds|
		var window = Window(name, bounds);
		window.view.background_(this.color(\background));
		^window
	}

	*alert { |string|
		string.warn;
		^nil
	}
}
