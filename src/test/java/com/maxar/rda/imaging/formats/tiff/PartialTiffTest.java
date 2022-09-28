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

import com.maxar.rda.imaging.formats.tiff.constants.GeoTiffTagConstants;
import com.maxar.rda.imaging.formats.tiff.constants.TiffConstants;
import com.maxar.rda.imaging.formats.tiff.constants.TiffTagConstants;
import com.maxar.rda.imaging.FormatCompliance;
import com.maxar.rda.imaging.common.ByteConversions;
import com.maxar.rda.imaging.common.bytesource.ByteSourceArray;
import com.maxar.rda.imaging.formats.tiff.write.TiffImageWriterLossy;
import com.maxar.rda.imaging.formats.tiff.write.TiffOutputDirectory;
import com.maxar.rda.imaging.formats.tiff.write.TiffOutputSet;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class PartialTiffTest {

    @Test
    public void testSLong8() {
        long[] offsets = {0L, -3959422976L, 4697620480L, -9223372036854775807L, 9223372036854775807L};
        byte[] offAsBytes = ByteConversions.toBytes(offsets, ByteOrder.LITTLE_ENDIAN);
        long[] regurg = ByteConversions.toLongs(offAsBytes, ByteOrder.LITTLE_ENDIAN);
        Assert.assertEquals(offsets.length, regurg.length);

        for (int idx = 0; idx < offsets.length; idx++) {
            Assert.assertEquals(offsets[idx], regurg[idx]);
        }

    }

    @Test
    public void testLong8()
    {
        long max = Long.parseUnsignedLong("18446744073709551615");
        String mstr = Long.toUnsignedString(max);
//        UInt64[] offsets = {new UInt64(9223372036854775807L), new UInt64(-3959422976L), new UInt64(4697620480L), new UInt64(-9223372036854775807L), new UInt64(9223372036854775807L), new UInt64("18446744073709551615")};
        long[] offsets = {9223372036854775807L, -3959422976L,4697620480L, -9223372036854775807L, 9223372036854775807L, max};
        byte[] offAsBytes = ByteConversions.toBytes(offsets, ByteOrder.LITTLE_ENDIAN);
        long[] regurg = ByteConversions.toLongs(offAsBytes, ByteOrder.LITTLE_ENDIAN);
        Assert.assertEquals(offsets.length, regurg.length);

        for (int idx = 0; idx < offsets.length; idx++) {
            Assert.assertEquals(offsets[idx], regurg[idx]);
        }
    }

    @Test
    @Ignore
    public void testPartialTiff() {
        double[] pixelScales = {1, -1, 0};


        double xO = 0;
        double yO = 0;
        double[] geoParams = {0.000000,0.000000,0.000000,xO,yO,0.000000};
        List<TiffImageData.Data> tiles = new ArrayList<>();

        int width = 9130;
        int height = 42882;
        short tileWidth = 2048;
        short tileHeight = 2048;
        short samplesPerPixel = 8;
        short[] bitsPerSample = new short[samplesPerPixel];
        short sampleBits = (short)64;
        Arrays.fill(bitsPerSample, sampleBits);
        short[] extraSamples = null;
        if(samplesPerPixel > 1)
        {
            extraSamples = new short[samplesPerPixel-1];
            Arrays.fill(extraSamples, (short)0);
        }


        short[] sampleFormat = new short[samplesPerPixel];
        short formatType = (short) TiffTagConstants.SAMPLE_FORMAT_VALUE_IEEE_FLOATING_POINT;
        for(int idx = 0; idx < bitsPerSample.length; idx++)
        {
            sampleFormat[idx] = formatType;
        }
        TiffOutputSet set;
        try
        {
            set = new TiffOutputSet(TiffConstants.DEFAULT_TIFF_BYTE_ORDER, TiffConstants.TIFF_BIGTIFF);
            final TiffOutputDirectory dir = set.getOrCreateRootDirectory();
            dir.add(TiffTagConstants.TIFF_TAG_COPYRIGHT, "Maxar Inc");
            dir.add(TiffTagConstants.TIFF_TAG_IMAGE_WIDTH, width);
            dir.add(TiffTagConstants.TIFF_TAG_IMAGE_LENGTH, height);
            dir.add(TiffTagConstants.TIFF_TAG_TILE_WIDTH, tileWidth);
            dir.add(TiffTagConstants.TIFF_TAG_COMPRESSION, (short) TiffTagConstants.COMPRESSION_VALUE_UNCOMPRESSED);
            dir.add(TiffTagConstants.TIFF_TAG_TILE_LENGTH, tileHeight);
            dir.add(TiffTagConstants.TIFF_TAG_BITS_PER_SAMPLE, bitsPerSample);
            dir.add(TiffTagConstants.TIFF_TAG_SAMPLE_FORMAT, sampleFormat);
            dir.add(TiffTagConstants.TIFF_TAG_SAMPLES_PER_PIXEL, samplesPerPixel);
            if(extraSamples != null)
            {
                dir.add(TiffTagConstants.TIFF_TAG_EXTRA_SAMPLES, extraSamples);
            }

            dir.add(TiffTagConstants.TIFF_TAG_PLANAR_CONFIGURATION, (short) TiffTagConstants.PLANAR_CONFIGURATION_VALUE_CHUNKY);
            dir.add(TiffTagConstants.TIFF_TAG_PHOTOMETRIC_INTERPRETATION, (short)TiffTagConstants.PHOTOMETRIC_INTERPRETATION_VALUE_BLACK_IS_ZERO);
            dir.add(GeoTiffTagConstants.EXIF_TAG_MODEL_PIXEL_SCALE_TAG, pixelScales);
            dir.add(GeoTiffTagConstants.EXIF_TAG_MODEL_TIEPOINT_TAG, geoParams);


            int[] keyCommon = new int[] { 1, 1, 0, 4 };
            int[] key;
            int GTModelTypeGeoKey = 1024;
            int GTRasterTypeGeoKey = 1025;

            String[] parts = "EPSG:32610".split(":");
            int projectedCSTypeGeoKey = 3072;
            key = new int[] {GTModelTypeGeoKey, 0, 1, 1, GTRasterTypeGeoKey, 0, 1, 1, projectedCSTypeGeoKey, 0, 1, Integer.parseInt(parts[1]) };


            int[] geokeysInt = IntStream.concat(Arrays.stream(keyCommon), Arrays.stream(key)).toArray();

            short[] geokeys = new short[geokeysInt.length];
            for(int idx = 0; idx < geokeys.length; idx++)
            {
                geokeys[idx] = (short)geokeysInt[idx];
            }

            dir.add(GeoTiffTagConstants.EXIF_TAG_GEO_KEY_DIRECTORY_TAG, geokeys);



            BigInteger tileOffset = new BigInteger("0");
            BigInteger bITileSize = BigInteger.valueOf((long)tileWidth*(long)tileHeight*(long)sampleBits*(long)samplesPerPixel/(long)8);

            byte[] commonDummy = new byte[bITileSize.intValueExact()];
            for(long ty = 0; ty < Math.ceil((double)height/(double)tileHeight); ty++)
            {
                for(long tx = 0; tx < Math.ceil((double)width/(double)tileWidth); tx++)
                {
                    TiffImageData.Data de = new TiffImageData.Data(tileOffset.longValueExact(), bITileSize.intValueExact(), commonDummy);
                    tiles.add(de);
                    tileOffset = tileOffset.add(bITileSize);
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

            {
                //todo: remove this
                OutputStream os = new FileOutputStream(new File("/tmp/deleteme_truebig.tif"));
                os.write(data);
                os.close();
            }

            final TiffReader reader = new TiffReader(true);
            final Map<String, Object> params = new TreeMap<>();
            final FormatCompliance formatCompliance = new FormatCompliance("");
            TiffContents contents = reader.readFirstDirectory(new ByteSourceArray(data), params, false, formatCompliance);
            final TiffDirectory rootDir = contents.directories.get(0);

            //get offsets and calc which offsets we need
            long[] offsets = (long[])rootDir.getFieldValue(TiffTagConstants.TIFF_TAG_TILE_OFFSETS);

            int iiiii = 0;

        }// + yMapShift
        catch (Exception e)
        {
            throw new RuntimeException("uhoh... " + e.getMessage());
        }


    }

}