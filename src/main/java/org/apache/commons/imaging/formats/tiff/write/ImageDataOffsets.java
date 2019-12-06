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
package org.apache.commons.imaging.formats.tiff.write;

import org.apache.commons.imaging.formats.tiff.TiffElement;
import org.apache.commons.imaging.formats.tiff.constants.TiffConstants;
import org.apache.commons.imaging.formats.tiff.fieldtypes.FieldType;

class ImageDataOffsets {
    final long[] imageDataOffsets;
    final TiffOutputField imageDataOffsetsField;
    final TiffOutputItem[] outputItems;

    final FieldType type;

    FieldType getType(){ return type; }

    ImageDataOffsets(final TiffElement.DataElement[] imageData,
            final int[] imageDataOffsets,
            final TiffOutputField imageDataOffsetsField) {
        type = FieldType.LONG;

        this.imageDataOffsets = new long[imageDataOffsets.length];
        for(int j = 0; j < imageDataOffsets.length; j++)
        {
            this.imageDataOffsets[j] = imageDataOffsets[j];
        }
        this.imageDataOffsetsField = imageDataOffsetsField;

        outputItems = new TiffOutputItem[imageData.length];
        for (int i = 0; i < imageData.length; i++) {
            final TiffOutputItem item = new TiffOutputItem.Value("TIFF image data",
                    imageData[i].getData());
            outputItems[i] = item;
        }

    }

    ImageDataOffsets(final TiffElement.DataElement[] imageData,
                     final long[] imageDataOffsets,
                     final TiffOutputField imageDataOffsetsField) {
        type = FieldType.LONG8;
        this.imageDataOffsets = imageDataOffsets;
        this.imageDataOffsetsField = imageDataOffsetsField;

        outputItems = new TiffOutputItem[imageData.length];
        for (int i = 0; i < imageData.length; i++) {
            final TiffOutputItem item = new TiffOutputItem.Value("TIFF image data",
                    imageData[i].getData());
            outputItems[i] = item;
        }

    }

}
