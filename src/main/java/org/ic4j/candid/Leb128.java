/*
 * Copyright 2021 Exilor Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.ic4j.candid;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

public final class Leb128 {

	
	public static int readSigned(ByteBuffer buf) {
        int result = 0;
        int cur;
        int count = 0;
        int signBits = -1;     

        do {
            cur = buf.get() & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            signBits <<= 7;
            count++;
        } while (((cur & 0x80) == 0x80) && count < 5);

        if ((cur & 0x80) == 0x80) {
        	CandidError.create(CandidError.CandidErrorCode.PARSE);
        }

        // Sign extend if appropriate
        if (((signBits >> 1) & result) != 0 ) {
            result |= signBits;
        }

        return result;
	}
	

    public static long readUnsigned(ByteBuffer buf) {
        long result = 0;
        long cur;
        int count = 0;
        

        do {
            cur = buf.get() & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            count++;
        } while (((cur & 0x80) == 0x80) && count < 9);

        if ((cur & 0x80) == 0x80) {
            throw CandidError.create(CandidError.CandidErrorCode.PARSE);
        }

        return result;
    } 
    
    public static int readUnsignedInt(ByteBuffer buf) {
        int result = 0;
        int cur;
        int count = 0;
        

        do {
            cur = buf.get() & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            count++;
        } while (((cur & 0x80) == 0x80) && count < 5);

        if ((cur & 0x80) == 0x80) {
            throw CandidError.create(CandidError.CandidErrorCode.PARSE);
        }

        return result;
    }    
	
    
    public static int writeUnsigned(ByteBuffer buf, int value) {
    	int remaining = value >>> 7;

        int bytesWritten = 0;
        
        while (value != 0) {
        	byte b;
        	if(remaining != 0)
        		b = (byte) ((value & 0x7f) | 0x80);
        	else
        		b = (byte) (value);
        	
            value = remaining;
            remaining >>>= 7;
            
            bytesWritten += 1;
        }

        return bytesWritten;
    }    
    
    public static int writeUnsigned(ByteBuffer buf, long value) {
    	long remaining = value >>> 7;

        int bytesWritten = 0;
        
        while (value != 0) {
        	byte b;
        	if(remaining != 0)
        		b = (byte) ((value & 0x7f) | 0x80);
        	else
        		b = (byte) (value);
        	
        	buf.put(b);
            value = remaining;
            remaining >>>= 7;
            
            bytesWritten += 1;
        }
        return bytesWritten;
    }    
    
    public static void writeSigned(ByteBuffer buf, int value) {
        int remaining = value >> 7;
        boolean hasMore = true;
        int end = ((value & Integer.MIN_VALUE) == 0) ? 0 : -1;

        while (hasMore) {
            hasMore = (remaining != end)
                    || ((remaining & 1) != ((value >> 6) & 1));

            buf.put((byte) ((value & 0x7f) | (hasMore ? 0x80 : 0)));
            value = remaining;
            remaining >>= 7;
        }   
    } 
    
    public static void writeSigned(ByteBuffer buf, long value) {
        long remaining = value >> 7;
        boolean hasMore = true;
        int end = ((value & Integer.MIN_VALUE) == 0) ? 0 : -1;

        while (hasMore) {
            hasMore = (remaining != end)
                    || ((remaining & 1) != ((value >> 6) & 1));

            buf.put((byte) ((value & 0x7f) | (hasMore ? 0x80 : 0)));
            value = remaining;
            remaining >>= 7;
        }   
    }    
    
    public static int readUnsigned(byte[] value) {
        int result = 0;
        int cur;
        int count = 0;
        
        List<Byte> list = Arrays.asList(ArrayUtils.toObject(value));
        
        Iterator<Byte> it = list.iterator();

        do {
            cur = it.next() & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            count++;
        } while (((cur & 0x80) == 0x80) && count < 5);

        if ((cur & 0x80) == 0x80) {
            throw CandidError.create(CandidError.CandidErrorCode.PARSE);
        }

        return result;
    }    
    
    public static int readSigned(byte[] value) {
        int result = 0;
        int cur;
        int count = 0;
        int signBits = -1;
        
        List<Byte> list = Arrays.asList(ArrayUtils.toObject(value));
        
        Iterator<Byte> it = list.iterator();        

        do {
            cur = it.next() & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            signBits <<= 7;
            count++;
        } while (((cur & 0x80) == 0x80) && count < 5);

        if ((cur & 0x80) == 0x80) {
        	CandidError.create(CandidError.CandidErrorCode.PARSE);
        }

        // Sign extend if appropriate
        if (((signBits >> 1) & result) != 0 ) {
            result |= signBits;
        }

        return result;
    }	    
    
    public static byte[] writeUnsigned(int value) {
        ArrayList<Byte> list = new ArrayList<Byte>();
    	int remaining = value >>> 7;

        while (remaining != 0) {
        	list.add((byte) ((value & 0x7f) | 0x80));
            value = remaining;
            remaining >>>= 7;
        }

        list.add((byte) (value & 0x7f));
        
        byte[] data = new byte[list.size()];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) list.get(i);
        }
        
        return data;
    } 
    
    public static byte[] writeUnsigned(long value) {
        ArrayList<Byte> list = new ArrayList<Byte>();
    	long remaining = value >>> 7;

        while (remaining != 0) {
        	list.add((byte) ((value & 0x7f) | 0x80));
            value = remaining;
            remaining >>>= 7;
        }

        list.add((byte) (value & 0x7f));
        
        byte[] data = new byte[list.size()];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) list.get(i);
        }
        
        return data;
    }     
    
    public static byte[] writeSigned( int value) {
    	ArrayList<Byte> list = new ArrayList<Byte>();
        int remaining = value >> 7;
        boolean hasMore = true;
        int end = ((value & Integer.MIN_VALUE) == 0) ? 0 : -1;

        while (hasMore) {
            hasMore = (remaining != end)
                    || ((remaining & 1) != ((value >> 6) & 1));

            list.add((byte) ((value & 0x7f) | (hasMore ? 0x80 : 0)));
            value = remaining;
            remaining >>= 7;
        }
        byte[] data = new byte[list.size()];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) list.get(i);
        }
        
        return data;         
    }     

}
