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
package com.maxar.rda.imaging.formats.tiff;

import com.maxar.rda.imaging.ImageReadException;
import com.maxar.rda.imaging.common.ByteConversions;
import com.maxar.rda.imaging.common.RationalNumber;
import com.maxar.rda.imaging.formats.tiff.constants.TiffConstants;
import com.maxar.rda.imaging.formats.tiff.constants.TiffDirectoryConstants;
import com.maxar.rda.imaging.formats.tiff.constants.TiffTagConstants;
import com.maxar.rda.imaging.formats.tiff.fieldtypes.FieldType;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfo;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoAscii;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoByte;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoBytes;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoDouble;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoDoubles;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoFloat;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoFloats;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoGpsText;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoLong;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoLong8;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoLong8OrIFD8;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoLong8s;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoLongs;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoRational;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoRationals;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoSByte;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoSBytes;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoSLong;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoSLongs;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoSRational;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoSRationals;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoSShort;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoSShorts;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoShort;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoShortOrLong;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoShorts;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoXpString;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.maxar.rda.imaging.formats.tiff.taginfos.*;

public class TiffDirectory extends TiffElement {
    public final int type;
    public final List<TiffField> entries;
    public final long nextDirectoryOffset;
    private TiffImageData tiffImageData;
    private JpegImageData jpegImageData;

    public TiffDirectory(final int type, final List<TiffField> entries, final long offset, final long nextDirectoryOffset) {
        super(offset, TiffConstants.TIFF_DIRECTORY_HEADER_LENGTH
                + entries.size() * TiffConstants.TIFF_ENTRY_LENGTH
                + TiffConstants.TIFF_DIRECTORY_FOOTER_LENGTH);

        this.type = type;
        this.entries = Collections.unmodifiableList(entries);
        this.nextDirectoryOffset = nextDirectoryOffset;
    }

    public String description() {
        return TiffDirectory.description(type);
    }

    @Override
    public String getElementDescription() {
        long entryOffset = offset + TiffConstants.TIFF_DIRECTORY_HEADER_LENGTH;

        final StringBuilder result = new StringBuilder();
        for (final TiffField entry : entries) {
            result.append(String.format("\t[%d]: %s (%d, 0x%x), %s, %d: %s%n",
                    entryOffset, entry.getTagInfo().name,
                    entry.getTag(), entry.getTag(),
                    entry.getFieldType().getName(), entry.getBytesLength(),
                    entry.getValueDescription()));

            entryOffset += TiffConstants.TIFF_ENTRY_LENGTH;
        }
        return result.toString();
    }

    public static String description(final int type) {
        switch (type) {
        case TiffDirectoryConstants.DIRECTORY_TYPE_UNKNOWN:
            return "Unknown";
        case TiffDirectoryConstants.DIRECTORY_TYPE_ROOT:
            return "Root";
        case TiffDirectoryConstants.DIRECTORY_TYPE_SUB:
            return "Sub";
        case TiffDirectoryConstants.DIRECTORY_TYPE_THUMBNAIL:
            return "Thumbnail";
        case TiffDirectoryConstants.DIRECTORY_TYPE_EXIF:
            return "Exif";
        case TiffDirectoryConstants.DIRECTORY_TYPE_GPS:
            return "Gps";
        case TiffDirectoryConstants.DIRECTORY_TYPE_INTEROPERABILITY:
            return "Interoperability";
        default:
            return "Bad Type";
        }
    }


    public List<TiffField> getDirectoryEntries() {
        return new ArrayList<>(entries);
    }

    public void dump() {
        for (final TiffField entry : entries) {
            entry.dump();
        }

    }

    public boolean hasJpegImageData() throws ImageReadException
    {
        if (null != findField(TiffTagConstants.TIFF_TAG_JPEG_INTERCHANGE_FORMAT)) {
            return true;
        }

        return false;
    }

    public boolean hasTiffImageData() throws ImageReadException {
        if (null != findField(TiffTagConstants.TIFF_TAG_TILE_OFFSETS)) {
            return true;
        }

        if (null != findField(TiffTagConstants.TIFF_TAG_STRIP_OFFSETS)) {
            return true;
        }

        return false;
    }

