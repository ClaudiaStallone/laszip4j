/*
 * Copyright 2011-2015, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import com.github.mreutegg.laszip4j.laslib.LASevlr;
import com.github.mreutegg.laszip4j.laslib.LASreadOpener;
import com.github.mreutegg.laszip4j.laslib.LASreader;

import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fclose;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fopenRAF;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdio.sprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strlen;
import static com.github.mreutegg.laszip4j.clib.Cstring.strncmp;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.asByteArray;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASindex {

    private static final PrintStream stderr = logger.log;

    public int start; // unsigned
    public int end; // unsigned
    public int full; // unsigned
    public int ToTal; // unsigned
    public int cells; // unsigned

    private LASquadtree spatial;
    private LASinterval interval;
    private boolean have_interval;

    private static class my_cell_hash extends HashMap<Integer, Integer> {}

    public LASindex()
    {
        spatial = null;
        interval = null;
        have_interval = FALSE;
        start = 0;
        end = 0;
        full = 0;
        total = 0;
        cells = 0;
    }

    public void prepare(LASquadtree spatial, int threshold)
    {
        this.spatial = spatial;
        this.interval = new LASinterval(threshold);
    }

    public boolean add(double x, double y, int p_index)
    {
        int cell = spatial.get_cell_index(x, y);
        return interval.add(p_index, cell);
    }

    public void complete(int u_minimum_points, int maximum_intervals) {
        complete(u_minimum_points, maximum_intervals, true);
    }

 public class LasDefinitions {
    // LAS Tools Version
    private static final int LAS_TOOLS_VERSION = 220310;

    // LAS Tools Format Constants
    private static final int LAS_TOOLS_FORMAT_DEFAULT = 0;
    private static final int LAS_TOOLS_FORMAT_LAS = 1;
    private static final int LAS_TOOLS_FORMAT_LAZ = 2;
    private static final int LAS_TOOLS_FORMAT_BIN = 3;
    private static final int LAS_TOOLS_FORMAT_QFIT = 4;
    private static final int LAS_TOOLS_FORMAT_VRML = 5;
    private static final int LAS_TOOLS_FORMAT_TXT = 6;
    private static final int LAS_TOOLS_FORMAT_SHP = 7;
    private static final int LAS_TOOLS_FORMAT_ASC = 8;
    private static final int LAS_TOOLS_FORMAT_BIL = 9;
    private static final int LAS_TOOLS_FORMAT_FLT = 10;
    private static final int LAS_TOOLS_FORMAT_DTM = 11;

    // LAS Tools Global Encoding Bits
    private static final int LAS_TOOLS_GLOBAL_ENCODING_BIT_GPS_TIME_TYPE = 0;
    private static final int LAS_TOOLS_GLOBAL_ENCODING_BIT_WDP_INTERNAL = 1;
    private static final int LAS_TOOLS_GLOBAL_ENCODING_BIT_WDP_EXTERNAL = 2;
    private static final int LAS_TOOLS_GLOBAL_ENCODING_BIT_SYNTHETIC = 3;
    private static final int LAS_TOOLS_GLOBAL_ENCODING_BIT_OGC_WKT_CRS = 4;

    // LAS Tools IO Buffer Sizes
    private static final int LAS_TOOLS_IO_IBUFFER_SIZE = 262144;
    private static final int LAS_TOOLS_IO_OBUFFER_SIZE = 262144;

    private LasDefinitions() {
        throw new AssertionError("LasDefinitions class should not be instantiated.");
    }
}
}
