# CodeBlockExtractor

<!-- ![Build Status](https://jenkins.sse.uni-hildesheim.de/buildStatus/icon?job=TODO) -->

A code-model extractor for [KernelHaven](https://github.com/KernelHaven/KernelHaven).

A simple extractor that extracts #ifdef blocks in C source files (`*.c`).

## Usage

Place [`CodeBlockExtractor.jar`](TODO) in the plugins folder of KernelHaven.

To use this extractor, set `code.extractor.class` to `net.ssehub.kernel_haven.block_extractor.CodeBlockExtractor` in the KernelHaven properties.

## Dependencies

This plugin has no additional dependencies other than KernelHaven.

## License

This plugin is licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

Note that some of the files in `testdata/scenario` may be licensed differently (e.g. because they are taken from the Linux Kernel). They serve only as a test case and are not included in any JAR release of this plugin.
