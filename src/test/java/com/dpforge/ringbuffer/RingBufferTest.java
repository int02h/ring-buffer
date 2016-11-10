package com.dpforge.ringbuffer;

import org.junit.Test;

import static org.junit.Assert.*;

public class RingBufferTest {
    @Test
    public void normalReadWrite() {
        RingBuffer buffer = new RingBuffer(10);

        // [..........]
        checkBuffer(buffer, 0, 0);

        RingBuffer.Range writeRange = new RingBuffer.Range();
        buffer.beginWriting(8, writeRange);
        checkRange(writeRange, 0, 7);
        buffer.finishWriting(5);

        // [#####.....]
        checkBuffer(buffer, 0, 5);

        RingBuffer.Range readRange = new RingBuffer.Range();
        buffer.beginReading(10, readRange);
        checkRange(readRange, 0, 4);
        buffer.finishReading(3);

        // [...##.....]
        checkBuffer(buffer, 3, 2);

        buffer.beginWriting(4, writeRange);
        checkRange(writeRange, 5, 8);
        buffer.finishWriting(4);

        // [...######.]
        checkBuffer(buffer, 3, 6);

        buffer.beginReading(5, readRange);
        checkRange(readRange, 3, 7);
        buffer.finishReading(5);

        // [........#.]
        checkBuffer(buffer, 8, 1);
    }

    @Test
    public void writeWrap() {
        RingBuffer buffer = new RingBuffer(10);
        RingBuffer.Range writeRange = new RingBuffer.Range();

        buffer.beginWriting(7, writeRange);
        checkRange(writeRange, 0, 6);
        buffer.finishWriting(7);
        checkBuffer(buffer, 0, 7);

        buffer.beginWriting(5, writeRange);
        checkRange(writeRange, 7, 9);
        buffer.finishWriting(3);
        checkBuffer(buffer, 0, 10);
    }

    @Test
    public void readWrap() {
        RingBuffer buffer = new RingBuffer(10);
        RingBuffer.Range readRange = new RingBuffer.Range();

        buffer.beginWriting(10, new RingBuffer.Range());
        buffer.finishWriting(10);
        checkBuffer(buffer, 0, 10);

        buffer.beginReading(5, readRange);
        checkRange(readRange, 0, 4);
        buffer.finishReading(5);
        checkBuffer(buffer, 5, 5);

        buffer.beginReading(10, readRange);
        checkRange(readRange, 5, 9);
        buffer.finishReading(5);
        checkBuffer(buffer, 0, 0);
    }

    @Test
    public void wrapping() {
        RingBuffer buffer = new RingBuffer(10);
        RingBuffer.Range readRange = new RingBuffer.Range();
        RingBuffer.Range writeRange = new RingBuffer.Range();

        buffer.beginWriting(10, new RingBuffer.Range());
        buffer.finishWriting(10);
        // [##########]
        checkBuffer(buffer, 0, 10);

        buffer.beginReading(7, readRange);
        checkRange(readRange, 0, 6);
        buffer.finishReading(6);
        // [......####]
        checkBuffer(buffer, 6, 4);

        buffer.beginWriting(4, writeRange);
        checkRange(writeRange, 0, 3);
        buffer.finishWriting(3);
        // [###...####]
        checkBuffer(buffer, 6, 7);

        buffer.beginWriting(10, writeRange);
        checkRange(writeRange, 3, 5);
        buffer.finishWriting(3);
        // [##########]
        checkBuffer(buffer, 6, 10);

        buffer.beginReading(10, readRange);
        checkRange(readRange, 6, 9);
        buffer.finishReading(4);
        // [######....]
        checkBuffer(buffer, 0, 6);

        buffer.beginReading(10, readRange);
        checkRange(readRange, 0, 5);
        buffer.finishReading(6);

        // [..........]
        checkBuffer(buffer, 6, 0);
    }

    @Test
    public void wrapAndFull() {
        RingBuffer buffer = new RingBuffer(10);
        RingBuffer.Range readRange = new RingBuffer.Range();
        RingBuffer.Range writeRange = new RingBuffer.Range();

        buffer.beginWriting(7, writeRange);
        checkRange(writeRange, 0, 6);
        buffer.finishWriting(7);
        // [#######...]
        checkBuffer(buffer, 0, 7);

        buffer.beginWriting(7, writeRange);
        checkRange(writeRange, 7, 9);
        buffer.finishWriting(3);
        // [##########]
        checkBuffer(buffer, 0, 10);

        buffer.beginReading(3, readRange);
        checkRange(readRange, 0, 2);
        buffer.finishReading(3);
        // [...#######]
        checkBuffer(buffer, 3, 7);

        buffer.beginWriting(10, writeRange);
        checkRange(writeRange, 0, 2);
        buffer.finishWriting(3);
        // [##########]
        checkBuffer(buffer, 3, 10);
    }

