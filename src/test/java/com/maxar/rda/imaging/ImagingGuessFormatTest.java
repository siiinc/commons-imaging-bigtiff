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

package com.maxar.rda.imaging;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ImagingGuessFormatTest extends ImagingTest {


    public static final String TIFF_IMAGE_FILE = "tiff\\1\\Oregon Scientific DS6639 - DSC_0307 - small.tif";

    private final ImageFormats expectedFormat;
    private final String pathToFile;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[] { ImageFormats.TIFF, TIFF_IMAGE_FILE },
                new Object[] { ImageFormats.TIFF, TIFF_IMAGE_FILE }
        );
    }

    public ImagingGuessFormatTest(final ImageFormats expectedFormat, final String pathToFile) {
        this.expectedFormat = expectedFormat;
        this.pathToFile = pathToFile;
    }

    @Test
    public void testGuessFormat() throws Exception {
        final String imagePath = FilenameUtils.separatorsToSystem(pathToFile);
        final File imageFile = new File(ImagingTestConstants.TEST_IMAGE_FOLDER, imagePath);

        final ImageFormat guessedFormat = Imaging.guessFormat(imageFile);
        assertEquals(expectedFormat, guessedFormat);
    }

}
