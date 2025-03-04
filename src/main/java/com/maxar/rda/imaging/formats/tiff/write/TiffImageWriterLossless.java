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

import com.maxar.rda.imaging.FormatCompliance;
import com.maxar.rda.imaging.ImageReadException;
import com.maxar.rda.imaging.ImageWriteException;
import com.maxar.rda.imaging.common.BinaryOutputStream;
import com.maxar.rda.imaging.common.bytesource.ByteSource;
import com.maxar.rda.imaging.common.bytesource.ByteSourceArray;
import com.maxar.rda.imaging.formats.tiff.JpegImageData;
import com.maxar.rda.imaging.formats.tiff.TiffContents;
import com.maxar.rda.imaging.formats.tiff.TiffDirectory;
import com.maxar.rda.imaging.formats.tiff.TiffElement;
import com.maxar.rda.imaging.formats.tiff.TiffField;
import com.maxar.rda.imaging.formats.tiff.TiffImageData;
import com.maxar.rda.imaging.formats.tiff.TiffReader;
import com.maxar.rda.imaging.formats.tiff.constants.ExifTagConstants;
import com.maxar.rda.imaging.formats.tiff.constants.TiffConstants;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TiffImageWriterLossless extends TiffImageWriterBase {
    private final byte[] exifBytes;
    private static final Comparator<TiffElement> ELEMENT_SIZE_COMPARATOR = (e1, e2) -> e1.length - e2.length;
    private static final Comparator<TiffOutputItem> ITEM_SIZE_COMPARATOR = (e1, e2) -> e1.getItemLength() - e2.getItemLength();

    public TiffImageWriterLossless(final byte[] exifBytes) {
        this.exifBytes = exifBytes;
    }

    public TiffImageWriterLossless(final ByteOrder byteOrder, final byte[] exifBytes) {
        super(byteOrder);
        this.exifBytes = exifBytes;
    }

    private List<TiffElement> analyzeOldTiff(final Map<Integer, TiffOutputField> frozenFields) throws ImageWriteException,
            IOException {
        try {
            final ByteSource byteSource = new ByteSourceArray(exifBytes);
            final Map<String, Object> params = null;
            final FormatCompliance formatCompliance = FormatCompliance.getDefault();
            final TiffContents contents = new TiffReader(false).readContents(
                    byteSource, params, formatCompliance);

            final List<TiffElement> elements = new ArrayList<>();

            final List<TiffDirectory> directories = contents.directories;
            for (final TiffDirectory directory : directories) {
                elements.add(directory);

                for (final TiffField field : directory.getDirectoryEntries()) {
                    final TiffElement oversizeValue = field.getOversizeValueElement();
                    if (oversizeValue != null) {
                        final TiffOutputField frozenField = frozenFields.get(field.getTag());
                        if (frozenField != null
                                && frozenField.getSeperateValue() != null
                                && frozenField.bytesEqual(field.getByteArrayValue())) {
                            frozenField.getSeperateValue().setOffset(field.getOffset());
                        } else {
                            elements.add(oversizeValue);
                        }
                    }
                }

                final JpegImageData jpegImageData = directory.getJpegImageData();
                if (jpegImageData != null) {
                    elements.add(jpegImageData);
                }

                final TiffImageData tiffImageData = directory.getTiffImageData();
                if (tiffImageData != null) {
                    final TiffElement.DataElement[] data = tiffImageData.getImageData();
                    Collections.addAll(elements, data);
                }
            }

            Collections.sort(elements, TiffElement.COMPARATOR);

            final List<TiffElement> rewritableElements = new ArrayList<>();
            final int TOLERANCE = 3;
            TiffElement start = null;
            long index = -1;
            for (final TiffElement element : elements) {
                final long lastElementByte = element.offset + element.length;
                if (start == null) {
                    start = element;
                    index = lastElementByte;
                } else if (element.offset - index > TOLERANCE) {
                    rewritableElements.add(new TiffElement.Stub(start.offset,
                            (int) (index - start.offset)));
                    start = element;
                    index = lastElementByte;
                } else {
                    index = lastElementByte;
                }
            }
            if (null != start) {
                rewritableElements.add(new TiffElement.Stub(start.offset,
                        (int) (index - start.offset)));
            }

            return rewritableElements;
        } catch (final ImageReadException e) {
            throw new ImageWriteException(e.getMessage(), e);
        }
    }

    @Override
    public void write(final OutputStream os, final TiffOutputSet outputSet)
            throws IOException, ImageWriteException {
        // There are some fields whose address in the file must not change,
        // unless of course their value is changed.
        final Map<Integer, TiffOutputField> frozenFields = new HashMap<>();
        final TiffOutputField makerNoteField = outputSet.findField(ExifTagConstants.EXIF_TAG_MAKER_NOTE);
        if (makerNoteField != null && makerNoteField.getSeperateValue() != null) {
            frozenFields.put(ExifTagConstants.EXIF_TAG_MAKER_NOTE.tag, makerNoteField);
        }
        final List<TiffElement> analysis = analyzeOldTiff(frozenFields);
        final int oldLength = exifBytes.length;
        if (analysis.isEmpty()) {
            throw new ImageWriteException("Couldn't analyze old tiff data.");
        } else if (analysis.size() == 1) {
            final TiffElement onlyElement = analysis.get(0);
            if (onlyElement.offset == TiffConstants.TIFF_HEADER_SIZE
                    && onlyElement.offset + onlyElement.length
                            + TiffConstants.TIFF_HEADER_SIZE == oldLength) {
                // no gaps in old data, safe to complete overwrite.
                new TiffImageWriterLossy(byteOrder).write(os, outputSet);
                return;
            }
        }
        final Map<Long, TiffOutputField> frozenFieldOffsets = new HashMap<>();
        for (final Map.Entry<Integer, TiffOutputField> entry : frozenFields.entrySet()) {
            final TiffOutputField frozenField = entry.getValue();
            if (frozenField.getSeperateValue().getOffset() != TiffOutputItem.UNDEFINED_VALUE) {
                frozenFieldOffsets.put(frozenField.getSeperateValue().getOffset(), frozenField);
            }
        }

        final TiffOutputSummary outputSummary = validateDirectories(outputSet);

        final List<TiffOutputItem> allOutputItems = outputSet.getOutputItems(outputSummary);
        final List<TiffOutputItem> outputItems = new ArrayList<>();
        for (final TiffOutputItem outputItem : allOutputItems) {
            if (!frozenFieldOffsets.containsKey(outputItem.getOffset())) {
                outputItems.add(outputItem);
            }
        }

        final long outputLength = updateOffsetsStep(analysis, outputItems);

        outputSummary.updateOffsets(byteOrder);

        writeStep(os, outputSet, analysis, outputItems, outputLength);

    }

    private long updateOffsetsStep(final List<TiffElement> analysis,
            final List<TiffOutputItem> outputItems) {
        // items we cannot fit into a gap, we shall append to tail.
        long overflowIndex = exifBytes.length;

        // make copy.
        final List<TiffElement> unusedElements = new ArrayList<>(analysis);

        // should already be in order of offset, but make sure.
        Collections.sort(unusedElements, TiffElement.COMPARATOR);
        Collections.reverse(unusedElements);
        // any items that represent a gap at the end of the exif segment, can be
        // discarded.
        while (!unusedElements.isEmpty()) {
            final TiffElement element = unusedElements.get(0);
            final long elementEnd = element.offset + element.length;
            if (elementEnd == overflowIndex) {
                // discarding a tail element. should only happen once.
                overflowIndex -= element.length;
                unusedElements.remove(0);
            } else {
                break;
            }
        }

        Collections.sort(unusedElements, ELEMENT_SIZE_COMPARATOR);
        Collections.reverse(unusedElements);

        // make copy.
        final List<TiffOutputItem> unplacedItems = new ArrayList<>(
                outputItems);
        Collections.sort(unplacedItems, ITEM_SIZE_COMPARATOR);
        Collections.reverse(unplacedItems);

        while (!unplacedItems.isEmpty()) {
            // pop off largest unplaced item.
            final TiffOutputItem outputItem = unplacedItems.remove(0);
            final int outputItemLength = outputItem.getItemLength();
            // search for the smallest possible element large enough to hold the
            // item.
            TiffElement bestFit = null;
            for (final TiffElement element : unusedElements) {
                if (element.length >= outputItemLength) {
                    bestFit = element;
                } else {
                    break;
                }
            }
            if (null == bestFit) {
                // we couldn't place this item. overflow.
                if ((overflowIndex & 1L) != 0) {
                    overflowIndex += 1;
                }
                outputItem.setOffset(overflowIndex);
                overflowIndex += outputItemLength;
            } else {
                long offset = bestFit.offset;
                if ((offset & 1L) != 0) {
                    offset += 1;
                }
                outputItem.setOffset(offset);
                unusedElements.remove(bestFit);

                if (bestFit.length > outputItemLength) {
                    // not a perfect fit.
                    final long excessOffset = bestFit.offset + outputItemLength;
                    final int excessLength = bestFit.length - outputItemLength;
                    unusedElements.add(new TiffElement.Stub(excessOffset,
                            excessLength));
                    // make sure the new element is in the correct order.
                    Collections.sort(unusedElements, ELEMENT_SIZE_COMPARATOR);
                    Collections.reverse(unusedElements);
                }
            }
        }

        return overflowIndex;
    }

    private static class BufferOutputStream extends OutputStream {
        private final byte[] buffer;
        private int index;

        BufferOutputStream(final byte[] buffer, final int index) {
            this.buffer = buffer;
            this.index = index;
        }

        @Override
        public void write(final int b) throws IOException {
            if (index >= buffer.length) {
                throw new IOException("Buffer overflow.");
            }

            buffer[index++] = (byte) b;
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            if (index + len > buffer.length) {
                throw new IOException("Buffer overflow.");
            }
            System.arraycopy(b, off, buffer, index, len);
            index += len;
        }
    }

    private void writeStep(final OutputStream os, final TiffOutputSet outputSet,
            final List<TiffElement> analysis, final List<TiffOutputItem> outputItems,
            final long outputLength) throws IOException, ImageWriteException {
        final TiffOutputDirectory rootDirectory = outputSet.getRootDirectory();

        final byte[] output = new byte[(int) outputLength];

        // copy old data (including maker notes, etc.)
        System.arraycopy(exifBytes, 0, output, 0, Math.min(exifBytes.length, output.length));

        final BufferOutputStream headerStream = new BufferOutputStream(output, 0);
        final BinaryOutputStream headerBinaryStream = new BinaryOutputStream(headerStream, byteOrder);
        writeImageFileHeader(headerBinaryStream, rootDirectory.getVersion() , rootDirectory.getOffset());

        // zero out the parsed pieces of old exif segment, in case we don't
        // overwrite them.
        for (final TiffElement element : analysis) {
            Arrays.fill(output, (int) element.offset, (int) Math.min(element.offset + element.length, output.length),
                    (byte) 0);
        }

        // write in the new items
        for (final TiffOutputItem outputItem : outputItems) {
            try (final BinaryOutputStream bos = new BinaryOutputStream(
                    new BufferOutputStream(output, (int) outputItem.getOffset()), byteOrder)) {
                outputItem.writeItem(bos);
            }
        }

        os.write(output);
    }

}
