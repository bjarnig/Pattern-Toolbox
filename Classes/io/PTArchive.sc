// PTArchive - reading and writing the toolbox environment.
//
// A .ptx file is plain sclang source: a series of PT.def calls, one per object,
// in creation order. This mirrors the AC Toolbox .acex format (which is plain
// Lisp define forms) and keeps environments readable, diffable and versionable.
//
// Only the spec is written. The seed is written alongside it, so loading a file
// reproduces the exact realization rather than merely the rules.

PTArchive {

	*save { |path, names|
		var file, objects;
		path = this.prPath(path);
		names = names ?? { PT.names };
		objects = names.collect { |n| PT(n) }.reject(_.isNil);
		file = File(path, "w");
		if(file.isOpen.not) { Error("PTArchive: cannot write %".format(path)).throw };
		protect {
			file.write("// Pattern Toolbox environment\n");
			file.write("// written %\n\n".format(Date.getDate.asString));
			objects.do { |object| file.write(object.asCode ++ "\n\n") };
		} { file.close };
		if(PT.verbose) { "PT: wrote % object(s) to %".format(objects.size, path).postln };
		^path
	}

	*load { |path|
		path = this.prPath(path);
		if(File.exists(path).not) { Error("PTArchive: no such file %".format(path)).throw };
		^path.load
	}

	*prPath { |path|
		path = path.standardizePath;
		if(path.endsWith(".ptx").not) { path = path ++ ".ptx" };
		^path
	}
}
