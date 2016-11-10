# Ring Buffer

### Basic Info
Simple Java implementation of data structure called [ring (circular) buffer][1]. It uses single fixed-sized byte array as if it were connected end-to-end. This ring buffer is thread-safe and supports only one reader and only writer at the same time. Also it does not support overwriting in case when buffer if full and someone tries to write to it. For more information please look at Javadoc.

### Where to use
It can be useful when on the one side of the buffer there is writer that continuously writing and on the other side there is reader that continuously reading. For example you have one thread that reads bytes from network and puts them to the ring buffer. And at the same time you have another thread that reads those bytes and displays them.

### How to use
Reading and writing operations are based on *Range* concept. When you want to do some operation with buffer you must at first get buffer range (inclusive start and end indices). Reading or writing operation should be done ONLY inside this range.

Writing example:
```java
RingBuffer buffer = new RingBuffer(1024); // 1 KB buffer
RingBuffer.Range writeRange = new RingBuffer.Range();

int availableToWrite = ...;
buffer.beginWriting(availableToWrite, writeRange);

// write here to buffer inside 'writeRange'

int actuallyWritten = ...;
buffer.finishWriting(actuallyWritten);
```

Make sure that you always call *finishWriting* in pair with *beginWriting*. Argument *availableToWrite* of *beginWriting* method says how many bytes you can write at most. Argument *actuallyWritten* of *finishWriting* method says how many bytes you've actually written. Note that *actuallyWritten* must be less or equal than *availableToWrite*.

Reading example:
```java
RingBuffer buffer = ...;
RingBuffer.Range readRange = new RingBuffer.Range();

int availableToRead = ...;
buffer.beginReading(availableToRead, readRange);

// read here from buffer inside 'readRange'

int actuallyRead = ...;
buffer.finishReading(actuallyRead);
```

Make sure that you always call *finishReading* in pair with *beginReading*. Argument *availableToRead* of *beginReading* method says how many bytes you can read at most. Argument *actuallyRead* of *finishReading* method says how many bytes you've actually read. Note that *actuallyRead* must be less or equal than *availableToRead*.

### Installation

There is no Maven artifact for this simple library. Current implementation of ring buffer is only one file. Just copy it to your project sources and begin to use. Good luck!

[1]: https://en.wikipedia.org/wiki/Circular_buffer