package com.dpforge.ringbuffer;

/**
 * Ring buffer of bytes that offers shared byte array to its clients.
 *
 * <p>Class designed to be thread-safe and tries to minimize unnecessary object instantiations and array copying.
 * It supports only one writer and only one reader at the same time. If more than one reader/writer tries to
 * read/write its data then {@link IllegalStateException} will be thrown. There is no operation that can lead to some
 * kind of locking, sleeping or waiting.</p>
 *
 * <p>This ring buffer does <b>NOT</b> overwrite its data when it is full and someone tries to write something. When
 * ring buffer is full then every {@link #beginWriting(int, Range)} call will immediately succeed but result range will
 * be invalid (see {@link Range#isValid()}). On other hand if buffer is empty every {@link #beginReading(int, Range)}
 * call will immediately succeed but result will be invalid like in case of writing to full buffer.</p>
 */
public class RingBuffer {
    private final Object lock = new Object();
    private final byte[] buffer;
    private int index;
    private int size;
    private boolean isWriting;
    private boolean isReading;

    /**
     * Instantiate new ring buffer
     * @param size size of new buffer in bytes
     * @throws IllegalArgumentException if size in less than 0
     */
    public RingBuffer(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Size must be greater than 0");
        }
        buffer = new byte[size];
    }

    /**
     * Provides index in buffer of first byte of data
     * @return index in buffer
     */
    public int getDataIndex() {
        synchronized (lock) {
            return index;
        }
    }

    /**
     * Provides size in bytes of stored data somewhere in buffer. Use {@link #getDataIndex()} to get index of first
     * byte of data.
     * @return size in bytes
     */
    public int getDataSize() {
        synchronized (lock) {
            return size;
        }
    }

    /**
     * Total size of underlying byte array. It includes empty and occupied bytes.
     * @return size in bytes
     */
    public int getTotalSize() {
        return buffer.length;
    }

    /**
     * Provides reference to array that actually stores buffer data. To avoid array copying original array will be
     * returned. So be careful when modifying it.
     * @return reference to original array
     */
    public byte[] getBuffer() {
        return buffer;
    }

    /**
     * Check if buffer is empty
     * @return {@code true} if buffer is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Check if buffer is full
     * @return {@code true} if buffer is full, {@code false} otherwise
     */
    public boolean isFull() {
        return size == buffer.length;
    }

    /**
     * Initiate writing process. Only one writer can write its data at the same time. Otherwise
     * {@link IllegalStateException} will be thrown.
     *
     * <p><b>CONTRACT</b>: must be called BEFORE {@link #finishWriting(int)} and always be paired to it regardless of
     * any exception thrown while executing this method. The most common way is to call {@link #finishWriting(int)}
     * inside {@code finally} block.</p>
     *
     * @param maxLength how many bytes writer can write at most
     * @param range out parameter that specifies range where writer can write buffer data
     *
     * @throws IllegalStateException if previous writing has not finished with {@link #finishWriting(int)}
     * @throws IllegalArgumentException if {@code maxLength} is less than zero
     * @throws NullPointerException if {@code range} is {@code null}
     */
    public void beginWriting(final int maxLength, final Range range) {
        synchronized (lock) {
            if (isWriting) {
                throw new IllegalStateException("Cannot begin writing until previous finished");
            }
            isWriting = true;

            checkArguments(maxLength, range);

            if (size != buffer.length && maxLength > 0) {
                range.start = (index + size) % buffer.length;
                if (range.start < index) { // wrapped
                    range.end = min(range.start + maxLength, index) - 1; // inclusive index
                } else {
                    range.end = min(range.start + maxLength, buffer.length) - 1; // inclusive index
                }
            } else {
                range.start = range.end = Range.INVALID_INDEX;
            }
        }
    }

    /**
     * Finish writing process
     *
     * <p><b>CONTRACT</b>: must be called AFTER {@link #beginWriting(int, Range)} and always be paired to it
     * regardless of any exception thrown while executing {@link #beginWriting(int, Range)}. The most common way is to
     * call this method inside {@code finally} block.</p>
     *
     * @param actuallyWritten amount of bytes actually written
     *
     * @throws IllegalStateException if no writing process has begun
     * @throws IllegalArgumentException if amount of written bytes is less than 0 or greater than available space
     */
    public void finishWriting(final int actuallyWritten) {
        synchronized (lock) {
            try {
                if (!isWriting) {
                    throw new IllegalStateException("Cannot finish writing because it is not begun");
                }
                if (actuallyWritten < 0) {
                    throw new IllegalArgumentException("Actually written bytes amount is less than 0");
                }
                if (size + actuallyWritten > buffer.length) {
                    throw new IllegalArgumentException("Actually written bytes amount is too large");
                }
                size += actuallyWritten;
            } finally {
                isWriting = false;
            }
        }
    }

    /**
     * Initiate reading process. Only one reader can read its data at the same time. Otherwise
     * {@link IllegalStateException} will be thrown.
     *
     * <p><b>CONTRACT</b>: must be called BEFORE {@link #finishReading(int)} and always be paired to it regardless of
     * any exception thrown while executing this method. The most common way is to call {@link #finishReading(int)}
     * inside {@code finally} block.</p>
     *
     * @param maxLength how many bytes reader can read at most
     * @param range out parameter that specifies range where reader can read buffer data
     *
     * @throws IllegalStateException if previous reading has not finished with {@link #finishReading(int)}
     * @throws IllegalArgumentException if {@code maxLength} is less than zero
     * @throws NullPointerException if {@code range} is {@code null}
     */
    public void beginReading(final int maxLength, final Range range) {
        synchronized (lock) {
            if (isReading) {
                throw new IllegalStateException("Cannot begin reading until previous finished");
            }
            isReading = true;

            checkArguments(maxLength, range);

            if (size > 0 && maxLength > 0) {
                range.start = index;
                range.end = min(index + maxLength, index + size, buffer.length) - 1; // inclusive index
            } else {
                range.start = range.end = Range.INVALID_INDEX;
            }
        }
    }

    /**
     * Finish reading process
     *
     * <p><b>CONTRACT</b>: must be called AFTER {@link #beginReading(int, Range)} and always be paired to it
     * regardless of any exception thrown while executing {@link #beginReading(int, Range)}. The most common way is to
     * call this method inside {@code finally} block.</p>
     *
     * @param actuallyRead amount of bytes actually read
     *
     * @throws IllegalStateException if no reading process has begun
     * @throws IllegalArgumentException if amount of read bytes is less than 0 or greater than data size
     */
    public void finishReading(final int actuallyRead) {
        synchronized (lock) {
            try {
                if (!isReading) {
                    throw new IllegalStateException("Cannot finish reading because it is not begun");
                }
                if (actuallyRead < 0) {
                    throw new IllegalArgumentException("Actually read bytes amount is less than 0");
                }
                if (actuallyRead > size) {
                    throw new IllegalArgumentException("Actually read bytes amount is too large");
                }
                size -= actuallyRead;
                index = (index + actuallyRead) % buffer.length;
            } finally {
                isReading = false;
            }
        }
    }

    /**
     * Mark buffer as empty. This method does not actually clear data in buffer.
     *
     * @throws IllegalStateException if there is active reading process and/or active writer
     */
    public void clear() {
        synchronized (lock) {
            if (isReading) {
                throw new IllegalStateException("Cannot clear buffer when someone reading");
            }
            if (isWriting) {
                throw new IllegalStateException("Cannot clear buffer when someone reading");
            }
            size = 0;
            index = 0;
        }
    }

    private static void checkArguments(final int maxLength, final Range range) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("Max length is less than 0");
        }
        if (range == null) {
            throw new NullPointerException("Range is null");
        }
    }

    private static int min(int a, int b) {
        return Math.min(a, b);
    }

    private static int min(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Specifies continuous range in buffer (start and end indices) where writer can write to and reader can read from.
     * It is guaranteed that end index is never less than start index.
     */
    public static class Range {
        public final static int INVALID_INDEX = -1;

        private int start = INVALID_INDEX;
        private int end = INVALID_INDEX;

        /**
         * Inclusive start index of this range
         */
        public int getStart() {
            return start;
        }

        /**
         * Inclusive end index of this range
         */
        public int getEnd() {
            return end;
        }

        /**
         * Length of this range in bytes
         * @return length in bytes
         */
        public int getLength() {
            return isValid() ? end - start + 1 : 0;
        }

        /**
         * Clears start and end positions of this range. After this call method {@link #isValid()} will return
         * {@code false}.
         */
        public void clear() {
            start = end = INVALID_INDEX;
        }

        /**
         * Checks validity of this range.
         *
         * <p><b>WARNING</b>: empty range ({@link #getLength()} returns 0) is a valid range.</p>
         * @return true - range is valid, false - range is NOT valid
         */
        public boolean isValid() {
            return start != INVALID_INDEX && end != INVALID_INDEX;
        }

        @Override
        public String toString() {
            return String.format("[%d..%d]", start, end);
        }
    }
}
