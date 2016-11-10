package com.dpforge.ringbuffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ErrorTest {
    @Test
    public void writeNegativeAmountOfBytes() {
        final RingBuffer buffer = new RingBuffer(10);
        final RingBuffer.Range range = new RingBuffer.Range();
        buffer.beginWriting(10, range);
        assertTrue(range.isValid());

        try {
            buffer.finishWriting(-1);
            fail("No exception thrown");
        } catch (IllegalArgumentException ignored) {
        }

        // Ensure we able to write after error occurred
        buffer.beginWriting(10, range);
        assertTrue(range.isValid());
        buffer.finishWriting(10);
    }

    @Test
    public void readNegativeAmountOfBytes() {
        final RingBuffer buffer = new RingBuffer(10);
        final RingBuffer.Range range = new RingBuffer.Range();

        // fill with some data
        buffer.beginWriting(1, range);
        assertTrue(range.isValid());
        buffer.getBuffer()[range.getStart()] = 123;
        buffer.finishWriting(range.getLength());

        buffer.beginReading(1, range);
        assertTrue(range.isValid());

        try {
            buffer.finishReading(-1);
            fail("No exception thrown");
        } catch (IllegalArgumentException ignored) {
        }

        // Ensure we able to read after error occurred
        buffer.beginReading(1, range);
        assertTrue(range.isValid());
        buffer.finishReading(1);
    }

    @Test
    public void writeNegativeMaxLength() {
        final RingBuffer buffer = new RingBuffer(10);
        final RingBuffer.Range range = new RingBuffer.Range();

        try {
            buffer.beginWriting(-2, range);
            fail("No exception thrown");
        } catch (IllegalArgumentException ignored) {
        } finally {
            buffer.finishWriting(0);
        }

        // Ensure we able to write after error occurred
        buffer.beginWriting(10, range);
        assertTrue(range.isValid());
        buffer.finishWriting(10);
    }

    @Test
    public void readNegativeMaxLength() {
        final RingBuffer buffer = new RingBuffer(10);
        final RingBuffer.Range range = new RingBuffer.Range();

        // fill with some data
        buffer.beginWriting(1, range);
        assertTrue(range.isValid());
        buffer.getBuffer()[range.getStart()] = 123;
        buffer.finishWriting(range.getLength());

        try {
            buffer.beginReading(-2, range);
        } catch (IllegalArgumentException ignored) {
        } finally {
            buffer.finishReading(0);
        }

        // Ensure we able to read after error occurred
        buffer.beginReading(1, range);
        assertTrue(range.isValid());
        buffer.finishReading(1);
    }

    @Test
    public void writeNullRange() {
        final RingBuffer buffer = new RingBuffer(10);

        try {
            buffer.beginWriting(-2, null);
            fail("No exception thrown");
        } catch (IllegalArgumentException ignored) {
        } finally {
            buffer.finishWriting(0);
        }

        // Ensure we able to write after error occurred
        final RingBuffer.Range range = new RingBuffer.Range();
        buffer.beginWriting(10, range);
        assertTrue(range.isValid());
        buffer.finishWriting(10);
    }

    @Test
    public void readNullRange() {
        final RingBuffer buffer = new RingBuffer(10);
        final RingBuffer.Range range = new RingBuffer.Range();

        // fill with some data
        buffer.beginWriting(1, range);
        assertTrue(range.isValid());
        buffer.getBuffer()[range.getStart()] = 123;
        buffer.finishWriting(range.getLength());

        try {
            buffer.beginReading(-2, null);
        } catch (IllegalArgumentException ignored) {
        } finally {
            buffer.finishReading(0);
        }

        // Ensure we able to read after error occurred
        buffer.beginReading(1, range);
        assertTrue(range.isValid());
        buffer.finishReading(1);
    }

    @Test
    public void toManyWritten() {
        final RingBuffer buffer = new RingBuffer(3);
        final RingBuffer.Range range = new RingBuffer.Range();

        buffer.beginWriting(3, range);
        assertTrue(range.isValid());
        assertEquals(0, range.getStart());
        assertEquals(2, range.getEnd());
        assertEquals(3, range.getLength());

        try {
            buffer.finishWriting(range.getLength() + 1);
            fail("No exception thrown");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void toManyRead() {
        final RingBuffer buffer = new RingBuffer(3);
        final RingBuffer.Range range = new RingBuffer.Range();

        buffer.beginWriting(3, range);
        assertTrue(range.isValid());
        buffer.finishWriting(range.getLength());

        buffer.beginReading(2, range);
        assertTrue(range.isValid());
        assertEquals(0, range.getStart());
        assertEquals(1, range.getEnd());
        assertEquals(2, range.getLength());

        try {
            buffer.finishReading(10);
            fail("No exception thrown");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void finishWritingWithoutBegin() {
        final RingBuffer buffer = new RingBuffer(3);

        try {
            buffer.finishWriting(1);
            fail("No exception thrown");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void finishReadingWithoutBegin() {
        final RingBuffer buffer = new RingBuffer(3);

        try {
            buffer.finishReading(1);
            fail("No exception thrown");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void finishWritingTwice() {
        final RingBuffer buffer = new RingBuffer(3);
        final RingBuffer.Range range = new RingBuffer.Range();

        buffer.beginWriting(1, range);
        assertTrue(range.isValid());
        buffer.finishWriting(1);


        try {
            buffer.finishWriting(1);
            fail("No exception thrown");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void finishReadingTwice() {
        final RingBuffer buffer = new RingBuffer(3);
        final RingBuffer.Range range = new RingBuffer.Range();

        // just write some data
        buffer.beginWriting(1, range);
        assertTrue(range.isValid());
        buffer.finishWriting(1);

        buffer.beginReading(1, range);
        assertTrue(range.isValid());
        buffer.finishReading(1);

        try {
            buffer.finishReading(1);
            fail("No exception thrown");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    public void clearWhileWriting() {
        final RingBuffer buffer = new RingBuffer(3);
        final RingBuffer.Range range = new RingBuffer.Range();

        buffer.beginWriting(1, range);

        try {
            buffer.clear();
            fail("No exception thrown");
        } catch (IllegalStateException ignored) {
        }

        buffer.finishWriting(0);
    }

    @Test
    public void clearWhileReading() {
        final RingBuffer buffer = new RingBuffer(3);
        final RingBuffer.Range range = new RingBuffer.Range();

        // just write some data
        buffer.beginWriting(1, range);
        assertTrue(range.isValid());
        buffer.finishWriting(1);

        buffer.beginReading(1, range);
        try {
            buffer.clear();
            fail("No exception thrown");
        } catch (IllegalStateException ignored) {
        }

        buffer.finishReading(0);
    }
}
