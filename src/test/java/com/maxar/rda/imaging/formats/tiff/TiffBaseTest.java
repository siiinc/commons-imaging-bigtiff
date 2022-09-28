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

import com.maxar.rda.imaging.ImagingTest;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.maxar.rda.imaging.ImageFormat;
import com.maxar.rda.imaging.ImageFormats;
import com.maxar.rda.imaging.ImageReadException;
import com.maxar.rda.imaging.Imaging;

public abstract class TiffBaseTest extends ImagingTest
{

    private static boolean isTiff(final File file) throws IOException,
            ImageReadException {
        final ImageFormat format = Imaging.guessFormat(file);
        return format == ImageFormats.TIFF;
    }

    private static final ImageFilter IMAGE_FILTER = file -> isTiff(file);

    protected List<File> getTiffImages() throws IOException, ImageReadException {
        return getTestImages(IMAGE_FILTER);
    }

}
