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
package com.maxar.rda.imaging.formats.tiff.taginfos;

import com.maxar.rda.imaging.common.ByteConversions;
import com.maxar.rda.imaging.formats.tiff.constants.TiffDirectoryType;
import com.maxar.rda.imaging.formats.tiff.fieldtypes.FieldType;

import java.nio.ByteOrder;

public class TagInfoShortOrLongOrLong8 extends TagInfo {
    public TagInfoShortOrLongOrLong8(final String name, final int tag, final int length, final TiffDirectoryType directoryType) {
        super(name, tag, FieldType.SHORT_OR_LONG, length, directoryType, false);
    }

    public TagInfoShortOrLongOrLong8(final String name, final int tag, final int length, final TiffDirectoryType directoryType, final boolean isOffset) {
        super(name, tag, FieldType.SHORT_OR_LONG, length, directoryType, isOffset);
    }

    public byte[] encodeValue(final ByteOrder byteOrder, final short... values) {
        return ByteConversions.toBytes(values, byteOrder);
    }

    public byte[] encodeValue(final ByteOrder byteOrder, final int... values) {
        return ByteConversions.toBytes(values, byteOrder);
    }

    public byte[] encodeValue(final ByteOrder byteOrder, final long... values) {
        return ByteConversions.toBytes(values, byteOrder);
    }
}
