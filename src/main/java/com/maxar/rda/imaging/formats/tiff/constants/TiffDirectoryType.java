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
package com.maxar.rda.imaging.formats.tiff.constants;

public enum TiffDirectoryType {
    TIFF_DIRECTORY_IFD0(
            true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_0, "IFD0"),

    TIFF_DIRECTORY_IFD1(
            true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_1, "IFD1"),

    TIFF_DIRECTORY_IFD2(
            true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_2, "IFD2"),

    TIFF_DIRECTORY_IFD3(
            true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_3, "IFD3"),

    TIFF_DIRECTORY_IFD4(
        true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_4, "IFD4"),

    TIFF_DIRECTORY_IFD5(
        true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_5, "IFD5"),

    TIFF_DIRECTORY_IFD6(
        true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_6, "IFD6"),

    TIFF_DIRECTORY_IFD7(
        true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_7, "IFD7"),

    TIFF_DIRECTORY_IFD8(
        true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_8, "IFD8"),

    TIFF_DIRECTORY_IFD9(
        true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_9, "IFD9"),

    TIFF_DIRECTORY_IFD10(
        true, TiffDirectoryConstants.DIRECTORY_TYPE_DIR_10, "IFD10"),

    EXIF_DIRECTORY_INTEROP_IFD(
            false, TiffDirectoryConstants.DIRECTORY_TYPE_INTEROPERABILITY, "Interop IFD"),
    EXIF_DIRECTORY_MAKER_NOTES(
            false, TiffDirectoryConstants.DIRECTORY_TYPE_MAKER_NOTES, "Maker Notes"),
    EXIF_DIRECTORY_EXIF_IFD(
            false, TiffDirectoryConstants.DIRECTORY_TYPE_EXIF, "Exif IFD"),
    EXIF_DIRECTORY_GPS(
            false, TiffDirectoryConstants.DIRECTORY_TYPE_GPS, "GPS IFD");

    private final boolean isImageDirectory;
    public final int directoryType;
    public final String name;

    TiffDirectoryType(final boolean isImageDirectory, final int directoryType, final String name) {
        this.isImageDirectory = isImageDirectory;
        this.directoryType = directoryType;
        this.name = name;
    }

    public boolean isImageDirectory() {
        return isImageDirectory;
    }

    public static TiffDirectoryType getExifDirectoryType(final int type) {

        for (final TiffDirectoryType tiffDirectoryType : values()) {
            if (tiffDirectoryType.directoryType == type) {
                return tiffDirectoryType;
            }
        }
        return TiffDirectoryType.EXIF_DIRECTORY_UNKNOWN;
    }

    public static final TiffDirectoryType EXIF_DIRECTORY_IFD0 = TIFF_DIRECTORY_IFD0;
    public static final TiffDirectoryType TIFF_DIRECTORY_ROOT = TIFF_DIRECTORY_IFD0;
    public static final TiffDirectoryType EXIF_DIRECTORY_IFD1 = TIFF_DIRECTORY_IFD1;
    public static final TiffDirectoryType EXIF_DIRECTORY_IFD2 = TIFF_DIRECTORY_IFD2;
    public static final TiffDirectoryType EXIF_DIRECTORY_IFD3 = TIFF_DIRECTORY_IFD3;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD = TIFF_DIRECTORY_IFD1;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD1 = TIFF_DIRECTORY_IFD2;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD2 = TIFF_DIRECTORY_IFD3;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD3 = TIFF_DIRECTORY_IFD3;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD4 = TIFF_DIRECTORY_IFD4;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD5 = TIFF_DIRECTORY_IFD5;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD6 = TIFF_DIRECTORY_IFD6;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD7 = TIFF_DIRECTORY_IFD7;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD8 = TIFF_DIRECTORY_IFD8;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD9 = TIFF_DIRECTORY_IFD9;
    public static final TiffDirectoryType EXIF_DIRECTORY_SUB_IFD10 = TIFF_DIRECTORY_IFD10;
    public static final TiffDirectoryType EXIF_DIRECTORY_UNKNOWN = null;
}
