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
package com.maxar.rda.imaging.formats.tiff.write;

import com.maxar.rda.imaging.ImageWriteException;
import com.maxar.rda.imaging.common.BinaryOutputStream;
import com.maxar.rda.imaging.common.RationalNumber;
import com.maxar.rda.imaging.formats.tiff.JpegImageData;
import com.maxar.rda.imaging.formats.tiff.TiffDirectory;
import com.maxar.rda.imaging.formats.tiff.TiffElement;
import com.maxar.rda.imaging.formats.tiff.TiffImageData;
import com.maxar.rda.imaging.formats.tiff.constants.TiffConstants;
import com.maxar.rda.imaging.formats.tiff.constants.TiffDirectoryType;
import com.maxar.rda.imaging.formats.tiff.constants.TiffTagConstants;
import com.maxar.rda.imaging.formats.tiff.fieldtypes.FieldType;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfo;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoAscii;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoAsciiOrByte;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoAsciiOrRational;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoByte;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoByteOrShort;
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
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoShortOrLongOrRational;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoShortOrRational;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoShorts;
import com.maxar.rda.imaging.formats.tiff.taginfos.TagInfoXpString;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.maxar.rda.imaging.formats.tiff.taginfos.*;


public final class TiffOutputDirectory extends TiffOutputItem {
    public final int type;
    private final List<TiffOutputField> fields = new ArrayList<>();
    private final ByteOrder byteOrder;
    private TiffOutputDirectory nextDirectory;

    public static final Comparator<TiffOutputDirectory> COMPARATOR = (o1, o2) -> {
        if (o1.type < o2.type) {
            return -1;
        } else if (o1.type > o2.type) {
            return 1;
        } else {
            return 0;
        }
    };
    private JpegImageData jpegImageData;
    private TiffImageData tiffImageData;

    public void setNextDirectory(final TiffOutputDirectory nextDirectory) {
        this.nextDirectory = nextDirectory;
    }

    public TiffOutputDirectory(final int type, final ByteOrder byteOrder) {
        this.type = type;
        this.byteOrder = byteOrder;
        this.version = TiffConstants.TIFF_CLASSIC;
    }

    public TiffOutputDirectory(final int type, final int version, final ByteOrder byteOrder) {
        this.type = type;
        this.byteOrder = byteOrder;
        this.version = version;
    }

    public int getVersion() {
        return this.version;
    }

