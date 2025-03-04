/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package com.maxar.rda.imaging.formats.jpeg.segments;

import com.maxar.rda.imaging.ImageReadException;
import com.maxar.rda.imaging.common.BinaryFunctions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DqtSegment extends Segment {
    public final List<QuantizationTable> quantizationTables = new ArrayList<>();

    public static class QuantizationTable {
        public final int precision;
        public final int destinationIdentifier;
        private final int[] elements;

        public QuantizationTable(final int precision, final int destinationIdentifier,
                final int[] elements) {
            this.precision = precision;
            this.destinationIdentifier = destinationIdentifier;
            this.elements = elements;
        }

        /**
         * @return the elements
         */
        public int[] getElements() {
            return elements;
        }
    }

    public DqtSegment(final int marker, final byte[] segmentData)
            throws ImageReadException, IOException {
        this(marker, segmentData.length, new ByteArrayInputStream(segmentData));
    }

    public DqtSegment(final int marker, int length, final InputStream is)
            throws ImageReadException, IOException {
        super(marker, length);

        while (length > 0) {
            final int precisionAndDestination = BinaryFunctions.readByte(
                    "QuantizationTablePrecisionAndDestination", is,
                    "Not a Valid JPEG File");
            length--;
            final int precision = (precisionAndDestination >> 4) & 0xf;
            final int destinationIdentifier = precisionAndDestination & 0xf;

            final int[] elements = new int[64];
            for (int i = 0; i < 64; i++) {
                if (precision == 0) {
                    elements[i] = 0xff & BinaryFunctions.readByte("QuantizationTableElement",
                            is, "Not a Valid JPEG File");
                    length--;
                } else if (precision == 1) {
                    elements[i] = BinaryFunctions.read2Bytes("QuantizationTableElement", is, "Not a Valid JPEG File", getByteOrder());
                    length -= 2;
                } else {
                    throw new ImageReadException(
                            "Quantization table precision '" + precision
                                    + "' is invalid");
                }
            }

            quantizationTables.add(new QuantizationTable(precision,
                    destinationIdentifier, elements));
        }
    }

    @Override
    public String getDescription() {
        return "DQT (" + getSegmentType() + ")";
    }
}
