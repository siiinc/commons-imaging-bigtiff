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
package com.maxar.rda.imaging.formats.jpeg.segments;

import static com.maxar.rda.imaging.common.BinaryFunctions.readBytes;
import static com.maxar.rda.imaging.common.BinaryFunctions.skipBytes;

import com.maxar.rda.imaging.ImageReadException;
import com.maxar.rda.imaging.common.BinaryFunctions;
import com.maxar.rda.imaging.formats.jpeg.JpegConstants;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class JfifSegment extends Segment {
    public final int jfifMajorVersion;
    public final int jfifMinorVersion;
    public final int densityUnits;
    public final int xDensity;
    public final int yDensity;

    public final int xThumbnail;
    public final int yThumbnail;
    public final int thumbnailSize;

    @Override
    public String getDescription() {
        return "JFIF (" + getSegmentType() + ")";
    }

    public JfifSegment(final int marker, final byte[] segmentData)
            throws ImageReadException, IOException {
        this(marker, segmentData.length, new ByteArrayInputStream(segmentData));
    }

    public JfifSegment(final int marker, final int markerLength, final InputStream is)
            throws ImageReadException, IOException {
        super(marker, markerLength);

        final byte[] signature = BinaryFunctions.readBytes(is, JpegConstants.JFIF0_SIGNATURE.size());
        if (!JpegConstants.JFIF0_SIGNATURE.equals(signature)
                && !JpegConstants.JFIF0_SIGNATURE_ALTERNATIVE.equals(signature)) {
            throw new ImageReadException(
                    "Not a Valid JPEG File: missing JFIF string");
        }

        jfifMajorVersion = BinaryFunctions.readByte("JFIF_major_version", is,
                "Not a Valid JPEG File");
        jfifMinorVersion = BinaryFunctions.readByte("JFIF_minor_version", is,
                "Not a Valid JPEG File");
        densityUnits = BinaryFunctions.readByte("density_units", is, "Not a Valid JPEG File");
        xDensity = BinaryFunctions.read2Bytes("x_density", is, "Not a Valid JPEG File", getByteOrder());
        yDensity = BinaryFunctions.read2Bytes("y_density", is, "Not a Valid JPEG File", getByteOrder());

        xThumbnail = BinaryFunctions.readByte("x_thumbnail", is, "Not a Valid JPEG File");
        yThumbnail = BinaryFunctions.readByte("y_thumbnail", is, "Not a Valid JPEG File");
        thumbnailSize = xThumbnail * yThumbnail;
        if (thumbnailSize > 0) {
            BinaryFunctions.skipBytes(is, thumbnailSize,
                    "Not a Valid JPEG File: missing thumbnail");

        }
    }

}
