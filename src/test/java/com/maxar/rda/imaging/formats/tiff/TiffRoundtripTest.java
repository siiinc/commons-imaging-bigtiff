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

import static org.junit.Assert.assertNotNull;

import com.maxar.rda.imaging.formats.tiff.constants.TiffConstants;
import com.maxar.rda.imaging.internal.Debug;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.maxar.rda.imaging.ImageFormats;
import com.maxar.rda.imaging.ImageInfo;
import com.maxar.rda.imaging.Imaging;
import com.maxar.rda.imaging.ImagingConstants;
import com.maxar.rda.imaging.common.ImageMetadata;
import org.junit.Test;

public class TiffRoundtripTest extends TiffBaseTest {

    @Test
    public void test() throws Exception {
        final List<File> images = getTiffImages();
        for (final File imageFile : images) {

            Debug.debug("imageFile", imageFile);

            final ImageMetadata metadata = Imaging.getMetadata(imageFile);
            assertNotNull(metadata);

            final ImageInfo imageInfo = Imaging.getImageInfo(imageFile);
            assertNotNull(imageInfo);

            final BufferedImage image = Imaging.getBufferedImage(imageFile);
            assertNotNull(image);

            final int[] compressions = new int[]{
                    TiffConstants.TIFF_COMPRESSION_UNCOMPRESSED,
                    TiffConstants.TIFF_COMPRESSION_LZW,
                    TiffConstants.TIFF_COMPRESSION_PACKBITS
            };
            for (final int compression : compressions) {
                final File tempFile = createTempFile(imageFile.getName() + "-" + compression + ".", ".tif");
                final Map<String, Object> params = new HashMap<>();
                params.put(ImagingConstants.PARAM_KEY_COMPRESSION, compression);
                Imaging.writeImage(image, tempFile, ImageFormats.TIFF,
                        params);
                final BufferedImage image2 = Imaging.getBufferedImage(tempFile);
                assertNotNull(image2);
            }
        }
    }

}