    public void add(final TagInfoByte tagInfo, final byte value)
            throws ImageWriteException
    {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.BYTE, bytes.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoBytes tagInfo, final byte... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.BYTE, values.length,
                bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoAscii tagInfo, final String... values)
            throws ImageWriteException {
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        if (tagInfo.length > 0 && tagInfo.length != bytes.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " byte(s), not " + values.length);
        }
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.ASCII, bytes.length,
                bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoShort tagInfo, final short value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SHORT, 1, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoShorts tagInfo, final short... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SHORT,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoLong tagInfo, final int value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.LONG, 1, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoLongs tagInfo, final int... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.LONG, values.length,
                bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoRational tagInfo, final RationalNumber value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.RATIONAL, 1, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoRationals tagInfo, final RationalNumber... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.RATIONAL,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoSByte tagInfo, final byte value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SBYTE, 1, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoSBytes tagInfo, final byte... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SBYTE,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoSShort tagInfo, final short value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SSHORT, 1, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoSShorts tagInfo, final short... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SSHORT,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoSLong tagInfo, final int value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SLONG, 1, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoSLongs tagInfo, final int... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SLONG,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoSRational tagInfo, final RationalNumber value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SRATIONAL, 1, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoSRationals tagInfo, final RationalNumber... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SRATIONAL,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoFloat tagInfo, final float value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.FLOAT, 1, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoFloats tagInfo, final float... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.FLOAT,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoDouble tagInfo, final double value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.DOUBLE, 1, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoDoubles tagInfo, final double... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.DOUBLE,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoByteOrShort tagInfo, final byte... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.BYTE, values.length,
                bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoByteOrShort tagInfo, final short... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SHORT,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoShortOrLong tagInfo, final short... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SHORT,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoShortOrLong tagInfo, final int... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.LONG, values.length,
                bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoShortOrLongOrRational tagInfo, final short... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SHORT,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoShortOrLongOrRational tagInfo, final int... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.LONG, values.length,
                bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoShortOrLongOrRational tagInfo,
            final RationalNumber... values) throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.RATIONAL,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoShortOrRational tagInfo, final short... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.SHORT,
                values.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoShortOrRational tagInfo, final RationalNumber... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.RATIONAL,
                values.length, bytes);
        add(tiffOutputField);
    }

    /*
        added bigtiff support
     */
    public void add(final TagInfoLong8 tagInfo, final long value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.LONG8, 1, bytes);
        add(tiffOutputField);

        this.version = TiffConstants.TIFF_BIGTIFF;
    }

    public void add(final TagInfoLong8s tagInfo, final long... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, values);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.LONG8, values.length,
                bytes);
        add(tiffOutputField);

        this.version = TiffConstants.TIFF_BIGTIFF;
    }

    public void add(final TagInfoLong8OrIFD8 tagInfo, final long value)
            throws ImageWriteException {
        if (tagInfo.length != 1) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not 1");
        }
        final byte[] bytes = tagInfo.encodeValue(byteOrder, value);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.LONG8, 1, bytes);
        add(tiffOutputField);

        this.version = TiffConstants.TIFF_BIGTIFF;
    }


    public void add(final TagInfoGpsText tagInfo, final String value)
            throws ImageWriteException {
        final byte[] bytes = tagInfo.encodeValue(
                FieldType.UNDEFINED, value, byteOrder);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, tagInfo.dataTypes.get(0), bytes.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoXpString tagInfo, final String value)
            throws ImageWriteException {
        final byte[] bytes = tagInfo.encodeValue(
                FieldType.BYTE, value, byteOrder);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.BYTE, bytes.length,
                bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoAsciiOrByte tagInfo, final String... values)
            throws ImageWriteException {
        final byte[] bytes = tagInfo.encodeValue(
                FieldType.ASCII, values, byteOrder);
        if (tagInfo.length > 0 && tagInfo.length != bytes.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " byte(s), not " + values.length);
        }
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.ASCII, bytes.length,
                bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoAsciiOrRational tagInfo, final String... values)
            throws ImageWriteException {
        final byte[] bytes = tagInfo.encodeValue(
                FieldType.ASCII, values, byteOrder);
        if (tagInfo.length > 0 && tagInfo.length != bytes.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " byte(s), not " + values.length);
        }
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.ASCII, bytes.length,
                bytes);
        add(tiffOutputField);
    }

    public void add(final TagInfoAsciiOrRational tagInfo, final RationalNumber... values)
            throws ImageWriteException {
        if (tagInfo.length > 0 && tagInfo.length != values.length) {
            throw new ImageWriteException("Tag expects " + tagInfo.length
                    + " value(s), not " + values.length);
        }
        final byte[] bytes = tagInfo.encodeValue(
                FieldType.RATIONAL, values, byteOrder);
        final TiffOutputField tiffOutputField = new TiffOutputField(tagInfo.tag,
                tagInfo, FieldType.RATIONAL,
                bytes.length, bytes);
        add(tiffOutputField);
    }

    public void add(final TiffOutputField field) {
        fields.add(field);
    }

    public List<TiffOutputField> getFields() {
        return new ArrayList<>(fields);
    }

    public void removeField(final TagInfo tagInfo) {
        removeField(tagInfo.tag);
    }

    public void removeField(final int tag) {
        final List<TiffOutputField> matches = new ArrayList<>();
        for (final TiffOutputField field : fields) {
            if (field.tag == tag) {
                matches.add(field);
            }
        }
        fields.removeAll(matches);
    }

    /**
     * Finds the TiffOutputField for the given TagInfo from this TiffOutputDirectory.
     *
     * <p>
     * If there is no field matching the given TagInfo, null will be returned.
     * </p>
     *
     * @param tagInfo the TagInfo specifying the field
     * @return the field matching tagInfo or null, if the field isn't present
     * @see #findField(int)
     */
    public TiffOutputField findField(final TagInfo tagInfo) {
        return findField(tagInfo.tag);
    }

    /**
     * Finds the TiffOutputField for the given tag from this TiffOutputDirectory.
     *
     * <p>
     * If there is no field matching the given tag, null will be returned.
     * </p>
     *
     * @param tag the tag specifying the field
     * @return the field matching tagInfo or null, if the field isn't present
     * @see #findField(TagInfo)
     */
    public TiffOutputField findField(final int tag) {
        for (final TiffOutputField field : fields) {
            if (field.tag == tag) {
                return field;
            }
        }
        return null;
    }

    public void sortFields() {
        final Comparator<TiffOutputField> comparator = (e1, e2) -> {
            if (e1.tag != e2.tag) {
                return e1.tag - e2.tag;
            }
            return e1.getSortHint() - e2.getSortHint();
        };
        Collections.sort(fields, comparator);
    }

    public String description() {
        return TiffDirectory.description(type);
    }

    @Override
    public void writeItem(final BinaryOutputStream bos) throws IOException,
            ImageWriteException {

        // Write Directory Field Count
        if(this.version == TiffConstants.TIFF_CLASSIC) {
            bos.write2Bytes(fields.size()); // DirectoryFieldCount
        }
        else
        {
            bos.write8Bytes(fields.size()); // DirectoryFieldCount
        }

        // Write Fields
        for (final TiffOutputField field : fields) {
            field.writeField(bos, this.getVersion());

//             Debug.debug("\t" + "writing field (" + field.tag + ", 0x" +
//             Integer.toHexString(field.tag) + ")", field.tagInfo);
//             if(field.tagInfo.isOffset())
//             Debug.debug("\t\tOFFSET!", field.bytes);
        }

        long nextDirectoryOffset = 0;
        if (nextDirectory != null) {
            nextDirectoryOffset = nextDirectory.getOffset();
        }

        if(this.version == TiffConstants.TIFF_CLASSIC)
        {
            // Write nextDirectoryOffset
            if (nextDirectoryOffset == UNDEFINED_VALUE) {
                bos.write4Bytes(0);
            } else {
                bos.write4Bytes((int) nextDirectoryOffset);
            }
        }
        else
        {
            // Write nextDirectoryOffset
            if (nextDirectoryOffset == UNDEFINED_VALUE) {
                bos.write8Bytes(0);
            } else {
                bos.write8Bytes(nextDirectoryOffset);
            }
        }
    }

    public void setJpegImageData(final JpegImageData rawJpegImageData) {
        this.jpegImageData = rawJpegImageData;
    }

    public JpegImageData getRawJpegImageData() {
        return jpegImageData;
    }

    public void setTiffImageData(final TiffImageData rawTiffImageData) {
        this.tiffImageData = rawTiffImageData;
    }

    public TiffImageData getRawTiffImageData() {
        return tiffImageData;
    }

    @Override
    public int getItemLength() {
        if(this.version == TiffConstants.TIFF_CLASSIC) {
            return TiffConstants.TIFF_ENTRY_LENGTH * fields.size() + TiffConstants.TIFF_DIRECTORY_HEADER_LENGTH
                    + TiffConstants.TIFF_DIRECTORY_FOOTER_LENGTH;
        }
        else
        {
            return TiffConstants.BIG_TIFF_ENTRY_LENGTH * fields.size() + TiffConstants.BIG_TIFF_DIRECTORY_HEADER_LENGTH
                    + TiffConstants.BIG_TIFF_DIRECTORY_FOOTER_LENGTH;
        }
    }

    @Override
    public String getItemDescription() {
        final TiffDirectoryType dirType = TiffDirectoryType.getExifDirectoryType(type);
        return "Directory: " + dirType.name + " (" + type + ")";
    }

    private void removeFieldIfPresent(final TagInfo tagInfo) {
        final TiffOutputField field = findField(tagInfo);
        if (null != field) {
            fields.remove(field);
        }
    }

    protected List<TiffOutputItem> getOutputItems(
            final TiffOutputSummary outputSummary) throws ImageWriteException {
        // first validate directory fields.

        removeFieldIfPresent(TiffTagConstants.TIFF_TAG_JPEG_INTERCHANGE_FORMAT);
        removeFieldIfPresent(TiffTagConstants.TIFF_TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);

        //todo: figure out if this needs bigtiff treatment
        TiffOutputField jpegOffsetField = null;
        if (null != jpegImageData) {
            jpegOffsetField = new TiffOutputField(
                    TiffTagConstants.TIFF_TAG_JPEG_INTERCHANGE_FORMAT,
                    FieldType.LONG, 1, new byte[TiffConstants.TIFF_ENTRY_MAX_VALUE_LENGTH]);
            add(jpegOffsetField);

            final byte[] lengthValue = FieldType.LONG.writeData(
                    jpegImageData.length,
                    outputSummary.byteOrder);

            final TiffOutputField jpegLengthField = new TiffOutputField(
                    TiffTagConstants.TIFF_TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
                    FieldType.LONG, 1, lengthValue);
            add(jpegLengthField);

        }

        // --------------------------------------------------------------

        removeFieldIfPresent(TiffTagConstants.TIFF_TAG_STRIP_OFFSETS);
        removeFieldIfPresent(TiffTagConstants.TIFF_TAG_STRIP_BYTE_COUNTS);
        removeFieldIfPresent(TiffTagConstants.TIFF_TAG_TILE_OFFSETS);
        removeFieldIfPresent(TiffTagConstants.TIFF_TAG_TILE_BYTE_COUNTS);

        TiffOutputField imageDataOffsetField;
        ImageDataOffsets imageDataInfo = null;
        if (null != tiffImageData) {
            final boolean stripsNotTiles = tiffImageData.stripsNotTiles();

            TagInfo offsetTag;
            TagInfo byteCountsTag;
            if (stripsNotTiles) {
                //todo: handle strips
                offsetTag = TiffTagConstants.TIFF_TAG_STRIP_OFFSETS;
                byteCountsTag = TiffTagConstants.TIFF_TAG_STRIP_BYTE_COUNTS;
            } else {
                offsetTag = TiffTagConstants.TIFF_TAG_TILE_OFFSETS;
                byteCountsTag = TiffTagConstants.TIFF_TAG_TILE_BYTE_COUNTS;
            }

            // --------

            final TiffElement.DataElement[] imageData = tiffImageData.getImageData();

            // TiffOutputField imageDataOffsetsField = null;

            if(this.version == TiffConstants.TIFF_CLASSIC)
            {
                final int[] imageDataOffsets = new int[imageData.length];
                final int[] imageDataByteCounts = new int[imageData.length];
                for (int i = 0; i < imageData.length; i++) {
                    imageDataByteCounts[i] = imageData[i].length;
                }

                // --------

                // Append imageData-related fields to first directory
                imageDataOffsetField = new TiffOutputField(offsetTag,
                        FieldType.LONG, imageDataOffsets.length,
                        FieldType.LONG.writeData(imageDataOffsets,
                                outputSummary.byteOrder));
                add(imageDataOffsetField);

                // --------

                final byte[] data = FieldType.LONG.writeData(imageDataByteCounts, outputSummary.byteOrder);
                final TiffOutputField byteCountsField = new TiffOutputField(
                        byteCountsTag, FieldType.LONG, imageDataByteCounts.length,
                        data);
                add(byteCountsField);

                // --------

                imageDataInfo = new ImageDataOffsets(imageData, imageDataOffsets, imageDataOffsetField);
            }
            else
            {
                final long[] imageDataOffsets = new long[imageData.length];
                final long[] imageDataByteCounts = new long[imageData.length];
                for (int i = 0; i < imageData.length; i++) {
                    imageDataByteCounts[i] = imageData[i].length;
                }

                // --------

                // Append imageData-related fields to first directory
                imageDataOffsetField = new TiffOutputField(offsetTag,
                        FieldType.LONG8, imageDataOffsets.length,
                        FieldType.LONG8.writeData(imageDataOffsets,
                                outputSummary.byteOrder));
                add(imageDataOffsetField);

                // --------

                final byte[] data = FieldType.LONG8.writeData(imageDataByteCounts, outputSummary.byteOrder);
                final TiffOutputField byteCountsField = new TiffOutputField(
                        byteCountsTag, FieldType.LONG8, imageDataByteCounts.length,
                        data);
                add(byteCountsField);

                // --------

                imageDataInfo = new ImageDataOffsets(imageData, imageDataOffsets, imageDataOffsetField);
            }

        }

        // --------------------------------------------------------------

        final List<TiffOutputItem> result = new ArrayList<>();
        result.add(this);
        sortFields();

        for (final TiffOutputField field : fields) {
            if (field.isLocalValue()) {
                continue;
            }

            final TiffOutputItem item = field.getSeperateValue();
            result.add(item);
            // outputSummary.add(item, field);
        }

        if (null != imageDataInfo) {
            Collections.addAll(result, imageDataInfo.outputItems);

            outputSummary.addTiffImageData(imageDataInfo);
        }

        if (null != jpegImageData) {
            final TiffOutputItem item = new TiffOutputItem.Value("JPEG image data",
                    jpegImageData.getData());
            result.add(item);
            outputSummary.add(item, jpegOffsetField);
        }

        return result;
    }
}
