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
import com.maxar.rda.imaging.formats.tiff.constants.TiffConstants;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.List;

public class TiffImageWriterLossy extends TiffImageWriterBase {

    public TiffImageWriterLossy() {
        // with default byte order
    }

    public TiffImageWriterLossy(final ByteOrder byteOrder) {
        super(byteOrder);
    }

    @Override
    public void write(final OutputStream os, final TiffOutputSet outputSet)
            throws IOException, ImageWriteException
    {
        final TiffOutputSummary outputSummary = validateDirectories(outputSet);

        final List<TiffOutputItem> outputItems = outputSet.getOutputItems(outputSummary);

        updateOffsetsStep(outputItems, outputSet.version());

        outputSummary.updateOffsets(byteOrder);

        final BinaryOutputStream bos = new BinaryOutputStream(os, byteOrder);

        // NB: resource is intentionally left open
        writeStep(bos, outputItems, outputSet.version(), true);
    }

    public void write(final OutputStream os, final TiffOutputSet outputSet, boolean writeData)
            throws IOException, ImageWriteException {
        final TiffOutputSummary outputSummary = validateDirectories(outputSet);

        final List<TiffOutputItem> outputItems = outputSet.getOutputItems(outputSummary);

        updateOffsetsStep(outputItems, outputSet.version());

        outputSummary.updateOffsets(byteOrder);

        final BinaryOutputStream bos = new BinaryOutputStream(os, byteOrder);

        // NB: resource is intentionally left open
        writeStep(bos, outputItems, outputSet.version(), writeData);
    }


    private void updateOffsetsStep(final List<TiffOutputItem> outputItems, int version) {
        int initialOffset = version == TiffConstants.TIFF_CLASSIC ? TiffConstants.TIFF_HEADER_SIZE : TiffConstants.BIG_TIFF_HEADER_SIZE;
        BigInteger offset = BigInteger.valueOf(initialOffset);
        try
        {
            if(version == TiffConstants.TIFF_CLASSIC)
            {
                for (final TiffOutputItem outputItem : outputItems) {
                    outputItem.setOffset(offset.intValueExact());
                    final int itemLength = outputItem.getItemLength();
                    offset = offset.add(BigInteger.valueOf(itemLength));

                    final int remainder = imageDataPaddingLength(itemLength);
                    offset = offset.add(BigInteger.valueOf(remainder));
                }
            }
            else
            {
                for (final TiffOutputItem outputItem : outputItems) {
                    outputItem.setOffset(offset.longValueExact());
                    final long itemLength = outputItem.getItemLength();
                    offset = offset.add(BigInteger.valueOf(itemLength));

                    final long remainder = imageDataPaddingLength(itemLength);
                    offset = offset.add(BigInteger.valueOf(remainder));
                }
            }
        }
        catch(ArithmeticException e)
        {
            throw new RuntimeException(String.format("TIFF offsets out of range of %s. Error: (%s) %s",
                    version == TiffConstants.TIFF_CLASSIC ? "ClassicTIFF" : "BigTIFF", offset.toString(), e.getMessage()));
        }


    }

    private void writeStep(final BinaryOutputStream bos,
            final List<TiffOutputItem> outputItems, int version, boolean writeData) throws IOException,
            ImageWriteException {
        writeImageFileHeader(bos, version);

        for (final TiffOutputItem outputItem : outputItems) {
            if(outputItem.getItemDescription().equalsIgnoreCase("TIFF image data") && !writeData) continue;
            outputItem.writeItem(bos);

            final long length = outputItem.getItemLength();

            final int remainder;
            if(version == TiffConstants.TIFF_CLASSIC)
                remainder = imageDataPaddingLength( (int)length);
            else
                remainder = (int)imageDataPaddingLength(length);
            for (int j = 0; j < remainder; j++) {
                bos.write(0);
            }
        }

    }
}
