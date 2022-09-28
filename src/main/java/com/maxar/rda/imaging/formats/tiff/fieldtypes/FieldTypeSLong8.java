/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.maxar.rda.imaging.formats.tiff.fieldtypes;

import com.maxar.rda.imaging.ImageWriteException;
import com.maxar.rda.imaging.common.ByteConversions;
import com.maxar.rda.imaging.formats.tiff.TiffField;

import java.nio.ByteOrder;

public class FieldTypeSLong8 extends FieldType {
    public FieldTypeSLong8(final int type, final String name) {
        super(type, name, 8);
    }

    @Override
    public Object getValue(final TiffField entry) {
        final byte[] bytes = entry.getByteArrayValue();
        if (entry.getCount() == 1) {
            return ByteConversions.toLong(bytes,
                    entry.getByteOrder());
        }
        return ByteConversions.toLongs(bytes, entry.getByteOrder());
    }

    @Override
    public byte[] writeData(final Object o, final ByteOrder byteOrder) throws ImageWriteException {
        if (o instanceof Integer) {
            return ByteConversions.toBytes((Integer) o, byteOrder);
        } else if (o instanceof int[]) {
            final int[] numbers = (int[]) o;
            return ByteConversions.toBytes(numbers, byteOrder);
        } else if (o instanceof Integer[]) {
            final Integer[] numbers = (Integer[]) o;
            final int[] values = new int[numbers.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = numbers[i];
            }
            return ByteConversions.toBytes(values, byteOrder);
        } else if (o instanceof long[]) {
            final long[] numbers = (long[]) o;
            return ByteConversions.toBytes(numbers, byteOrder);
        } else if (o instanceof Long[]) {
            final Long[] numbers = (Long[]) o;
            final long[] values = new long[numbers.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = numbers[i];
            }
            return ByteConversions.toBytes(values, byteOrder);
        } else {
            throw new ImageWriteException("Invalid data", o);
        }
    }

}
