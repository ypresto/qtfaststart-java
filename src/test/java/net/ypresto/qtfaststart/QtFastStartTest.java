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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class QtFastStartTest {

    @Test
    public void testUint32ToLong() throws Exception {
        assertEquals(0x00000000ffffffffL, QtFastStart.uint32ToLong(0xffffffff));
        assertEquals(Integer.MAX_VALUE, QtFastStart.uint32ToLong(Integer.MAX_VALUE));
    }

    @Test
    public void testUint32ToIntWhenLong() throws Exception {
        assertEquals(0x7fffffff, QtFastStart.uint32ToInt(0x000000007fffffffL));
        assertEquals(1L, QtFastStart.uint32ToInt(1L));
        try {
            QtFastStart.uint32ToInt(0x0000000080000000L);
            fail("should throw when sign bit is 1");
        } catch (QtFastStart.UnsupportedFileException e) {
        }
        try {
            QtFastStart.uint32ToInt(-1L);
            fail("should throw when sign bit is 1");
        } catch (QtFastStart.UnsupportedFileException e) {
        }
    }

    @Test
    public void testUint32ToIntWhenInt() throws Exception {
        assertEquals(0x7fffffff, QtFastStart.uint32ToInt(0x7fffffff));
        assertEquals(1, QtFastStart.uint32ToInt(1));
        try {
            QtFastStart.uint32ToInt(0x80000000);
            fail("should throw when sign bit is 1");
        } catch (QtFastStart.UnsupportedFileException e) {
        }
        try {
            QtFastStart.uint32ToInt(-1);
            fail("should throw when sign bit is 1");
        } catch (QtFastStart.UnsupportedFileException e) {
        }
    }

    @Test
    public void testUint64ToLong() throws Exception {
        assertEquals(Long.MAX_VALUE, QtFastStart.uint64ToLong(Long.MAX_VALUE));
        try {
            QtFastStart.uint64ToLong(0xffffffffffffffffL);
            fail("should throw when sign bit is 1");
        } catch (QtFastStart.UnsupportedFileException e) {
        }
        try {
            QtFastStart.uint64ToLong(-1);
            fail("should throw when sign bit is 1");
        } catch (QtFastStart.UnsupportedFileException e) {
        }
    }
}