    @Test
    public void zeroWrite() {
        RingBuffer buffer = new RingBuffer(10);
        RingBuffer.Range writeRange = new RingBuffer.Range();

        buffer.beginWriting(0, writeRange);
        checkRange(writeRange, RingBuffer.Range.INVALID_INDEX, RingBuffer.Range.INVALID_INDEX);
        buffer.finishWriting(0);
    }

    @Test
    public void zeroRead() {
        RingBuffer buffer = new RingBuffer(10);
        RingBuffer.Range readRange = new RingBuffer.Range();

        buffer.beginWriting(5, new RingBuffer.Range());
        buffer.finishWriting(5);

        buffer.beginReading(0, readRange);
        checkRange(readRange, RingBuffer.Range.INVALID_INDEX, RingBuffer.Range.INVALID_INDEX);
        buffer.finishReading(0);
    }

    @Test
    public void fullWrite() {
        RingBuffer buffer = new RingBuffer(10);
        RingBuffer.Range writeRange = new RingBuffer.Range();

        buffer.beginWriting(8, writeRange);
        checkRange(writeRange, 0, 7);
        buffer.finishWriting(8);

        buffer.beginWriting(8, writeRange);
        checkRange(writeRange, 8, 9);
        buffer.finishWriting(2);

        buffer.beginWriting(1, writeRange);
        checkRange(writeRange, RingBuffer.Range.INVALID_INDEX, RingBuffer.Range.INVALID_INDEX);
        buffer.finishWriting(0);
    }

    @Test
    public void emptyRead() {
        RingBuffer buffer = new RingBuffer(10);
        RingBuffer.Range readRange = new RingBuffer.Range();

        buffer.beginWriting(5, new RingBuffer.Range());
        buffer.finishWriting(5);

        buffer.beginReading(5, readRange);
        checkRange(readRange, 0, 4);
        buffer.finishReading(4);

        buffer.beginReading(5, readRange);
        checkRange(readRange, 4, 4);
        buffer.finishReading(1);

        buffer.beginReading(10, readRange);
        checkRange(readRange, RingBuffer.Range.INVALID_INDEX, RingBuffer.Range.INVALID_INDEX);
        buffer.finishReading(0);
    }

    @Test
    public void isFullOrEmpty() {
        RingBuffer buffer = new RingBuffer(2);

        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());

        buffer.beginWriting(1, new RingBuffer.Range());
        buffer.finishWriting(1);

        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());

        buffer.beginWriting(1, new RingBuffer.Range());
        buffer.finishWriting(1);

        assertFalse(buffer.isEmpty());
        assertTrue(buffer.isFull());

        buffer.beginReading(1, new RingBuffer.Range());
        buffer.finishReading(1);

        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());

        buffer.beginReading(1, new RingBuffer.Range());
        buffer.finishReading(1);

        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
    }

    @Test
    public void clearBuffer() {
        RingBuffer buffer = new RingBuffer(3);
        RingBuffer.Range range = new RingBuffer.Range();

        buffer.beginWriting(3, range);
        checkRange(range, 0, 2);
        buffer.finishWriting(3);

        assertFalse(buffer.isEmpty());
        assertTrue(buffer.isFull());

        buffer.beginWriting(3, range);
        checkRange(range, RingBuffer.Range.INVALID_INDEX, RingBuffer.Range.INVALID_INDEX);
        buffer.finishWriting(0);

        assertFalse(buffer.isEmpty());
        assertTrue(buffer.isFull());

        buffer.clear();

        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());

        buffer.beginWriting(3, range);
        checkRange(range, 0, 2);
        buffer.finishWriting(3);
    }

    @Test
    public void clearRange() {
        RingBuffer buffer = new RingBuffer(3);
        RingBuffer.Range range = new RingBuffer.Range();

        assertFalse(range.isValid());

        buffer.beginWriting(1, range);
        assertTrue(range.isValid());
        checkRange(range, 0, 0);
        buffer.finishWriting(1);

        assertTrue(range.isValid());
        range.clear();
        assertFalse(range.isValid());
    }

    private static void checkBuffer(RingBuffer buffer, int index, int size) {
        assertEquals(index, buffer.getDataIndex());
        assertEquals(size, buffer.getDataSize());
    }

    private static void checkRange(RingBuffer.Range range, int start, int end) {
        assertEquals(start, range.getStart());
        assertEquals(end, range.getEnd());

        assertEquals(start != RingBuffer.Range.INVALID_INDEX && end != RingBuffer.Range.INVALID_INDEX,
                range.isValid());
    }
}