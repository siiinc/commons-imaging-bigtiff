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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.maxar.rda.imaging.internal.Debug;
import com.maxar.rda.imaging.test.util.FileSystemTraversal;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class ImagingTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    protected File createTempFile(final String prefix, final String suffix)
            throws IOException {
        return File.createTempFile(prefix, suffix, folder.newFolder());
    }

    protected boolean isPhilHarveyTestImage(final File file) {
        return file.getAbsolutePath().startsWith(
                ImagingTestConstants.PHIL_HARVEY_TEST_IMAGE_FOLDER.getAbsolutePath());
    }

    public interface ImageFilter {
        boolean accept(File file) throws IOException, ImageReadException;
    }

    protected File getTestImage() throws IOException, ImageReadException {
        return getTestImage(null);
    }

    protected File getTestImageByName(final String fileName)
            throws IOException, ImageReadException {
        return getTestImage(file -> file.getName().equals(fileName));
    }

    protected File getTestImage(final ImageFilter filter) throws IOException,
            ImageReadException {
        final List<File> images = getTestImages(filter, 1);

        assertTrue(images.size() > 0);

        return images.get(0);
    }

    protected static List<File> getTestImages() throws IOException, ImageReadException {
        return getTestImages(null, -1);
    }

    protected static List<File> getTestImages(final ImageFilter filter) throws IOException,
            ImageReadException {
        return getTestImages(filter, -1);
    }

    private static final List<File> ALL_IMAGES = new ArrayList<>();

    static {
        File imagesFolder = ImagingTestConstants.TEST_IMAGE_FOLDER;

        imagesFolder = imagesFolder.getAbsoluteFile();

        Debug.debug("imagesFolder", imagesFolder);
        assertTrue(imagesFolder.exists());

        final FileSystemTraversal.Visitor visitor = (file, progressEstimate) -> {
            if (!Imaging.hasImageFileExtension(file)) {
                return true;
            }
            ALL_IMAGES.add(file);
            return true;
        };
        new FileSystemTraversal().traverseFiles(imagesFolder, visitor);
    }

    protected static List<File> getTestImages(final ImageFilter filter, final int max)
            throws IOException, ImageReadException {
        final List<File> images = new ArrayList<>();

        for (final File file : ALL_IMAGES) {
            if (!Imaging.hasImageFileExtension(file)) {
                continue;
            }

            if (file.getParentFile().getName().toLowerCase().equals("@broken")) {
                continue;
            }

            //todo: remove this when finished with bigtiff implementation
            if (file.getParentFile().getName().equalsIgnoreCase("9")) {
                continue;
            }

            if (filter != null && !filter.accept(file)) {
                continue;
            }

            images.add(file);

            if (max > 0 && images.size() >= max) {
                break;
            }
        }

        assertTrue(images.size() > 0);

        return images;
    }

    protected boolean isInvalidPNGTestFile(final File file) {
        return (file.getParentFile().getName().equalsIgnoreCase("pngsuite") &&
                file.getName().toLowerCase().startsWith("x"));
    }
}