    public BufferedImage getTiffImage(final ByteOrder byteOrder) throws ImageReadException,
            IOException {
        final Map<String, Object> params = null;
        return getTiffImage(byteOrder, params);
    }

    public BufferedImage getTiffImage(final ByteOrder byteOrder, final Map<String, Object> params)
            throws ImageReadException, IOException {
        if (null == tiffImageData) {
            return null;
        }

        return new TiffImageParser().getBufferedImage(this, byteOrder, params);
    }

    public TiffField findField(final TagInfo tag) throws ImageReadException {
        final boolean failIfMissing = false;
        return findField(tag, failIfMissing);
    }

    public TiffField findField(final TagInfo tag, final boolean failIfMissing)
            throws ImageReadException {
        if (entries == null) {
            return null;
        }

        for (final TiffField field : entries) {
            if (field.getTag() == tag.tag) {
                return field;
            }
        }

        if (failIfMissing) {
            throw new ImageReadException("Missing expected field: "
                    + tag.getDescription());
        }

        return null;
    }

    public Object getFieldValue(final TagInfo tag) throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            return null;
        }
        return field.getValue();
    }

    public String getSingleFieldValue(final TagInfoAscii tag)
            throws ImageReadException {
        final String[] result = getFieldValue(tag, true);
        if (result.length != 1) {
            throw new ImageReadException("Field \"" + tag.name
                    + "\" has incorrect length " + result.length);
        }
        return result[0];
    }

    public int getSingleFieldValue(final TagInfoShortOrLong tag) throws ImageReadException {
        final int[] result = getFieldValue(tag, true);
        if (result.length != 1) {
            throw new ImageReadException("Field \"" + tag.name
                    + "\" has incorrect length " + result.length);
        }
        return result[0];
    }

    public byte getFieldValue(final TagInfoByte tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        return field.getByteArrayValue()[0];
    }

    public byte[] getFieldValue(final TagInfoBytes tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        return field.getByteArrayValue();
    }

    public String[] getFieldValue(final TagInfoAscii tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public short getFieldValue(final TagInfoShort tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public short[] getFieldValue(final TagInfoShorts tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public int getFieldValue(final TagInfoLong tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public int[] getFieldValue(final TagInfoLongs tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public int[] getFieldValue(final TagInfoShortOrLong tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        if (field.getFieldType() == FieldType.SHORT) {
            return ByteConversions.toUInt16s(bytes, field.getByteOrder());
        } else {
            return ByteConversions.toInts(bytes, field.getByteOrder());
        }
    }

    public RationalNumber getFieldValue(final TagInfoRational tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public RationalNumber[] getFieldValue(final TagInfoRationals tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public byte getFieldValue(final TagInfoSByte tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        return field.getByteArrayValue()[0];
    }

    public byte[] getFieldValue(final TagInfoSBytes tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        return field.getByteArrayValue();
    }

    public short getFieldValue(final TagInfoSShort tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public short[] getFieldValue(final TagInfoSShorts tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public int getFieldValue(final TagInfoSLong tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public int[] getFieldValue(final TagInfoSLongs tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public RationalNumber getFieldValue(final TagInfoSRational tag) throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public RationalNumber[] getFieldValue(final TagInfoSRationals tag,
            final boolean mustExist) throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public float getFieldValue(final TagInfoFloat tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public float[] getFieldValue(final TagInfoFloats tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public double getFieldValue(final TagInfoDouble tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public double[] getFieldValue(final TagInfoDoubles tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public String getFieldValue(final TagInfoGpsText tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        return tag.getValue(field);
    }

    public String getFieldValue(final TagInfoXpString tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        return tag.getValue(field);
    }

    public long getFieldValue(final TagInfoLong8 tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public long[] getFieldValue(final TagInfoLong8s tag, final boolean mustExist)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" is missing");
            } else {
                return null;
            }
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            if (mustExist) {
                throw new ImageReadException("Required field \"" + tag.name
                        + "\" has incorrect type " + field.getFieldType().getName());
            } else {
                return null;
            }
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public long getFieldValue(final TagInfoLong8OrIFD8 tag)
            throws ImageReadException {
        final TiffField field = findField(tag);
        if (field == null) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" is missing");
        }
        if (!tag.dataTypes.contains(field.getFieldType())) {
            throw new ImageReadException("Required field \"" + tag.name
                    + "\" has incorrect type " + field.getFieldType().getName());
        }
        if (field.getCount() != 1) {
            throw new ImageReadException("Field \"" + tag.name + "\" has wrong count " + field.getCount());
        }
        final byte[] bytes = field.getByteArrayValue();
        return tag.getValue(field.getByteOrder(), bytes);
    }

    public static final class ImageDataElement extends TiffElement {
        public ImageDataElement(final long offset, final int length) {
            super(offset, length);
        }

        @Override
        public String getElementDescription() {
            return "ImageDataElement";
        }
    }

    private List<ImageDataElement> getRawImageDataElements(
            final TiffField offsetsField, final TiffField byteCountsField)
            throws ImageReadException {
        final int[] offsets = offsetsField.getIntArrayValue();
        final int[] byteCounts = byteCountsField.getIntArrayValue();

        if (offsets.length != byteCounts.length) {
            throw new ImageReadException("offsets.length(" + offsets.length
                    + ") != byteCounts.length(" + byteCounts.length + ")");
        }

        final List<ImageDataElement> result = new ArrayList<>(offsets.length);
        for (int i = 0; i < offsets.length; i++) {
            result.add(new ImageDataElement(offsets[i], byteCounts[i]));
        }
        return result;
    }

    public List<ImageDataElement> getTiffRawImageDataElements()
            throws ImageReadException {
        final TiffField tileOffsets = findField(TiffTagConstants.TIFF_TAG_TILE_OFFSETS);
        final TiffField tileByteCounts = findField(TiffTagConstants.TIFF_TAG_TILE_BYTE_COUNTS);
        final TiffField stripOffsets = findField(TiffTagConstants.TIFF_TAG_STRIP_OFFSETS);
        final TiffField stripByteCounts = findField(TiffTagConstants.TIFF_TAG_STRIP_BYTE_COUNTS);

        if ((tileOffsets != null) && (tileByteCounts != null)) {
            return getRawImageDataElements(tileOffsets, tileByteCounts);
        } else if ((stripOffsets != null) && (stripByteCounts != null)) {
            return getRawImageDataElements(stripOffsets, stripByteCounts);
        } else {
            throw new ImageReadException("Couldn't find image data.");
        }
    }

    public boolean imageDataInStrips() throws ImageReadException {
        final TiffField tileOffsets = findField(TiffTagConstants.TIFF_TAG_TILE_OFFSETS);
        final TiffField tileByteCounts = findField(TiffTagConstants.TIFF_TAG_TILE_BYTE_COUNTS);
        final TiffField stripOffsets = findField(TiffTagConstants.TIFF_TAG_STRIP_OFFSETS);
        final TiffField stripByteCounts = findField(TiffTagConstants.TIFF_TAG_STRIP_BYTE_COUNTS);

        if ((tileOffsets != null) && (tileByteCounts != null)) {
            return false;
        } else if ((stripOffsets != null) && (stripByteCounts != null)) {
            return true;
        } else {
            throw new ImageReadException("Couldn't find image data.");
        }
    }

    public ImageDataElement getJpegRawImageDataElement() throws ImageReadException {
        final TiffField jpegInterchangeFormat = findField(TiffTagConstants.TIFF_TAG_JPEG_INTERCHANGE_FORMAT);
        final TiffField jpegInterchangeFormatLength = findField(TiffTagConstants.TIFF_TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);

        if (jpegInterchangeFormat != null && jpegInterchangeFormatLength != null) {
            final int offSet = jpegInterchangeFormat.getIntArrayValue()[0];
            final int byteCount = jpegInterchangeFormatLength.getIntArrayValue()[0];

            return new ImageDataElement(offSet, byteCount);
        } else {
            throw new ImageReadException("Couldn't find image data.");
        }
    }

    public void setTiffImageData(final TiffImageData rawImageData) {
        this.tiffImageData = rawImageData;
    }

    public TiffImageData getTiffImageData() {
        return tiffImageData;
    }

    public void setJpegImageData(final JpegImageData value) {
        this.jpegImageData = value;
    }

    public JpegImageData getJpegImageData() {
        return jpegImageData;
    }

}
