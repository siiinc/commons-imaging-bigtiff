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
package org.apache.commons.imaging.formats.tiff;


import org.apache.commons.imaging.FormatCompliance;
import org.apache.commons.imaging.ImagingTestConstants;
import org.apache.commons.imaging.common.bytesource.ByteSourceArray;
import org.apache.commons.imaging.formats.tiff.constants.TiffConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffImageWriterLossy;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class BigTiffTest {

    private final String TIFF_IMAGE_FILE1 = "tiff\\9\\BigTIFF.tif";
    private final String TIFF_IMAGE_FILE2 = "tiff\\9\\BigTIFFLong.tif";
    private final String TIFF_IMAGE_FILE3 = "tiff\\9\\BigTIFFLong8.tif";
    private final String TIFF_IMAGE_FILE4 = "tiff\\9\\BigTIFFLong8Tiles.tif";

    @Test
    public void testBigTiffRead() throws Exception{

        for (String image: Arrays.asList(TIFF_IMAGE_FILE1, TIFF_IMAGE_FILE2, TIFF_IMAGE_FILE3, TIFF_IMAGE_FILE4))
        {
            final String imagePath = FilenameUtils.separatorsToSystem(image);
            final File imageFile = new File(ImagingTestConstants.TEST_IMAGE_FOLDER, imagePath);
            final byte imageFileBytes[] = FileUtils.readFileToByteArray(imageFile);
            final TiffReader reader = new TiffReader(true);
            final Map<String, Object> params = new TreeMap<>();
            final FormatCompliance formatCompliance = new FormatCompliance("");
            TiffContents contents = reader.readFirstDirectory(new ByteSourceArray(imageFileBytes), params, false, formatCompliance);
            final TiffDirectory rootDir = contents.directories.get(0);
            int ii = 0;
        }
    }


    @Test
    public void testBigTiffWrite() throws Exception{

        short width = (short)64;
        short height = (short)64;
        short tileWidth = 32;
        short tileHeight = 32;
        short samplesPerPixel = 3;
        short[] bitsPerSample = new short[samplesPerPixel];
        short sampleBits = (short)8;
        Arrays.fill(bitsPerSample, sampleBits);

        TiffOutputSet set;
        //set = new TiffOutputSet(TiffConstants.DEFAULT_TIFF_BYTE_ORDER, TiffConstants.TIFF_CLASSIC);
        set = new TiffOutputSet(TiffConstants.DEFAULT_TIFF_BYTE_ORDER, TiffConstants.TIFF_BIGTIFF);
        final TiffOutputDirectory dir = set.getOrCreateRootDirectory();
        String tst = "Maxar Inc. " + LocalDate.now().getYear();
        dir.add(TiffTagConstants.TIFF_TAG_COPYRIGHT, tst);
        dir.add(TiffTagConstants.TIFF_TAG_IMAGE_WIDTH, width);
        dir.add(TiffTagConstants.TIFF_TAG_IMAGE_LENGTH, height);
        dir.add(TiffTagConstants.TIFF_TAG_TILE_WIDTH, tileWidth);
        dir.add(TiffTagConstants.TIFF_TAG_TILE_LENGTH, tileHeight);
        dir.add(TiffTagConstants.TIFF_TAG_BITS_PER_SAMPLE, bitsPerSample);
        dir.add(TiffTagConstants.TIFF_TAG_SAMPLES_PER_PIXEL, samplesPerPixel);
        dir.add(TiffTagConstants.TIFF_TAG_PHOTOMETRIC_INTERPRETATION, (short) TiffTagConstants.PHOTOMETRIC_INTERPRETATION_VALUE_BLACK_IS_ZERO);

        List<TiffImageData.Data> tiles = new ArrayList<>();
        long tileOffset = 0;
        long tileSize = tileWidth*tileHeight*sampleBits*samplesPerPixel/8;
        byte[] commonDummy = new byte[(int)tileSize];
        for(long ty = 0; ty < Math.ceil((double)height/(double)tileHeight); ty++)
        {
            for(long tx = 0; tx < Math.ceil((double)width/(double)tileWidth); tx++)
            {
                TiffImageData.Data de = new TiffImageData.Data(tileOffset, (int)tileSize, commonDummy);
                tiles.add(de);
                tileOffset+=tileSize;
            }
        }


        TiffElement.DataElement[] tdes = new TiffElement.DataElement[tiles.size()];
        for(int idx = 0; idx < tiles.size(); idx++)
        {
            tdes[idx] = tiles.get(idx);
        }
        TiffImageData tiffImageData = new TiffImageData.Tiles(tdes, tileWidth, tileHeight);

        dir.setTiffImageData(tiffImageData);

        final TiffImageWriterLossy writer = new TiffImageWriterLossy();
        final ByteArrayOutputStream tiff = new ByteArrayOutputStream();
        writer.write(tiff, set, false);
        byte[] data = tiff.toByteArray();

        //todo: remove this
        OutputStream os = new FileOutputStream(new File("/tmp/java_ref.tif"));
        os.write(data);
        os.close();



    }
}