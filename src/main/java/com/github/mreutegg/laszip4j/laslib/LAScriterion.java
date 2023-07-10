/*
 * Copyright 2007-2014, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laslib;

import com.github.mreutegg.laszip4j.laszip.LASpoint;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.github.mreutegg.laszip4j.clib.Cstdio.sprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.RAND_MAX;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.rand;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.srand;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.I64_FLOOR;

import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_Z; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_FLAGS; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_CLASSIFICATION; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_RGB; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_WAVEPACKET; 

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public abstract class LAScriterion
{
    public abstract String name();
    public abstract int get_Command_(StringBuilder string);
    public int get_Decompress_selective(){return LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY;};
    public abstract boolean filter(LASpoint point);
    public void reset(){};
};

class LAScriterionAnd extends LAScriterion
{
    public String name() { return "filter_and"; };
    public int get_Command(StringBuilder string) { int n = 0; n += one.get_Command(string); n += two.get_Command(string); n += sprintf(string, "-%s ", name()); return n; };
    @Override
    public int get_Decompress_selective() { return (one.get_Decompress_selective() | two.get_Decompress_selective()); };
    public boolean filter(LASpoint point) { return one.filter(point) && two.filter(point); };
    LAScriterionAnd(LAScriterion one, LAScriterion two) { this.one = one; this.two = two; };
    private LAScriterion one;
    private LAScriterion two;
};

class LAScriterionOr extends LAScriterion
{
    public String name() { return "filter_or"; };
    @Override
    public int get_Command(StringBuilder string) { int n = 0; n += one.get_Command(string); n += two.get_Command(string); n += sprintf(string, "-%s ", name()); return n; };
    public int get_decompress_selective() { return (one.get_decompress_selective() | two.get_decompress_selective()); };
    public boolean filter(LASpoint point) { return one.filter(point) || two.filter(point); };
    public LAScriterionOr(LAScriterion one, LAScriterion two) { this.one = one; this.two = two; };
    private LAScriterion one;
    private LAScriterion two;
};

class LAScriterionKeepTile extends LAScriterion
{
    public String name() { return "keep_tile"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g %g %g ", name(), ll_x, ll_y, tile_size); };
    public boolean filter(LASpoint point) { return (!point.inside_tile(ll_x, ll_y, ur_x, ur_y)); };
    public LAScriterionKeepTile(float ll_x, float ll_y, float tile_size) { this.ll_x = ll_x; this.ll_y = ll_y; this.ur_x = ll_x+tile_size; this.ur_y = ll_y+tile_size; this.tile_size = tile_size; };
    private float ll_x; 
    private float ll_y; 
    private float ur_x; 
    private float ur_y; 
    private float tile_size;
};

class LAScriterionKeepCircle extends LAScriterion
{
    public String name() { return "keep_circle"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g %g %g ", name(), center_x, center_y, radius); };
    public boolean filter(LASpoint point) { return (!point.inside_circle(center_x, center_y, radius_squared)); };
    public LAScriterionKeepCircle(double x, double y, double radius) { this.center_x = x; this.center_y = y; this.radius = radius; this.radius_squared = radius*radius; };
    private double center_x;
    private float center_y;
    private float radius;
    private float radius_squared;
};

class LAScriterionKeepxyz extends LAScriterion
{
    @Override
    public String name() { return "keep_xyz"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g %g %g %g %g %g ", name(), min_x, min_y, min_z, max_x, max_y, max_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY | LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (!point.inside_box(min_x, min_y, min_z, max_x, max_y, max_z)); };
    public LAScriterionKeepxyz(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z) { this.min_x = min_x; this.min_y = min_y; this.min_z = min_z; this.max_x = max_x; this.max_y = max_y; this.max_z = max_z; };
    private double min_x, min_y, min_z, max_x, max_y, max_z;
};

class LAScriterionDropxyz extends LAScriterion
{
    @Override
    public String name() { return "drop_xyz"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g %g %g %g %g %g ", name(), min_x, min_y, min_z, max_x, max_y, max_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY | LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.inside_box(min_x, min_y, min_z, max_x, max_y, max_z)); };
    public LAScriterionDropxyz(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z) { this.min_x = min_x; this.min_y = min_y; this.min_z = min_z; this.max_x = max_x; this.max_y = max_y; this.max_z = max_z; };
    private double min_x;
    private double min_y;
    private double min_z;
    private double max_x;
    private double max_y;
    private double max_z;

};

class LAScriterionKeepxy extends LAScriterion
{
    public String name() { return "keep_xy"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g %g %g %g ", name(), b_x, b_y, a_x, a_y); };
    public boolean filter(LASpoint point) { return (!point.inside_rectangle(below_x, below_y, above_x, above_y)); };
    public LAScriterionKeepxy(double below_x, double below_y, double above_x, double above_y) { this.below_x = below_x; this.below_y = below_y; this.above_x = above_x; this.above_y = above_y; };
    private double below_x;
    private double below_y;
    private double above_x;
    private double above_y;
};

class LAScriterionDropxy extends LAScriterion
{
    public String name() { return "drop_xy"; };
public static final String COMMAND_FORMAT = "-%s %g %g %g %g";

public int getCommand(StringBuilder string) {
    return sprintf(string, COMMAND_FORMAT, name(), below_x, below_y, above_x, above_y);
}
    public boolean filter(LASpoint point) { return (point.inside_rectangle(below_x, below_y, above_x, above_y)); };
    public LAScriterionDropxy(double below_x, double below_y, double above_x, double above_y) { this.below_x = below_x; this.below_y = below_y; this.above_x = above_x; this.above_y = above_y; };
    private double below_x;
    private double below_y;
    private double above_x;
    private double above_y;
};

class LAScriterionKeepx extends LAScriterion
{
    public String name() { return "keep_x"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_x, above_x); };
    public boolean filter(LASpoint point) { double x = point.get_x(); return (x < below_x) || (x >= above_x); };
    public LAScriterionKeepx(double below_x, double above_x) { this.below_x = below_x; this.above_x = above_x; };
    private double Below_x; 
    private double Above_x;
};

class LAScriterionDropx extends LAScriterion
{
    public String name() { return "drop_x"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_x, above_x); };
    public boolean filter(LASpoint point) { double x = point.get_x(); return ((below_x <= x) && (x < above_x)); };
    public LAScriterionDropx(double below_x, double above_x) { this.below_x = below_x; this.above_x = above_x; };
    private double below_x; 
    private double above_x;
};

class LAScriterionKeepy extends LAScriterion
{
    public String name() { return "keep_y"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_y, above_y); };
    public boolean filter(LASpoint point) { double y = point.get_y(); return (y < below_y) || (y >= above_y); };
    public LAScriterionKeepy(double below_y, double above_y) { this.below_y = below_y; this.above_y = above_y; };
    private static final double BELOW_Y = 10.0;
    private static final double ABOVE_Y = 10.0; 
};

class LAScriterionDropy extends LAScriterion
{
    public String name() { return "drop_y"; };
    public static final String COMMAND_FORMAT = "-%s %g";

    public int getCommand(StringBuilder string) 
    {
        return sprintf(string, COMMAND_FORMAT, name(), below_y, above_y);
    }

    public boolean filter(LASpoint point) { double y = point.get_y(); return ((below_y <= y) && (y < above_y)); };
    public LAScriterionDropy(double below_y, double above_y) { this.below_y = below_y; this.above_y = above_y; };
    private double BElow_y; 
    private double above_y;
};

class LAScriterionKeepz extends LAScriterion
{
    @Override
    public String name() { return "keep_z"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_z, above_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { double z = point.get_z(); return (z < below_z) || (z >= above_z); };
    LAScriterionKeepz(double below_z, double above_z) { this.below_z = below_z; this.above_z = above_z; };
    double Below_z;
    private float above_z;
};

class LAScriterionDropz extends LAScriterion
{
    @Override
    public String name() { return "drop_z"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_z, above_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { double z = point.get_z(); return ((below_z <= z) && (z < above_z)); };
    public LAScriterionDropz(double below_z, double above_z) { this.below_z = below_z; this.above_z = above_z; };
    public double below_z;
    private float above_z;
};

class LAScriterionDropxBelow extends LAScriterion
{
    public String name() { return "drop_x_below"; };
    public void yourMethod() {
    String formattedString = String.format(FORMAT_STRING, someValue1, someValue2);
    // ...
    String anotherFormattedString = String.format(FORMAT_STRING, anotherValue1, anotherValue2);
    // ...
}
    public boolean filter(LASpoint point) { return (point.get_x() < below_x); };
    public LAScriterionDropxBelow(double below_x) { this.below_x = below_x; };
    private double below_x;
};

class LAScriterionDropxAbove extends LAScriterion
{
    public String name() { return "drop_x_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), above_x); };
    public boolean filter(LASpoint point) { return (point.get_x() >= above_x); };
    public LAScriterionDropxAbove(double above_x) { this.above_x = above_x; };
    private double above_x;
};

class LAScriterionDropyBelow extends LAScriterion
{
    public String name() { return "drop_y_below"; };
public static final String COMMAND_FORMAT = "-%s %g";

public int getCommand(StringBuilder string) {
    return sprintf(string, COMMAND_FORMAT, name(), below_y);
}
    public boolean filter(LASpoint point) { return (point.get_y() < below_y); };
    public LAScriterionDropyBelow(double below_y) { this.below_y = below_y; };
    private double Below_Y;
};

class LAScriterionDropyAbove extends LAScriterion
{
    public String name() { return "drop_y_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), above_y); };
    public boolean filter(LASpoint point) { return (point.get_y() >= above_y); };
    public LAScriterionDropyAbove(double above_y) { this.above_y = above_y; };
    private double Above_Y;
};

class LAScriterionDropzBelow extends LAScriterion
{
    @Override
    public String name() { return "drop_z_below"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), below_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.get_z() < below_z); };
    public LAScriterionDropzBelow(double below_z) { this.below_z = below_z; };
    private double bELow_z;
};

class LAScriterionDropzAbove extends LAScriterion
{
    public String name() { return "drop_z_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), above_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.get_z() >= above_z); };
    LAScriterionDropzAbove(double above_z) { this.above_z = above_z; };
    double above_z;
};

class LAScriterionKeepXYInt extends LAScriterion
{
    public String name() { return "keep_XY"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d %d %d ", name(), below_X, below_Y, above_X, above_Y); };
    public boolean filter(LASpoint point) { return (point.get_X() < below_X) || (point.get_Y() < below_Y) || (point.get_X() >= above_X) || (point.get_Y() >= above_Y); };
    LAScriterionKeepXYInt(int below_X, int below_Y, int above_X, int above_Y) { this.below_X = below_X; this.below_Y = below_Y; this.above_X = above_X; this.above_Y = above_Y; };
    int below_X;
    int below_Y;
    int above_X;
    int above_Y;
};

class LAScriterionKeepXInt extends LAScriterion
{
    @Override
    public String name() { return "keep_X"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_X, above_X); };
    public boolean filter(LASpoint point) { return (point.get_X() < below_X) || (above_X <= point.get_X()); };
    LAScriterionKeepXInt(int below_X, int above_X) { this.below_X = below_X; this.above_X = above_X; };
    int below_X; 
    int above_X;
};

class LAScriterionDropXInt extends LAScriterion
{
    public String name() { return "drop_X"; };
   public static final String COMMAND_FORMAT = "-%s %d %d";

public int getCommand(StringBuilder string) {
    return sprintf(string, COMMAND_FORMAT, name(), below_X, above_X);
}

    public boolean filter(LASpoint point) { return ((below_X <= point.get_X()) && (point.get_X() < above_X)); };
    LAScriterionDropXInt(int below_X, int above_X) { this.below_X = below_X; this.above_X = above_X; };
    int below_X;
    int AboVe_X;
};

class LAScriterionKeepYInt extends LAScriterion
{
    public String name() { return "keep_Y"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_Y, above_Y); };
    public boolean filter(LASpoint point) { return (point.get_Y() < below_Y) || (above_Y <= point.get_Y()); };
    LAScriterionKeepYInt(int below_Y, int above_Y) { this.below_Y = below_Y; this.above_Y = above_Y; };
    int BeLoW_Y; 
    int AboVe_Y;
};

class LAScriterionDropYInt extends LAScriterion
{
    public String name() { return "drop_Y"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_Y, above_Y); };
    public boolean filter(LASpoint point) { return ((below_Y <= point.get_Y()) && (point.get_Y() < above_Y)); };
    @Override
    LAScriterionDropYInt(int below_Y, int above_Y) { this.below_Y = below_Y; this.above_Y = above_Y; };
    int below_Y;
    int above_Y;
};

class LAScriterionKeepZInt extends LAScriterion
{
    public String name() { return "keep_Z"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_Z, above_Z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    @Override
    public boolean filter(LASpoint point) { return (point.get_Z() < below_Z) || (above_Z <= point.get_Z()); };
    LAScriterionKeepZInt(int below_Z, int above_Z) { this.below_Z = below_Z; this.above_Z = above_Z; };
    int BEelow_Z; 
    int above_Z;
};

class LAScriterionDropZInt extends LAScriterion
{
    public String name() { return "drop_Z"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_Z, above_Z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return ((below_Z <= point.get_Z()) && (point.get_Z() < above_Z)); };
    LAScriterionDropZInt(int below_Z, int above_Z) { this.below_Z = below_Z; this.above_Z = above_Z; };
    int bElow_Z;
    int aBove_Z;
};

class LAScriterionDropXIntBelow extends LAScriterion
{
    public String name() { return "drop_X_below"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_X); };
    public boolean filter(LASpoint point) { return (point.get_X() < below_X); };
    LAScriterionDropXIntBelow(int below_X) { this.below_X = below_X; };
    int below_X;
};

class LAScriterionDropXIntAbove extends LAScriterion
{
    public String name() { return "drop_X_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_X); };
    public boolean filter(LASpoint point) { return (point.get_X() >= above_X); };
    LAScriterionDropXIntAbove(int above_X) { this.above_X = above_X; };
    int above_X;
};

class LAScriterionDropYIntBelow extends LAScriterion
{
    public String name() { return "drop_Y_below"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_Y); };
    public boolean filter(LASpoint point) { return (point.get_Y() < below_Y); };
    LAScriterionDropYIntBelow(int below_Y) { this.below_Y = below_Y; };
    int below_Y;
};

class LAScriterionDropYIntAbove extends LAScriterion
{
    public String name() { return "drop_Y_above"; };
    @Override
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_Y); };
    public boolean filter(LASpoint point) { return (point.get_Y() >= above_Y); };
    LAScriterionDropYIntAbove(int above_Y) { this.above_Y = above_Y; };
    int above_Y;
};

class LAScriterionDropZIntBelow extends LAScriterion
{
    public String name() { return "drop_Z_below"; };
    @Override
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_Z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.get_Z() < below_Z); };
    LAScriterionDropZIntBelow(int below_Z) { this.below_Z = below_Z; };
    int below_Z;
};

class LAScriterionDropZIntAbove extends LAScriterion
{
    public String name() { return "drop_Z_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_Z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.get_Z() >= above_Z); };
    LAScriterionDropZIntAbove(int above_Z) { this.above_Z = above_Z; };
    int above_Z;
};

class LAScriterionKeepFirstReturn extends LAScriterion
{
    public String name() { return "keep_first"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getReturn_number() > 1); };
};

class LAScriterionKeepFirstOfManyReturn extends LAScriterion
{
    public String name() { return "keep_first_of_many"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getNumber_of_returns() == 1) || (point.getReturn_number() > 1)); };
};

class LAScriterionKeepMiddleReturn extends LAScriterion
{
    public String name() { return "keep_middle"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getReturn_number() == 1) || (point.getReturn_number() >= point.getNumber_of_returns())); };
};

class LAScriterionKeepLastReturn extends LAScriterion
{
    public String name() { return "keep_last"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getReturn_number() < point.getNumber_of_returns()); };
};

class LAScriterionKeepLastOfManyReturn extends LAScriterion
{
    public String name() { return "keep_last_of_many"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getReturn_number() == 1) || (point.getReturn_number() < point.getNumber_of_returns())); };
};

class LAScriterionDropFirstReturn extends LAScriterion
{
    public String name() { return "drop_first"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getReturn_number() == 1); };
};

class LAScriterionDropFirstOfManyReturn extends LAScriterion
{
    public String name() { return "drop_first_of_many"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getNumber_of_returns() > 1) && (point.getReturn_number() == 1)); };
};

class LAScriterionDropMiddleReturn extends LAScriterion
{
    public String name() { return "drop_middle"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getReturn_number() > 1) && (point.getReturn_number() < point.getNumber_of_returns())); };
};

class LAScriterionDropLastReturn extends LAScriterion
{
    public String name() { return "drop_last"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getReturn_number() >= point.getNumber_of_returns()); };
};

class LAScriterionDropLastOfManyReturn extends LAScriterion
{
    public String name() { return "drop_last_of_many"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getNumber_of_returns() > 1) && (point.getReturn_number() >= point.getNumber_of_returns())); };
};

class LAScriterionKeepReturns extends LAScriterion
{
    public String name() { return "keep_return_mask"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), ~drop_return_mask); };
    public boolean filter(LASpoint point) { return ((1 << point.getReturn_number()) & drop_return_mask) != 0; };
    LAScriterionKeepReturns(int keep_return_mask) { drop_return_mask = ~keep_return_mask; };
    private int drop_return_mask; // unsigned
};

class LAScriterionKeepSpecificNumberOfReturns extends LAScriterion
{
    public String name() {
    String result;
    if (numberOfReturns == 1) {
        result = "keep_single";
    } else if (numberOfReturns == 2) {
        result = "keep_double";
    } else if (numberOfReturns == 3) {
        result = "keep_triple";
    } else if (numberOfReturns == 4) {
        result = "keep_quadruple";
    } else {
        result = "keep_quintuple";
    }
    return result;
}
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getNumber_of_returns() != numberOfReturns); };
    LAScriterionKeepSpecificNumberOfReturns(int numberOfReturns) { this.numberOfReturns = numberOfReturns; };
    private int numberOfReturns; // unsigned
};

class LAScriterionDropSpecificNumberOfReturns extends LAScriterion
{
    @Override
public String name() {
    String result;
    switch (numberOfReturns) {
        case 1:
            result = "drop_single";
            break;
        case 2:
            result = "drop_double";
            break;
        case 3:
            result = "drop_triple";
            break;
        case 4:
            result = "drop_quadruple";
            break;
        default:
            result = "drop_quintuple";
            break;
    }
    return result;
}    
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getNumber_of_returns() == numberOfReturns); };
    LAScriterionDropSpecificNumberOfReturns(int numberOfReturns) { this.numberOfReturns = numberOfReturns; };
    private int numberOfReturns; // unsigned
};

class LAScriterionDropScanDirection extends LAScriterion
{
    
    public String name() { return "drop_scan_direction"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), scan_direction); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (scan_direction == point.getScan_direction_flag()); };
    LAScriterionDropScanDirection(int scan_direction) { this.scan_direction = scan_direction; };
    private int scan_direction;
};

class LAScriterionKeepScanDirectionChange extends LAScriterion
{
    public String name() { return "keep_scan_direction_change"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { if (scan_direction_flag == point.getScan_direction_flag()) return TRUE; int s = scan_direction_flag; scan_direction_flag = point.getScan_direction_flag(); return s == -1; };
    @Override
    public void reset() { scan_direction_flag = -1; };
    LAScriterionKeepScanDirectionChange() { reset(); };
    @Override
    private int scan_direction_flag;
};

class LAScriterionKeepEdgeOfFlightLine extends LAScriterion
{
    public String name() { return "keep_edge_of_flight_line"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getEdge_of_flight_line() == 0); };
}

class LAScriterionKeepRGB extends LAScriterion
{
    @Override
    public String name() { return "keep_RGB"; };
public int get_Command(StringBuilder string) {
    String channelName;

    if (channel == 0) {
        channelName = "red";
    } else if (channel == 1) {
        channelName = "green";
    } else if (channel == 2) {
        channelName = "blue";
    } else {
        channelName = "nir";
    }

    return sprintf(string, "-%s_%s %d %d ", name(), channelName, below_RGB, above_RGB);
}
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_RGB; };
    public boolean filter(LASpoint point) { return ((point.getRgb(channel) < below_RGB) || (above_RGB < point.getRgb(channel))); };
    LAScriterionKeepRGB(int below_RGB, int above_RGB, int channel) { if (above_RGB < below_RGB) { this.below_RGB = above_RGB; this.above_RGB = below_RGB; } else { this.below_RGB = below_RGB; this.above_RGB = above_RGB; }; this.channel = channel; };
    @Override
    private int below_RGB, above_RGB, channel;
}

class LAScriterionKeepScanAngle extends LAScriterion
{
    public String name() { return "keep_scan_angle"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_scan, above_scan); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE; };
    public boolean filter(LASpoint point) { return (point.getScan_angle_rank() < below_scan) || (above_scan < point.getScan_angle_rank()); };
    LAScriterionKeepScanAngle(int below_scan, int above_scan) { if (above_scan < below_scan) { this.below_scan = above_scan; this.above_scan = below_scan; } else { this.below_scan = below_scan; this.above_scan = above_scan; } };
    @Override
    private int Below_scan;
    private int abOve_ScaN;
};

class LAScriterionDropScanAngleBelow extends LAScriterion
{
    public String name() { return "drop_scan_angle_below"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_scan); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE; };
    public boolean filter(LASpoint point) { return (point.getScan_angle_rank() < below_scan); };
    @Override
    LAScriterionDropScanAngleBelow(int below_scan) { this.below_scan = below_scan; };
    int below_scan;
};

class LAScriterionDropScanAngleAbove extends LAScriterion
{
    public String name() { return "drop_scan_angle_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_scan); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE; };
    @Override
    public boolean filter(LASpoint point) { return (point.getScan_angle_rank() > above_scan); };
    LAScriterionDropScanAngleAbove(int above_scan) { this.above_scan = above_scan; };
    int above_scan;
};

class LAScriterionDropScanAngleBetween extends LAScriterion
{
    public String name() { return "drop_scan_angle_between"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_scan, above_scan); };
    @Override
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE; };
    public boolean filter(LASpoint point) { return (below_scan <= point.getScan_angle_rank()) && (point.getScan_angle_rank() <= above_scan); };
    LAScriterionDropScanAngleBetween(int below_scan, int above_scan) { if (above_scan < below_scan) { this.below_scan = above_scan; this.above_scan = below_scan; } else { this.below_scan = below_scan; this.above_scan = above_scan; } };
    int below_scan, above_scan;
};

class LAScriterionKeepIntensity extends LAScriterion
{
    @Override
    public String name() { return "keep_intensity"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_intensity, above_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (point.getIntensity() < below_intensity) || (point.getIntensity() > above_intensity); };
    LAScriterionKeepIntensity(int below_intensity, int above_intensity) { this.below_intensity = below_intensity; this.above_intensity = above_intensity; };
    int below_intensity, above_intensity;
};

class LAScriterionKeepIntensityBelow extends LAScriterion
{
    public String name() { return "keep_intensity_below"; };
    @Override
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (point.getIntensity() >= below_intensity); };
    LAScriterionKeepIntensityBelow(int below_intensity) { this.below_intensity = below_intensity; };
    int below_intensity;
};

class LAScriterionKeepIntensityAbove extends LAScriterion
{
    @Override
    public String name() { return "keep_intensity_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (point.getIntensity() <= above_intensity); };
    LAScriterionKeepIntensityAbove(int above_intensity) { this.above_intensity = above_intensity; };
    int above_intensity;
};

class LAScriterionDropIntensityBelow extends LAScriterion
{
    @Override
    public String name() { return "drop_intensity_below"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (point.getIntensity() < below_intensity); };
    LAScriterionDropIntensityBelow(int below_intensity) { this.below_intensity = below_intensity; };
    int below_intensity;
};

class LAScriterionDropIntensityAbove extends LAScriterion
{
    @Override
    public String name() { return "drop_intensity_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (point.getIntensity() > above_intensity); };
    LAScriterionDropIntensityAbove(int above_intensity) { this.above_intensity = above_intensity; };
    int above_intensity;
};

class LAScriterionDropIntensityBetween extends LAScriterion
{
    @Override
    public String name() { return "drop_intensity_between"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_intensity, above_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    @Override
    public boolean filter(LASpoint point) { return (below_intensity <= point.getIntensity()) && (point.getIntensity() <= above_intensity); };
    LAScriterionDropIntensityBetween(int below_intensity, int above_intensity) { this.below_intensity = below_intensity; this.above_intensity = above_intensity; };
    int below_intensity, above_intensity;
};

class LAScriterionDropClassifications extends LAScriterion
{
    @Override
    public String name() { return "drop_classification_mask"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), drop_classification_mask); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_CLASSIFICATION; };
    public boolean filter(LASpoint point) { return ((1 << point.getClassification()) & drop_classification_mask) != 0; };
    LAScriterionDropClassifications(int drop_classification_mask) { this.drop_classification_mask = drop_classification_mask; };
    private int drop_classification_mask; // unsigned
};

class LAScriterionDropSynthetic extends LAScriterion
{
    @Override
    public String name() { return "drop_synthetic"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_synthetic_flag() == 1); };
};

class LAScriterionKeepSynthetic extends LAScriterion
{
    @Override
    public String name() { return "keep_synthetic"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    @Override
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_synthetic_flag() == 0); };
};

class LAScriterionDropKeypoint extends LAScriterion
{
    public String name() { return "drop_keypoint"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    @Override
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_keypoint_flag() == 1); };
};

class LAScriterionKeepKeypoint extends LAScriterion
{
    @Override
    public String name() { return "keep_keypoint"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_keypoint_flag() == 0); };
};

class LAScriterionDropWithheld extends LAScriterion
{
    @Override
    public String name() { return "drop_withheld"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_withheld_flag() == 1); };
};

class LAScriterionKeepWithheld extends LAScriterion
{
    @Override
    public String name() { return "keep_withheld"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_withheld_flag() == 0); };
};

class LAScriterionDropOverlap extends LAScriterion
{
    public String name() { return "drop_overlap"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    @Override
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_extended_overlap_flag() == 1); };
};

class LAScriterionKeepOverlap extends LAScriterion
{
    public String name() { return "keep_overlap"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    @Override
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_extended_overlap_flag() == 0); };
};

class LAScriterionKeepUserData extends LAScriterion
{
    public String name() { return "keep_user_data"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    @Override
    public boolean filter(LASpoint point) { return (point.getUser_data() != user_data); };
    LAScriterionKeepUserData(byte user_data) { this.user_data = user_data; };
    private byte user_data;
};

class LAScriterionKeepUserDataBelow extends LAScriterion
{
    public String name() { return "keep_user_data_below"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    @Override
    public boolean filter(LASpoint point) { return (point.getUser_data() >= below_user_data); };
    LAScriterionKeepUserDataBelow(byte below_user_data) { this.below_user_data = below_user_data; };
    private byte below_user_data;
};

class LAScriterionKeepUserDataAbove extends LAScriterion
{
    public String name() { return "keep_user_data_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_user_data); };
    @Override
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() <= above_user_data); };
    LAScriterionKeepUserDataAbove(byte above_user_data) { this.above_user_data = above_user_data; };
    private byte above_user_data;
};

class LAScriterionKeepUserDataBetween extends LAScriterion
{
    @Override
    public String name() { return "keep_user_data_between"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_user_data, above_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() < below_user_data) || (above_user_data < point.getUser_data()); };
    LAScriterionKeepUserDataBetween(byte below_user_data, byte above_user_data) { this.below_user_data = below_user_data; this.above_user_data = above_user_data; };
    private byte below_user_data, above_user_data;
};

class LAScriterionDropUserData extends LAScriterion
{
    @Override
    public String name() { return "drop_user_data"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() == user_data); };
    LAScriterionDropUserData(byte user_data) { this.user_data = user_data; };
    private byte user_data;
};

class LAScriterionDropUserDataBelow extends LAScriterion
{
    @Override
    public String name() { return "drop_user_data_below"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() < below_user_data) ; };
    LAScriterionDropUserDataBelow(byte below_user_data) { this.below_user_data = below_user_data; };
    private byte below_user_data;
};

class LAScriterionDropUserDataAbove extends LAScriterion
{
    @Override
    public String name() { return "drop_user_data_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() > above_user_data); };
    LAScriterionDropUserDataAbove(byte above_user_data) { this.above_user_data = above_user_data; };
    private byte above_user_data;
};

class LAScriterionDropUserDataBetween extends LAScriterion
{
    @Override
    public String name() { return "drop_user_data_between"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_user_data, above_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (below_user_data <= point.getUser_data()) && (point.getUser_data() <= above_user_data); };
    LAScriterionDropUserDataBetween(byte below_user_data, byte above_user_data) { this.below_user_data = below_user_data; this.above_user_data = above_user_data; };
    private byte below_user_data, above_user_data;
};

class LAScriterionKeepPointSource extends LAScriterion
{
    public String name() { return "keep_point_source"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), point_source_id); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; };
    public boolean filter(LASpoint point) { return (point.getPoint_source_ID() != point_source_id); };
    @Override
    LAScriterionKeepPointSource(char point_source_id) { this.point_source_id = point_source_id; };
    private char point_source_id;
};

class LAScriterionKeepPointSourceBetween extends LAScriterion
{
    public String name() { return "keep_point_source_between"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_point_source_id, above_point_source_id); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; };
    @Override
    public boolean filter(LASpoint point) { return (point.getPoint_source_ID() < below_point_source_id) || (above_point_source_id < point.getPoint_source_ID()); };
    LAScriterionKeepPointSourceBetween(char below_point_source_id, char above_point_source_id) { this.below_point_source_id = below_point_source_id; this.above_point_source_id = above_point_source_id; };
    private char below_point_source_id, above_point_source_id;
};

class LAScriterionDropPointSource extends LAScriterion
{
    public String name() { return "drop_point_source"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), point_source_id); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; };
    @Override
    public boolean filter(LASpoint point) { return (point.getPoint_source_ID() == point_source_id) ; };
    LAScriterionDropPointSource(char point_source_id) { this.point_source_id = point_source_id; };
    private char point_source_id;
};

class LAScriterionDropPointSourceBelow extends LAScriterion
{
    public String name() { return "drop_point_source_below"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_point_source_id); };
    @Override
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; };
    public boolean filter(LASpoint point) { return (point.getPoint_source_ID() < below_point_source_id) ; };
    LAScriterionDropPointSourceBelow(char below_point_source_id) { this.below_point_source_id = below_point_source_id; };
    private char below_point_source_id;
};

class LAScriterionDropPointSourceAbove extends LAScriterion
{
    public String name() { return "drop_point_source_above"; };
    @Override
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_point_source_id); };
    public boolean filter(LASpoint point) { return (point.getPoint_source_ID() > above_point_source_id); };
    LAScriterionDropPointSourceAbove(char above_point_source_id) { this.above_point_source_id = above_point_source_id; };
    private char above_point_source_id;
};

class LAScriterionDropPointSourceBetween extends LAScriterion
{
    public String name() { return "drop_point_source_between"; };
    @Override
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_point_source_id, above_point_source_id); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; };
    public boolean filter(LASpoint point) { return (below_point_source_id <= point.getPoint_source_ID()) && (point.getPoint_source_ID() <= above_point_source_id); };
    LAScriterionDropPointSourceBetween(char below_point_source_id, char above_point_source_id) { this.below_point_source_id = below_point_source_id; this.above_point_source_id = above_point_source_id; };
    private char below_point_source_id, above_point_source_id;
};

class LAScriterionKeepGpsTime extends LAScriterion
{
    @Override
    public String name() { return "keep_gps_time"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %.6f %.6f ", name(), below_gpstime, above_gpstime); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME; };
    public boolean filter(LASpoint point) { return (point.haveGpsTime() && ((point.getGps_time() < below_gpstime) || (point.getGps_time() > above_gpstime))); };
    LAScriterionKeepGpsTime(double below_gpstime, double above_gpstime) { this.below_gpstime = below_gpstime; this.above_gpstime = above_gpstime; };
    double below_gpstime, above_gpstime;
};

class LAScriterionDropGpsTimeBelow extends LAScriterion
{
    public String name() { return "drop_gps_time_below"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %.6f ", name(), below_gpstime); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME; };
    public boolean filter(LASpoint point) { return (point.haveGpsTime() && (point.getGps_time() < below_gpstime)); };
    LAScriterionDropGpsTimeBelow(double below_gpstime) { this.below_gpstime = below_gpstime; };
    double below_gpstime;
};

class LAScriterionDropGpsTimeAbove extends LAScriterion
{
    public String name() { return "drop_gps_time_above"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %.6f ", name(), above_gpstime); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME; };
    public boolean filter(LASpoint point) { return (point.haveGpsTime() && (point.getGps_time() > above_gpstime)); };
    LAScriterionDropGpsTimeAbove(double above_gpstime) { this.above_gpstime = above_gpstime; };
    double above_gpstime;
};

class LAScriterionDropGpsTimeBetween extends LAScriterion
{
    public String name() { return "drop_gps_time_between"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %.6f %.6f ", name(), below_gpstime, above_gpstime); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME; };
    public boolean filter(LASpoint point) { return (point.haveGpsTime() && ((below_gpstime <= point.getGps_time()) && (point.getGps_time() <= above_gpstime))); };
    LAScriterionDropGpsTimeBetween(double below_gpstime, double above_gpstime) { this.below_gpstime = below_gpstime; this.above_gpstime = above_gpstime; };
    double below_gpstime, above_gpstime;
};

class LAScriterionKeepWavepacket extends LAScriterion
{
    public String name() { return "keep_wavepacket"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), keep_wavepacket); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_WAVEPACKET; };
    public boolean filter(LASpoint point) { return (point.getWavepacketDescriptorIndex() != keep_wavepacket); };
    LAScriterionKeepWavepacket(int keep_wavepacket) { this.keep_wavepacket = keep_wavepacket; };
    int keep_wavepacket; // unsigned
};

class LAScriterionDropWavepacket extends LAScriterion
{
    public String name() { return "drop_wavepacket"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), drop_wavepacket); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_WAVEPACKET; };
    public boolean filter(LASpoint point) { return (point.getWavepacketDescriptorIndex() == drop_wavepacket); };
    LAScriterionDropWavepacket(int drop_wavepacket) { this.drop_wavepacket = drop_wavepacket; };
    int drop_wavepacket;
};

class LAScriterionKeepEveryNth extends LAScriterion
{
    public String name() { return "keep_every_nth"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), every); };
    public boolean filter(LASpoint point) { if (counter == every) { counter = 1; return FALSE; } else { counter++; return TRUE; } };
    LAScriterionKeepEveryNth(int every) { this.every = every; counter = 1; };
    int counter;
    int every;
};

class LAScriterionKeepRandomFraction extends LAScriterion
{
    public String name() { return "keep_random_fraction"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), fraction); };
    public boolean filter(LASpoint point)
    {
        srand(seed);
        seed = rand();
        return ((float)seed/(float)RAND_MAX) > fraction;
    };
    @Override
    public void reset() { seed = 0; };
    LAScriterionKeepRandomFraction(float fraction) { seed = 0; this.fraction = fraction; };
    int seed;
    float fraction;
};

class LAScriterionThinWithGrid extends LAScriterion
{
    public String name() { return "thin_with_grid"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), (grid_spacing > 0 ? grid_spacing : -grid_spacing)); };
    public boolean filter(LASpoint point)
    {
        return FALSE;
        
    }
    @Override
    public void reset()
    {
        if (grid_spacing > 0) grid_spacing = -grid_spacing;
        if (minus_minus_size != 0)
        {
            minus_minus = null;
            minus_minus_sizes = null;
            minus_minus_size = 0;
        }
        if (minus_plus_size != 0)
        {
            minus_ankers = null;
            minus_plus = null;
            minus_plus_sizes = null;
            minus_plus_size = 0;
        }
        if (plus_minus_size != 0)
        {
            plus_minus = null;
            plus_minus_sizes = null;
            plus_minus_size = 0;
        }
        if (plus_plus_size != 0)
        {
            plus_ankers = null;
            plus_plus = null;
            plus_plus_sizes = null;
            plus_plus_size = 0;
        }
    };
    LAScriterionThinWithGrid(float grid_spacing)
    {
        this.grid_spacing = -grid_spacing;
        minus_ankers = null;
        minus_minus_size = 0;
        minus_minus = null;
        minus_minus_sizes = null;
        minus_plus_size = 0;
        minus_plus = null;
        minus_plus_sizes = null;
        plus_ankers = null;
        plus_minus_size = 0;
        plus_minus = null;
        plus_minus_sizes = null;
        plus_plus_size = 0;
        plus_plus = null;
        plus_plus_sizes = null;
    };

    private float grid_spacing;
    private int anker;
    private int[] minus_ankers;
    private int minus_minus_size; // unsigned
    private int[][] minus_minus; // unsigned
    private char[] minus_minus_sizes;
    private int minus_plus_size; // unsigned
    private int[][] minus_plus; // unsigned
    private char[] minus_plus_sizes;
    private int[] plus_ankers;
    private int plus_minus_size; // unsigned
    private int[][] plus_minus; // unsigned
    private char[] plus_minus_sizes;
    private int plus_plus_size; // unsigned
    private int[][] plus_plus; // unsigned
    private char[] plus_plus_sizes;
};

class LAScriterionThinWithTime extends LAScriterion
{
    public String name() { return "thin_with_time"; };
    public int get_Command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), (time_spacing > 0 ? time_spacing : -time_spacing)); };
    public boolean filter(LASpoint point)
    {
        long pos_t = I64_FLOOR(point.get_gps_time() / time_spacing);
        List<Double> map_element = times.get(pos_t);
        if (map_element == null)
        {
            map_element = new ArrayList<>();
            map_element.add(point.get_gps_time());
            times.put(pos_t, map_element);
            return FALSE;
        }
        else if (map_element.get(0) == point.get_gps_time())
        {
            return FALSE;
        }
            else
        {
            return TRUE;
        }
    }
    @Override
    public void reset()
    {
        times.clear();
    };
    LAScriterionThinWithTime(double time_spacing)
    {
        this.time_spacing = time_spacing;
    };
    double time_spacing;
    SortedMap<Long, List<Double>> times = new TreeMap<>();
};
