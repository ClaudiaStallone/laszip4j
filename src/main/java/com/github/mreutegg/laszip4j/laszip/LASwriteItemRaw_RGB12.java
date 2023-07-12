/*
 * (c) 2007-2022, rapidlasso GmbH - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the Apache Public License 2.0 published by the Apache Software
 * Foundation. See the COPYING file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

public class LASwriteItemRaw_RGB12_ extends LASwriteItemRaw<PointDataRecordRGB> {
    @Override
    public boolean write(PointDataRecordRGB point, int context) {
        outstream.put16bitsLE(point.R);
        outstream.put16bitsLE(point.G);
        outstream.put16bitsLE(point.B);
        return true;
    }
}
