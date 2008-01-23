/*
 * Copyright 2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.squawk.flash;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreFullException;

import com.sun.squawk.Address;
import com.sun.squawk.VM;
import com.sun.squawk.peripheral.INorFlashSector;
import com.sun.squawk.peripheral.INorFlashSectorAllocator;
import com.sun.squawk.util.Arrays;
import com.sun.squawk.util.Comparer;
import com.sun.squawk.util.UnexpectedException;

public class NorFlashMemoryHeap implements INorFlashMemoryHeap {

    public static final byte ERASED_VALUE = (byte) 0xFF;
    public static final byte ERASED_VALUE_XOR = (byte) 0x00;
    // size (int) + allocated (byte, byte)
    public static final int BLOCK_HEADER_SIZE = 4 + 2;
    public static final byte[] BLOCK_FOOTER = new byte[] {ERASED_VALUE_XOR, ERASED_VALUE_XOR};

    protected INorFlashSectorState[] sectorStates;
    protected INorFlashSectorState currentSectorState;
    protected int erasedSequenceCurrentValue;
    protected INorFlashSectorStateList inUseSectorStateList;
    protected INorFlashSectorStateList toBeErasedSectorStateList;
    protected boolean hasScannedBlocks;
    
    /**
     * 
     * @return SquawkVector<INorFlashMemorySector>
     */
    public static Vector getNorFlashSectors(int purpose) {
        // Get all user purposed flash memory sectors
    	
    	INorFlashSectorAllocator allocator = (INorFlashSectorAllocator) VM.getPeripheralRegistry().getSingleton(INorFlashSectorAllocator.class);
    	if (allocator == null) {
    		throw new IllegalStateException("No INorFlashSectorAllocator available");
    	}
    	INorFlashSector[] allFlashMemorySectors;
		try {
			allFlashMemorySectors = allocator.getInitialSectors(purpose);
		} catch (IOException e) {
		    throw new UnexpectedException(e);
		}
        Vector userSectors = new Vector(allFlashMemorySectors.length);
        for (int i=0, max=allFlashMemorySectors.length; i < max; i++) {
            userSectors.addElement(allFlashMemorySectors[i]);
        }
        return userSectors;
    }

    protected NorFlashMemoryHeap() {
        
    }
    
    public NorFlashMemoryHeap(INorFlashSectorState[] sectorStates) {
        init(sectorStates);
    }
    
    /**
     * 
     * @param purpose One of INorFlashMemorySector.USER_PURPOSED
     * @throws IOException
     */
    public NorFlashMemoryHeap(int purpose) {
        Vector userSectors = getNorFlashSectors(purpose);
        if (userSectors.size() ==0) {
            throw new RuntimeException("No user purposed flash memory found.");
        }
        INorFlashSectorState[] sectorStates = new INorFlashSectorState[userSectors.size()];
        for (int i=0, max=userSectors.size(); i < max; i++) {
            INorFlashSector sector = (INorFlashSector) userSectors.elementAt(i);
            sectorStates[i] = new NorFlashSectorState(sector);
        }
        init(sectorStates);
    }
    
    public void forceEraseAll() {
        for (int i=0; i < sectorStates.length; i++) {
            try {
                sectorStates[i].forceErase();
            } catch (RecordStoreException e) {
            }
        }
    }
    
    public void freeBlockAt(Address address) throws RecordStoreException {
        INorFlashSectorState sectorState = getSectorContaining(address);
        // add 4 for the size int at beginning of block header
        int offset = address.diff(sectorState.getStartAddress()).toInt() + BLOCK_HEADER_SIZE - 2;
        sectorState.writeBytes(offset, BLOCK_FOOTER, 0, 2);
        sectorState.decrementMallocedCount();
        sectorState.incrementFreedBlockCount();
        if (hasScannedBlocks && sectorState != currentSectorState && sectorState.getOwningList() != toBeErasedSectorStateList && sectorState.getAllocatedBlockCount() == 0) {
            toBeErasedSectorStateList.addLast(sectorState);
        }
    }

    public IMemoryHeapBlock getBlockAt(Address address) throws RecordStoreException {
        INorFlashSectorState sectorState = getSectorContaining(address);
        if (sectorState == null) {
            throw new RecordStoreException("Address specified is outside my valid range");
        }
        int offset = address.diff(sectorState.getStartAddress()).toInt();
        if (offset >= sectorState.getWriteHeadPosition()) {
            return null;
        }
        MemoryHeapBlock block = new MemoryHeapBlock();
        if (getBlockAt(block, sectorState, offset) && block.isAllocated) {
            return block;
        }
        return null;
    }

    /**
     * 
     * @param block
     * @param sectorState
     * @param offset
     * @return boolean true if there was a block at that address, false if not
     * @throws RecordStoreException
     */
    protected boolean getBlockAt(IMemoryHeapBlock block, INorFlashSectorState sectorState, int offset) throws RecordStoreException {
        try {
            if (offset + BLOCK_HEADER_SIZE >= sectorState.getSize()) {
                return false;
            }
            block.setAddress(sectorState.getStartAddress().add(offset));
            block.setLength(BLOCK_HEADER_SIZE);
            byte[] bytes = block.getBytes();
            sectorState.readBytes(offset, bytes, 0, BLOCK_HEADER_SIZE);
            // If the block header we are looking for starts with ERASED_VALUE, we know we did not write a header here
            if (bytes[0] == ERASED_VALUE) {
                return false;
            }
            DataInputStream input = block.getDataInputStream();
            int blockSize = input.readInt();
            int blockSizeWithPadding = blockSize + (blockSize & 1) + 2;
            if (blockSize < 0 || blockSizeWithPadding > (sectorState.getSize() - offset)) {
                throw new RecordStoreException("Data seems corrupted, read block size bigger than sectorState");
            }
            block.setNextOffset(offset + BLOCK_HEADER_SIZE + blockSizeWithPadding);
            // The allocated flag is written as a word, so we only need to look at the second byte
            input.readByte();
            if (input.readByte() == ERASED_VALUE) {
                block.setIsAllocated(true);
                try {
                    block.setLength(blockSizeWithPadding);
                } catch (OutOfMemoryError e) {
                    // Interesting place to do this, but this is about the only place where
                    // could be allocating a huge amount of memory
                    block.resetBytes();
                    throw new RecordStoreException("OutOfMemoryError on reading a block from the heap, something is likely wrong");
                }
                block.setLength(blockSize);
                bytes = block.getBytes();
                sectorState.readBytes(offset + BLOCK_HEADER_SIZE, bytes, 0, blockSizeWithPadding);
                // Make sure the padding is there to confirm that the write operation DID indeed finish when the block was written
                // If we did not finish writing the entry, then we know this had to be an entry added to the end of the log, so lets
                // make it as if this entry was never written
                if (bytes[blockSizeWithPadding - 1] != ERASED_VALUE_XOR) {
                    block.setIsAllocated(false);
                    return true;
                }
            } else {
                block.setIsAllocated(false);
            }
            return true;
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }
    
    public long getErasedSequenceCurrentValue() {
        return erasedSequenceCurrentValue;
    }

    public INorFlashSectorState getSectorContaining(Address address) {
        int left = 0;
        int right = sectorStates.length -1;
        if (address.lo(sectorStates[left].getStartAddress())) {
            return null;
        }
        if (address.hieq(sectorStates[right].getEndAddress())) {
            return null;
        }
        // Do a binary search to find the correct sectorState
        while (true) {
            int index = left + ((right - left) / 2);
            INorFlashSectorState sectorState = sectorStates[index];
            if (address.lo(sectorState.getStartAddress())) {
                right = index - 1;
            } else {
                if (address.lo(sectorState.getEndAddress())) {
                    return sectorState;
                }
                left = index + 1;
            }
        }
    }
    
    public int getSizeAvailable() throws RecordStoreException {
        final int[] used = new int[1];
        scanBlocks(new INorFlashMemoryHeapScanner() {
            public void reScanBlock(Address oldAddress, Address newAddress, IMemoryHeapBlock block) throws RecordStoreException {
                throw new RuntimeException("System error");
            }
            public void scanBlock(IMemoryHeapBlock block) throws RecordStoreException {
                used[0] += block.getLength();
            }
        });
        int total = 0;
        for (int i=0, max=sectorStates.length; i < max; i++) {
            INorFlashSectorState sectorState = sectorStates[i];
            total += sectorState.getSize();
        }
        return total - used[0];
    }

    public int incrementErasedSequence() {
        return ++erasedSequenceCurrentValue;
    }
    
    protected void init(INorFlashSectorState[] sectorStates) {
        Arrays.sort(sectorStates, new Comparer() {
            public int compare(Object a, Object b) {
                Address aStart = ((INorFlashSectorState) a).getStartAddress();
                Address bStart = ((INorFlashSectorState) b).getStartAddress();
                if (aStart.eq(bStart)) {
                    return 0;
                }
                if (aStart.lo(bStart)) {
                    return -1;
                }
                return +1;
            }
        });
        this.sectorStates = sectorStates;
        erasedSequenceCurrentValue = 0;
        inUseSectorStateList = new NorFlashSectorStateList();
        toBeErasedSectorStateList = new NorFlashSectorStateList();
    }
    
    protected void makeRoomToWrite(int entrySize, INorFlashMemoryHeapScanner scanner) throws RecordStoreException {
        if (currentSectorState != null && currentSectorState.hasAvailable(entrySize)) {
            return;
        }
        // Queue up the current sectorState on the appropriate list
        if (currentSectorState != null) {
            INorFlashSectorStateList list;
            if (currentSectorState.getAllocatedBlockCount() == 0) {
                list = toBeErasedSectorStateList;
            } else {
                list = inUseSectorStateList;
            }
            list.addLast(currentSectorState);
            currentSectorState = null;
        }
        if (sectorStates.length < 2) {
            currentSectorState = toBeErasedSectorStateList.consumeFirst();
            if (currentSectorState == null) {
                throw new RecordStoreFullException("Case of 1 sector does not currently support GC");
            }
            currentSectorState.erase(incrementErasedSequence());
            makeRoomToWrite(entrySize, scanner);
            return;
        }
        if (toBeErasedSectorStateList.size() > 1) {
            // We have plenty of sectorStates that can be erased so lets just use one up
            INorFlashSectorState sectorState = toBeErasedSectorStateList.consumeFirst();
            currentSectorState = sectorState;
            sectorState.erase(incrementErasedSequence());
            if (sectorState.hasAvailable(entrySize)) {
                return;
            }
            throw new RecordStoreFullException("Sectors is " + sectorState.getSize() + " is not large enough to hold an entry of size " + entrySize);
        }
        // we have one sector state left that can be erased, lets copy the still allocated content from
        // one of the in use sectors and go from there
        // TODO
        // - do the 1 in 100 to decide whether to use a dirty or clean one
        // - Need to have a dirty list, and a clean list, so can check dirty list size quickly ?
        // - even when there are available sectors to be erased, need to pick a sectorState once in a while
        INorFlashSectorState inUseSector = inUseSectorStateList.getFirst();
        while (inUseSector != null && inUseSector.getFreedBlockCount() == 0) {
            inUseSector = inUseSector.getNextSector();
        }
        // We have no freed blocks, so there is no space we can recoop
        if (inUseSector == null) {
            throw new RecordStoreFullException("There are no sectorStates with freed blocks");
        }
        // We've identified a sectorState to gc, go through and copy its contents into the next available toBeErased
        if (inUseSector != null) {
            inUseSectorStateList.remove(inUseSector);
            currentSectorState = toBeErasedSectorStateList.consumeFirst();
            currentSectorState.erase(incrementErasedSequence());
            inUseSector.resetHead();
            int offset = inUseSector.getWriteHeadPosition();
            Address startAddress = inUseSector.getStartAddress();
            MemoryHeapBlock block = new MemoryHeapBlock();
            while (true) {
                if (!getBlockAt(block, inUseSector, offset)) {
                    break;
                }
                if (block.isAllocated) {
                    writeBlock(block, scanner, startAddress.add(offset));
                }
                offset = block.getNextBlockOffset();
            }
            inUseSector.removeErasedHeader();
            toBeErasedSectorStateList.addLast(inUseSector);
            makeRoomToWrite(entrySize, scanner);
            return;
        }
        throw new RecordStoreFullException("How to handle this case ?");
    }
    
    public Address allocateAndWriteBlock(byte[] bytes, int offset, int length, INorFlashMemoryHeapScanner scanner) throws RecordStoreException {
        MemoryHeapBlock block = new MemoryHeapBlock();
        block.setBytes(bytes, offset, length);
        return writeBlock(block, scanner, Address.zero());
    }
    
    /**
     * It is guaranteed that the order that blocks were allocated in, is the order in which they will be iterated.
     * 
     * @param scanner
     * @throws RecordStoreException
     */
    public void scanBlocks(INorFlashMemoryHeapScanner scanner) throws RecordStoreException {
        INorFlashSectorState[] sectorStatesSortedBySequence = new INorFlashSectorState[sectorStates.length];
        System.arraycopy(sectorStates, 0, sectorStatesSortedBySequence, 0, sectorStates.length);
        Arrays.sort(sectorStatesSortedBySequence, new Comparer() {
            public int compare(Object a, Object b) {
                return (int) (((INorFlashSectorState) a).getSequence() - ((INorFlashSectorState) b).getSequence());
            }
        });

        for (int i=0, maxI=sectorStatesSortedBySequence.length, lastI=sectorStatesSortedBySequence.length-1; i < maxI; i++) {
            INorFlashSectorState sectorState;
            sectorState = sectorStatesSortedBySequence[i];
            if (!sectorState.hasErasedHeader()) {
                toBeErasedSectorStateList.addLast(sectorState);
                continue;
            }
            sectorState.resetHead();
            int offset = sectorState.getWriteHeadPosition();
            MemoryHeapBlock block = new MemoryHeapBlock();
            while (true) {
                if (!getBlockAt(block, sectorState, offset)) {
                    break;
                }
                if (block.isAllocated()) {
                    sectorState.incrementAllocatedBlockCount();
                    if (scanner != null) {
                        scanner.scanBlock(block);
                    }
                } else {
                    sectorState.incrementFreedBlockCount();
                }
                offset = block.getNextBlockOffset();
            }
            sectorState.setWriteHeadPosition(offset);
            if (i == lastI) {
                currentSectorState = sectorState;
            } else {
                if (sectorState.getAllocatedBlockCount() == 0) {
                    toBeErasedSectorStateList.addLast(sectorState);
                } else {
                    inUseSectorStateList.addLast(sectorState);
                }
            }
        }
        hasScannedBlocks = true;
    }
    
    protected Address writeBlock(IMemoryHeapBlock block, INorFlashMemoryHeapScanner scanner, Address oldAddress) throws RecordStoreException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream(BLOCK_HEADER_SIZE);
        DataOutputStream output = new DataOutputStream(bytesOut);
        int blockLength = block.getLength();
        try {
            output.writeInt(blockLength);
            output.writeByte(ERASED_VALUE);
            output.writeByte(ERASED_VALUE);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
        if (oldAddress.isZero()) {
            if (!hasScannedBlocks) {
                scanBlocks(null);
            }
            makeRoomToWrite(bytesOut.size() + blockLength + (blockLength & 1) + 2, scanner);
        }
        block.setAddress(currentSectorState.getWriteHeadAddress());
        currentSectorState.writeBytes(bytesOut.toByteArray(), 0, bytesOut.size());
        if ((blockLength & 1) == 1) {
            // MemoryBlock guarantees me an extra byte if I need it
            block.getBytes()[block.getOffset() + blockLength] = ERASED_VALUE_XOR;
        }
        currentSectorState.writeBytes(block.getBytes(), block.getOffset(), blockLength + (blockLength & 1));
        currentSectorState.writeBytes(BLOCK_FOOTER, 0, 2);
        currentSectorState.incrementAllocatedBlockCount();
        if (!oldAddress.isZero()) {
            freeBlockAt(oldAddress);
            scanner.reScanBlock(oldAddress, block.getAddress(), block);
        }
        return block.getAddress();
    }
    
}
