/*
 * Copyright 2007-2015, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.lastools;

import com.github.mreutegg.laszip4j.clib.Cstdio;
import com.github.mreutegg.laszip4j.laslib.LASreader;
import com.github.mreutegg.laszip4j.laslib.LASwaveform13reader;
import com.github.mreutegg.laszip4j.laslib.LASwaveform13writer;
import com.github.mreutegg.laszip4j.laslib.LASwriter;
import com.github.mreutegg.laszip4j.laslib.LasDefinitions;
import com.github.mreutegg.laszip4j.laszip.ByteStreamIn;
import com.github.mreutegg.laszip4j.laslib.LASreadOpener;
import com.github.mreutegg.laszip4j.laslib.LASwriteOpener;
import com.github.mreutegg.laszip4j.laszip.LASindex;
import com.github.mreutegg.laszip4j.laszip.LASquadtree;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atof;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atoi;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAS;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAZ;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class Laszip {

    public static void main(String[] args) {
        run(args);
        byebye(false);
    }

   public static void run(String[] args) {
    boolean verbose = false;
    boolean report_file_size = false;
    boolean check_integrity = false;
    boolean waveform = false;
    boolean waveform_with_map = false;
    boolean format_not_specified = false;
    boolean lax = false;
    boolean append = false;
    float tile_size = 100.0f;
    int threshold = 1000;
    int minimum_points = 100000;
    int maximum_intervals = -20;
    int end_of_points = -1;

    LASreadOpener lasreadopener = new LASreadOpener();
    GeoProjectionConverter geoprojectionconverter = new GeoProjectionConverter();
    LASwriteOpener laswriteopener = new LASwriteOpener();

    if (args.length < 2) {
        System.err.println("missing argument");
        System.exit(1);
    }

    if (!geoprojectionconverter.parse(args) || !lasreadopener.parse(args) || !laswriteopener.parse(args)) {
        byebye(true);
    }

    for (int i = 1; i < args.length; i++) {
        String arg = args[i];
        if (arg == null || arg.isEmpty()) {
            continue;
        } else if (arg.equals("-h") || arg.equals("-help")) {
            System.err.println("LAStools (by martin@rapidlasso.com) version " + LasDefinitions.LAS_TOOLS_VERSION);
            usage();
        } else if (arg.equals("-v") || arg.equals("-verbose")) {
            verbose = true;
        } else if (arg.equals("-version")) {
            System.err.println("LAStools (by martin@rapidlasso.com) version " + LasDefinitions.LAS_TOOLS_VERSION);
            byebye();
        } else if (arg.equals("-gui")) {
            System.err.println("WARNING: not compiled with GUI support. ignoring '-gui' ...");
        } else if (arg.equals("-cores")) {
            System.err.println("WARNING: not compiled with multi-core batching. ignoring '-cores' ...");
            i++;
        } else if (arg.equals("-dry")) {
            dry = true;
        } else if (arg.equals("-lax")) {
            lax = true;
        } else if (arg.equals("-append")) {
            append = true;
        } else if (arg.equals("-eop")) {
            if ((i + 1) >= args.length) {
                System.err.println("ERROR: '-eop' needs 1 argument: char");
                usage(true);
            }
            i++;
            end_of_points = Integer.parseInt(args[i]);
            if (end_of_points < 0 || end_of_points > 255) {
                System.err.println("ERROR: end of points value needs to be between 0 and 255");
                usage(true);
            }
        } else if (arg.equals("-tile_size")) {
            if ((i + 1) >= args.length) {
                System.err.println("ERROR: '-tile_size' needs 1 argument: size");
                usage(true);
            }
            i++;
            tile_size = Float.parseFloat(args[i]);
        } else if (arg.equals("-maximum")) {
            if ((i + 1) >= args.length) {
                System.err.println("ERROR: '-maximum' needs 1 argument: number");
                usage(true);
            }
            i++;
            maximum_intervals = Integer.parseInt(args[i]);
        } else if (arg.equals("-minimum")) {
            if ((i + 1) >= args.length) {
                System.err.println("ERROR: '-minimum' needs 1 argument: number");
            usage(true);
        }
        i++;
        minimum_points = Integer.parseInt(args[i]);
    } else if (arg.equals("-threshold")) {
        if ((i + 1) >= args.length) {
            System.err.println("ERROR: '-threshold' needs 1 argument: value");
            usage(true);
        }
        i++;
        threshold = Integer.parseInt(args[i]);
    } else if (arg.equals("-size")) {
        report_file_size = true;
    } else if (arg.equals("-check")) {
        check_integrity = true;
    } else if (arg.equals("-waveform") || arg.equals("-waveforms")) {
        waveform = true;
    } else if (arg.equals("-waveform_with_map") || arg.equals("-waveforms_with_map")) {
        waveform = true;
        waveform_with_map = true;
    } else if (!arg.startsWith("-") && lasreadopener.get_file_name_number() == 0) {
        lasreadopener.add_file_name(arg);
        args[i] = null;
    } else {
        System.err.println("ERROR: cannot understand argument '" + arg + "'");
        usage(true);
    }
}

// check input
if (!lasreadopener.active()) {
    System.err.println("ERROR: no input specified");
    usage(true);
}

// check output
if (laswriteopener.is_piped()) {
    if (lax) {
        System.err.println("WARNING: disabling LAX generation for piped output");
        lax = false;
        append = false;
    }
}

// make sure we do not corrupt the input file
if (lasreadopener.get_file_name() != null && laswriteopener.get_file_name() != null && lasreadopener.get_file_name().equals(laswriteopener.get_file_name())) {
    System.err.println("ERROR: input and output file name are identical");
    usage(true);
}

// check if projection info was set in the command line
int number_of_keys = 0;
GeoProjectionGeoKeys[] geo_keys = null;
int num_geo_double_params = 0;
double[] geo_double_params = null;
if (geoprojectionconverter.has_projection()) {
    projection_was_set = geoprojectionconverter.get_geo_keys_from_projection(number_of_keys, geo_keys, num_geo_double_params, geo_double_params);
}

// check if the output format was *not* specified in the command line
format_not_specified = !laswriteopener.format_was_specified();

if (verbose) total_start_time = taketime();

// loop over multiple input files
while (lasreadopener.active()) {
    if (verbose) start_time = taketime();

    // open lasreader
    LASreader lasreader = lasreadopener.open();
    if (lasreader == null) {
        System.err.println("ERROR: could not open lasreader");
        usage(true);
    }

    // switch
    if (report_file_size) {
        // maybe only report uncompressed file size
        long uncompressed_file_size = lasreader.npoints * lasreader.header.point_data_record_length + lasreader.header.offset_to_point_data;
        if (uncompressed_file_size < Integer.MAX_VALUE) {
            System.err.println("uncompressed file size is " + uncompressed_file_size + " bytes or " + (double) uncompressed_file_size / 1024.0 / 1024.0 + " MB for '" + lasreadopener.get_file_name() + "'");
        } else {
            System.err.println("uncompressedfile size is " + (double) uncompressed_file_size / 1024.0 / 1024.0 + " MB or " + (double) uncompressed_file_size / 1024.0/ 1024.0 / 1024.0 / 1024.0 + " GB for '" + lasreadopener.get_file_name() + "'");
        }
    } else if (dry || check_integrity) {
        // maybe only a dry read pass
        start_time = taketime();
        while (lasreader.read_point())
            ;
        if (check_integrity) {
            if (lasreader.p_count != lasreader.npoints) {
                System.err.println("FAILED integrity check for '" + lasreadopener.get_file_name() + "' after " + lasreader.p_count + " of " + lasreader.npoints + " points");
            } else {
                System.err.println("SUCCESS for '" + lasreadopener.get_file_name() + "'");
            }
        } else {
            System.err.println("needed " + (taketime() - start_time) + " seconds to parse " + lasreader.npoints + " points from '" + lasreadopener.get_file_name() + "'");
        }
    } else {

    }
}
}


    private static double taketime() {
        return (double) System.currentTimeMillis() / 1000;
    }

    private static void usage() {
        usage(false);
    }

    private static void usage(boolean error)
    {
        PrintStream stderr = System.err;
        fprintf(stderr,"usage:\n");
        fprintf(stderr,"laszip *.las\n");
        fprintf(stderr,"laszip *.laz\n");
        fprintf(stderr,"laszip *.txt -iparse xyztiarn\n");
        fprintf(stderr,"laszip lidar.las\n");
        fprintf(stderr,"laszip lidar.laz -v\n");
        fprintf(stderr,"laszip -i lidar.las -o lidar_zipped.laz\n");
        fprintf(stderr,"laszip -i lidar.laz -o lidar_unzipped.las\n");
        fprintf(stderr,"laszip -i lidar.las -stdout -olaz > lidar.laz\n");
        fprintf(stderr,"laszip -stdin -o lidar.laz < lidar.las\n");
        fprintf(stderr,"laszip -h\n");
        byebye(error);
    }

    private static void byebye() {
        byebye(false);
    }

    private static void byebye(boolean error)
    {
        System.exit(error ? 1 : 0);
    }

}
