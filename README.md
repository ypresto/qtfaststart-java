qtfaststart-java
=================

qt-faststart library (and executable) for Java (MIT)

## What is qt-faststart?

qt-faststart is tiny tool which makes mp4 video file ready for streaming.

It moves 'moov' box, metadata required for starting play, from the end of a file to the beginning.

## Why use this library even there is another one?

I know there is another implementation of qt-faststart for Java in GPL:

https://github.com/LyleE/QTFastStart

This library has below advantages than it:

- MIT licensed.
- Following original code's structure where possible.
- Paying attention to performance; using `FileChannel#transferTo` and not using String comparison.

## License

MIT license.

## Information about original source code

This product is built upon qt-faststart.c from [FFmpeg](https://github.com/FFmpeg/FFmpeg), which is placed in puglic domain.

Original author of qt-faststart.c: Mike Melanson
