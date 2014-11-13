/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Yuya Tanaka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.ypresto.qtfaststart;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

// Ported from qt-faststart.c, released in public domain.
// I'll make this open source. :)
// blob: d2a06242966d7a640d32d304a5653f4e1545f259
// commit: 0ea54d698be613465d92a82495001ddabae128b0
// author: ypresto
public class QtFastStart {
    public static boolean sDEBUG = false;

    public static void main(String[] args) {
        sDEBUG = true;
        if (args.length < 2) {
            printf("input file and output file is required.");
            System.exit(1);
        }
        try {
            fastStart(new File(args[0]), new File(args[1]));
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                printe(e, "Failed to close file: ");
            }
        }
    }

    /* package */
    static long uint32ToLong(int int32) {
        return int32 & 0x00000000ffffffffL;
    }

    /**
     * Ensures passed uint32 value in long can be represented as Java int.
     */
    /* package */
    static int uint32ToInt(int uint32) throws UnsupportedFileException {
        if (uint32 < 0) {
            throw new UnsupportedFileException("uint32 value is too large");
        }
        return uint32;
    }

    /**
     * Ensures passed uint32 value in long can be represented as Java int.
     */
    /* package */
    static int uint32ToInt(long uint32) throws UnsupportedFileException {
        if (uint32 > Integer.MAX_VALUE || uint32 < 0) {
            throw new UnsupportedFileException("uint32 value is too large");
        }
        return (int) uint32;
    }

    /**
     * Ensures passed uint64 value can be represented as Java long.
     */
    /* package */
    static long uint64ToLong(long uint64) throws UnsupportedFileException {
        if (uint64 < 0) throw new UnsupportedFileException("uint64 value is too large");
        return uint64;
    }

    private static int fourCcToInt(byte[] byteArray) {
        return ByteBuffer.wrap(byteArray).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private static void printf(String format, Object... args) {
        if (sDEBUG) System.err.println("QtFastStart: " + String.format(format, args));
    }

    private static void printe(Throwable e, String format, Object... args) {
        printf(format, args);
        if (sDEBUG) e.printStackTrace();
    }

    private static boolean readAndFill(FileChannel infile, ByteBuffer buffer) throws IOException {
        buffer.clear();
        int size = infile.read(buffer);
        buffer.flip();
        return size == buffer.capacity();
    }

    private static boolean readAndFill(FileChannel infile, ByteBuffer buffer, long position) throws IOException {
        buffer.clear();
        int size = infile.read(buffer, position);
        buffer.flip();
        return size == buffer.capacity();
    }

    /* top level atoms */
    private static final int FREE_ATOM = fourCcToInt(new byte[]{'f', 'r', 'e', 'e'});
    private static final int JUNK_ATOM = fourCcToInt(new byte[]{'j', 'u', 'n', 'k'});
    private static final int MDAT_ATOM = fourCcToInt(new byte[]{'m', 'd', 'a', 't'});
    private static final int MOOV_ATOM = fourCcToInt(new byte[]{'m', 'o', 'o', 'v'});
    private static final int PNOT_ATOM = fourCcToInt(new byte[]{'p', 'n', 'o', 't'});
    private static final int SKIP_ATOM = fourCcToInt(new byte[]{'s', 'k', 'i', 'p'});
    private static final int WIDE_ATOM = fourCcToInt(new byte[]{'w', 'i', 'd', 'e'});
    private static final int PICT_ATOM = fourCcToInt(new byte[]{'P', 'I', 'C', 'T'});
    private static final int FTYP_ATOM = fourCcToInt(new byte[]{'f', 't', 'y', 'p'});
    private static final int UUID_ATOM = fourCcToInt(new byte[]{'u', 'u', 'i', 'd'});

    private static final int CMOV_ATOM = fourCcToInt(new byte[]{'c', 'm', 'o', 'v'});
    private static final int STCO_ATOM = fourCcToInt(new byte[]{'s', 't', 'c', 'o'});
    private static final int CO64_ATOM = fourCcToInt(new byte[]{'c', 'o', '6', '4'});

    private static final int ATOM_PREAMBLE_SIZE = 8;

    /**
     * @param in  Input file.
     * @param out Output file.
     * @return false if input file is already fast start.
     * @throws IOException
     */
    public static boolean fastStart(File in, File out) throws IOException, MalformedFileException, UnsupportedFileException {
        boolean ret = false;
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        try {
            inStream = new FileInputStream(in);
            FileChannel infile = inStream.getChannel();
            outStream = new FileOutputStream(out);
            FileChannel outfile = outStream.getChannel();
            return ret = fastStartImpl(infile, outfile);
        } finally {
            safeClose(inStream);
            safeClose(outStream);
            if (!ret) {
                out.delete();
            }
        }
    }

    private static boolean fastStartImpl(FileChannel infile, FileChannel outfile) throws IOException, MalformedFileException, UnsupportedFileException {
        ByteBuffer atomBytes = ByteBuffer.allocate(ATOM_PREAMBLE_SIZE).order(ByteOrder.BIG_ENDIAN);
        int atomType = 0;
        long atomSize = 0; // uint64_t
        long lastOffset;
        ByteBuffer moovAtom;
        ByteBuffer ftypAtom = null;
        // uint64_t, but assuming it is in int32 range. It is reasonable as int max is around 2GB. Such large moov is unlikely, yet unallocatable :).
        int moovAtomSize;
        long startOffset = 0;

        // traverse through the atoms in the file to make sure that 'moov' is at the end
        while (readAndFill(infile, atomBytes)) {
            atomSize = uint32ToLong(atomBytes.getInt()); // uint32
            atomType = atomBytes.getInt(); // representing uint32_t in signed int

            // keep ftyp atom
            if (atomType == FTYP_ATOM) {
                int ftypAtomSize = uint32ToInt(atomSize); // XXX: assume in range of int32_t
                ftypAtom = ByteBuffer.allocate(ftypAtomSize).order(ByteOrder.BIG_ENDIAN);
                atomBytes.rewind();
                ftypAtom.put(atomBytes);
                if (infile.read(ftypAtom) < ftypAtomSize - ATOM_PREAMBLE_SIZE) break;
                ftypAtom.flip();
                startOffset = infile.position(); // after ftyp atom
            } else {
                if (atomSize == 1) {
                    /* 64-bit special case */
                    atomBytes.clear();
                    if (!readAndFill(infile, atomBytes)) break;
                    atomSize = uint64ToLong(atomBytes.getLong()); // XXX: assume in range of int64_t
                    infile.position(infile.position() + atomSize - ATOM_PREAMBLE_SIZE * 2); // seek
                } else {
                    infile.position(infile.position() + atomSize - ATOM_PREAMBLE_SIZE); // seek
                }
            }
            if (sDEBUG) printf("%c%c%c%c %10d %d",
                    (atomType >> 24) & 255,
                    (atomType >> 16) & 255,
                    (atomType >> 8) & 255,
                    (atomType >> 0) & 255,
                    infile.position() - atomSize,
                    atomSize);
            if ((atomType != FREE_ATOM)
                    && (atomType != JUNK_ATOM)
                    && (atomType != MDAT_ATOM)
                    && (atomType != MOOV_ATOM)
                    && (atomType != PNOT_ATOM)
                    && (atomType != SKIP_ATOM)
                    && (atomType != WIDE_ATOM)
                    && (atomType != PICT_ATOM)
                    && (atomType != UUID_ATOM)
                    && (atomType != FTYP_ATOM)) {
                printf("encountered non-QT top-level atom (is this a QuickTime file?)");
                break;
            }

        /* The atom header is 8 (or 16 bytes), if the atom size (which
         * includes these 8 or 16 bytes) is less than that, we won't be
         * able to continue scanning sensibly after this atom, so break. */
            if (atomSize < 8)
                break;
        }

        if (atomType != MOOV_ATOM) {
            printf("last atom in file was not a moov atom");
            return false;
        }

        // moov atom was, in fact, the last atom in the chunk; load the whole moov atom

        // atomSize is uint64, but for moov uint32 should be stored.
        // XXX: assuming moov atomSize <= max vaue of int32
        moovAtomSize = uint32ToInt(atomSize);
        lastOffset = infile.size() - moovAtomSize; // NOTE: assuming no extra data after moov, as qt-faststart.c
        moovAtom = ByteBuffer.allocate(moovAtomSize).order(ByteOrder.BIG_ENDIAN);
        if (!readAndFill(infile, moovAtom, lastOffset)) {
            throw new MalformedFileException("failed to read moov atom");
        }

        // this utility does not support compressed atoms yet, so disqualify files with compressed QT atoms
        if (moovAtom.getInt(12) == CMOV_ATOM) {
            throw new UnsupportedFileException("this utility does not support compressed moov atoms yet");
        }

        // crawl through the moov chunk in search of stco or co64 atoms
        while (moovAtom.remaining() >= 8) {
            int atomHead = moovAtom.position();
            atomType = moovAtom.getInt(atomHead + 4); // representing uint32_t in signed int
            if (!(atomType == STCO_ATOM || atomType == CO64_ATOM)) {
                moovAtom.position(moovAtom.position() + 1);
                continue;
            }
            atomSize = uint32ToLong(moovAtom.getInt(atomHead)); // uint32
            if (atomSize > moovAtom.remaining()) {
                throw new MalformedFileException("bad atom size");
            }
            moovAtom.position(atomHead + 12); // skip size (4 bytes), type (4 bytes), version (1 byte) and flags (3 bytes)
            if (moovAtom.remaining() < 4) {
                throw new MalformedFileException("malformed atom");
            }
            // uint32_t, but assuming moovAtomSize is in int32 range, so this will be in int32 range
            int offsetCount = uint32ToInt(moovAtom.getInt());
            if (atomType == STCO_ATOM) {
                printf("patching stco atom...");
                if (moovAtom.remaining() < offsetCount * 4) {
                    throw new MalformedFileException("bad atom size/element count");
                }
                for (int i = 0; i < offsetCount; i++) {
                    int currentOffset = moovAtom.getInt(moovAtom.position());
                    int newOffset = currentOffset + moovAtomSize; // calculate uint32 in int, bitwise addition
                    // current 0xffffffff => new 0x00000000 (actual >= 0x0000000100000000L)
                    if (currentOffset < 0 && newOffset >= 0) {
                        throw new UnsupportedFileException("This is bug in original qt-faststart.c: "
                                + "stco atom should be extended to co64 atom as new offset value overflows uint32, "
                                + "but is not implemented.");
                    }
                    moovAtom.putInt(newOffset);
                }
            } else if (atomType == CO64_ATOM) {
                printf("patching co64 atom...");
                if (moovAtom.remaining() < offsetCount * 8) {
                    throw new MalformedFileException("bad atom size/element count");
                }
                for (int i = 0; i < offsetCount; i++) {
                    long currentOffset = moovAtom.getLong(moovAtom.position());
                    moovAtom.putLong(currentOffset + moovAtomSize); // calculate uint64 in long, bitwise addition
                }
            }
        }

        infile.position(startOffset); // seek after ftyp atom

        if (ftypAtom != null) {
            // dump the same ftyp atom
            printf("writing ftyp atom...");
            ftypAtom.rewind();
            outfile.write(ftypAtom);
        }

        // dump the new moov atom
        printf("writing moov atom...");
        moovAtom.rewind();
        outfile.write(moovAtom);

        // copy the remainder of the infile, from offset 0 -> (lastOffset - startOffset) - 1
        printf("copying rest of file...");
        infile.transferTo(startOffset, lastOffset - startOffset, outfile);

        return true;
    }

    public static class QtFastStartException extends Exception {
        private QtFastStartException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static class MalformedFileException extends QtFastStartException {
        private MalformedFileException(String detailMessage) {
            super(detailMessage);
        }
    }

    public static class UnsupportedFileException extends QtFastStartException {
        private UnsupportedFileException(String detailMessage) {
            super(detailMessage);
        }
    }
}
