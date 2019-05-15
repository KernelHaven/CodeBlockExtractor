# CodeBlockExtractor

![Build Status](https://jenkins-2.sse.uni-hildesheim.de/buildStatus/icon?job=KH_CodeBlockExtractor)

A code-model extractor for [KernelHaven](https://github.com/KernelHaven/KernelHaven).

A simple extractor that extracts `#ifdef` blocks in C source files (`*.c`).

## Capabilities

* Extracts `#if`, `#ifdef`, `#ifndef`, `#elif` and `#else` blocks
* Preserves the nesting structure
* Parses the conditions of the blocks into Boolean formulas
	* Optionally does replacements for integer comparisons ("fuzzy parsing"), e.g. `VAR == 3` becomes the Boolean variable `VAR_eq_3`
	* Optionally considers `IS_ENABLED`, `IS_BUILTIN` and `IS_MODULE` macros used in the Linux Kernel
* Calculates presence conditions for nested blocks
* Provides the start and end line numbers of blocks
* Optionally creates a pseudo-block with condition `true` for the whole file if there is code outside of blocks
* Considers line continuation of preprocessor directives (a `\` at the end of the line)
* Considers comments (commented out blocks are ignored)
* The result is provided as a hierachy of `CodeBlock`s to the analysis

## Usage

Place [`CodeBlockExtractor.jar`](https://jenkins-2.sse.uni-hildesheim.de/job/KH_CodeBlockExtractor/lastSuccessfulBuild/artifact/build/jar/CodeBlockExtractor.jar) in the plugins folder of KernelHaven.

To use this extractor, set `code.extractor.class` to `net.ssehub.kernel_haven.block_extractor.CodeBlockExtractor` in the KernelHaven properties.

## Dependencies

This plugin has no additional dependencies other than KernelHaven.

## License

This plugin is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

Note that some of the files in `testdata/scenario` may be licensed differently (e.g. because they are taken from the Linux Kernel). They serve only as a test case and are not included in any JAR release of this plugin.
