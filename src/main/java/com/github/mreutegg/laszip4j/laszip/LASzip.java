/*
 * Copyright 2007-2013, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.BYTE;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.BYTE14;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.GPSTIME11;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.POINT10;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.POINT14;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.RGB12;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.RGB14;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.RGBNIR14;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.WAVEPACKET13;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.WAVEPACKET14;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASzip {

    private static final PrintStream stderr = System.err;

    public static final int LASZIP_VERSION_MAJOR                = 3;
    public static final int LASZIP_VERSION_MINOR                = 4;
    public static final int LASZIP_VERSION_REVISION             = 3;
    public static final int LASZIP_VERSION_BUILD_DATE      = 191111;

    public static final char LASZIP_COMPRESSOR_NONE              = 0;
    public static final char LASZIP_COMPRESSOR_POINTWISE         = 1;
    public static final char LASZIP_COMPRESSOR_POINTWISE_CHUNKED = 2;
    public static final char LASZIP_COMPRESSOR_LAYERED_CHUNKED   = 3;
    public static final char LASZIP_COMPRESSOR_TOTAL_NUMBER_OF   = 4;

    public static final char LASZIP_COMPRESSOR_CHUNKED = LASZIP_COMPRESSOR_POINTWISE_CHUNKED;
    public static final char LASZIP_COMPRESSOR_NOT_CHUNKED = LASZIP_COMPRESSOR_POINTWISE;

    public static final char LASZIP_COMPRESSOR_DEFAULT = LASZIP_COMPRESSOR_CHUNKED;

    public static final int LASZIP_CODER_ARITHMETIC             = 0;
    public static final int LASZIP_CODER_TOTAL_NUMBER_OF        = 1;

    public static final int LASZIP_CHUNK_SIZE_DEFAULT           = 50000;

    public static final int LASZIP_DECOMPRESS_SELECTIVE_ALL                = 0xFFFFFFFF;

    public static final int LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY = 0x00000000;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_Z                  = 0x00000001;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_CLASSIFICATION     = 0x00000002;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_FLAGS              = 0x00000004;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_INTENSITY          = 0x00000008;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE         = 0x00000010;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_USER_DATA          = 0x00000020;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE       = 0x00000040;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME           = 0x00000080;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_RGB                = 0x00000100;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_NIR                = 0x00000200;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_WAVEPACKET         = 0x00000400;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_BYTE0              = 0x00010000;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_BYTE1              = 0x00020000;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_BYTE2              = 0x00040000;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_BYTE3              = 0x00080000;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_BYTE4              = 0x00100000;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_BYTE5              = 0x00200000;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_BYTE6              = 0x00400000;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_BYTE7              = 0x00800000;
    public static final int LASZIP_DECOMPRESS_SELECTIVE_EXTRA_BYTES        = 0xFFFF0000;
        
    // pack to and unpack from VLR
    byte[] bytes; // unsigned

    // stored in LASzip VLR data section
    public char compressor;
    public char coder;
    public byte version_major; // unsigned
    public byte version_minor; // unsigned
    public char version_revision;
    public int options; // unsigned
    public int chunk_size; // unsigned
    public long number_of_special_evlrs; /* must be -1 if unused */
    public long offset_to_special_evlrs; /* must be -1 if unused */
    public char num_items;
    public LASitem[] items;

    private String error_string;

    public LASzip()
    {
        compressor = LASZIP_COMPRESSOR_DEFAULT;
        coder = LASZIP_CODER_ARITHMETIC;
        version_major = LASZIP_VERSION_MAJOR;
        version_minor = LASZIP_VERSION_MINOR;
        version_revision = LASZIP_VERSION_REVISION;
        options = 0;
        num_items = 0;
        chunk_size = LASZIP_CHUNK_SIZE_DEFAULT;
        number_of_special_evlrs = -1;
        offset_to_special_evlrs = -1;
        error_string = null;
        items = null;
        bytes = null;
    }

    // unpack from VLR data
    boolean unpack(byte[] bytes, int num)
    {
        // check input
        if (num < 34) return return_error("too few bytes to unpack");
        if (((num - 34) % 6) != 0) return return_error("wrong number bytes to unpack");
        if (((num - 34) / 6) == 0) return return_error("zero items to unpack");
        num_items = (char) ((num - 34) / 6);

        // create item list
        items = new LASitem[num_items];

        // do the unpacking
        int i;
        int b = 0;
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        compressor = buffer.getChar(b);
        b += 2;
        coder = buffer.getChar(b);
        b += 2;
        version_major = buffer.get(b);
        b += 1;
        version_minor = buffer.get(b);
        b += 1;
        version_revision = buffer.getChar(b);
        b += 2;
        options = buffer.getInt(b);
        b += 4;
        chunk_size = buffer.getInt(b);
        b += 4;
        number_of_special_evlrs = buffer.getLong(b);
        b += 8;
        offset_to_special_evlrs = buffer.getLong(b);
        b += 8;
        num_items = buffer.getChar(b);
        b += 2;
        for (i = 0; i < num_items; i++)
        {
            items[i].type = LASitem.Type.fromOrdinal(buffer.getChar(b));
            b += 2;
            items[i].size = buffer.getChar(b);
            b += 2;
            items[i].version = buffer.getChar(b);
            b += 2;
        }
        assert(num == b);

        // check if we support the contents

        for (i = 0; i < num_items; i++)
        {
            if (!check_item(items[i])) return false;
        }
        return true;
    }

    // pack to VLR data
    boolean pack(byte[][] bytes, int[] num)
    {
        // check if we support the contents
        if (!check()) return false;

        // prepare output
        num[0] = 34 + 6*num_items;
        this.bytes = bytes[0] = new byte[num[0]];

        // pack
        int i;
        int b = 0;
        ByteBuffer buffer = ByteBuffer.wrap(bytes[0]).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putChar(b, compressor);
        b += 2;
        buffer.putChar(b, coder);
        b += 2;
        buffer.put(b, version_major);
        b += 1;
        buffer.put(b, version_minor);
        b += 1;
        buffer.putChar(b, version_revision);
        b += 2;
        buffer.putInt(b, options);
        b += 4;
        buffer.putInt(b, chunk_size);
        b += 4;
        buffer.putLong(b, number_of_special_evlrs);
        b += 8;
        buffer.putLong(b, offset_to_special_evlrs);
        b += 8;
        buffer.putChar(b, num_items);
        b += 2;
        for (i = 0; i < num_items; i++)
        {
            buffer.putChar(b, (char) items[i].type.ordinal());
            b += 2;
            buffer.putChar(b, items[i].size);
            b += 2;
            buffer.putChar(b, items[i].version);
            b += 2;
        }
        assert(num[0] == b);
        return true;
    }

    public String get_error()
    {
        return error_string;
    }

    boolean return_error(String error)
    {
        error_string = String.format("%s (LASzip v%d.%dr%d)", error, LASZIP_VERSION_MAJOR, LASZIP_VERSION_MINOR, LASZIP_VERSION_REVISION);
        return false;
    }

    boolean check_compressor(char compressor)
    {
        if (compressor < LASZIP_COMPRESSOR_TOTAL_NUMBER_OF) return true;
        String error = String.format("compressor %d not supported", (int) compressor);
        return return_error(error);
    }

    boolean check_coder(char coder)
    {
        if (coder < LASZIP_CODER_TOTAL_NUMBER_OF) return true;
        String error = String.format("coder %d not supported", (int) coder);
        return return_error(error);
    }

    boolean check_item(LASitem item) {
    int size = (int) item.size;
    int version = (int) item.version;

    switch (item.type) {
        case POINT10:
            return (size == 20 && version <= 2);
        case GPSTIME11:
            return (size == 8 && version <= 2);
        case RGB12:
            return (size == 6 && version <= 2);
        case WAVEPACKET13:
            return (size == 29 && version <= 1);
        case BYTE:
            return (size >= 1 && version <= 2);
        case POINT14:
            return (size == 30 && (version == 0 || version == 2 || version == 3 || version == 4));
        case RGB14:
            return (size == 6 && (version == 0 || version == 2 || version == 3 || version == 4));
        case BYTE14:
            return (size >= 1 && (version == 0 || version == 2 || version == 3 || version == 4));
        case RGBNIR14:
            return (size == 8 && (version == 0 || version == 2 || version == 3 || version == 4));
        case WAVEPACKET14:
            return (size == 29 && (version == 0 || version == 3 || version == 4));
        default:
            String error = String.format("item unknown (%d,%d,%d)", item.type.ordinal(), size, version);
            return return_error(error);
    }
}


    boolean check_items(char num_items, LASitem[] items)
    {
        if (num_items == 0) return return_error("number of items cannot be zero");
        if (items == null) return return_error("items pointer cannot be NULL");
        int i;
        for (i = 0; i < num_items; i++)
        {
            if (!check_item(items[i])) return false;
        }
        return true;
    }

    public boolean check()
    {
        if (!check_compressor(compressor)) return false;
        if (!check_coder(coder)) return false;
        if (!check_items(num_items, items)) return false;
        return true;
    }

    boolean request_compatibility_mode(char requested_compatibility_mode)
    {
        if (num_items != 0) return return_error("request compatibility mode before calling setup()");
        if (requested_compatibility_mode > 1)
        {
            return return_error("compatibility mode larger than 1 not supported");
        }
        if (requested_compatibility_mode != 0)
        {
            options = options | 0x00000001;
        }
        else
        {
            options = options & 0xFFFFFFFE;
        }
        return true;
    }

    boolean setup(byte u_point_type, char point_size, char compressor)
    {
        if (!check_compressor(compressor)) return false;

        this.num_items = 0;
        this.items = null;
        char[] _num_items = new char[1];
        LASitem[][] _items = new LASitem[1][];
        if (!setup(_num_items, _items, u_point_type, point_size, compressor)) return false;
        this.num_items = _num_items[0];
        this.items = _items[0];

        this.compressor = compressor;
        if (this.compressor == LASZIP_COMPRESSOR_POINTWISE_CHUNKED)
        {
            if (chunk_size == 0) chunk_size = LASZIP_CHUNK_SIZE_DEFAULT;
        }
        return true;
    }

    public boolean setup(char num_items, LASitem[] items, char compressor)
    {
        // check input
        if (!check_compressor(compressor)) return false;
        if (!check_items(num_items, items)) return false;

        // setup compressor
        this.compressor = compressor;
        if (this.compressor == LASZIP_COMPRESSOR_POINTWISE_CHUNKED)
        {
            if (chunk_size == 0) chunk_size = LASZIP_CHUNK_SIZE_DEFAULT;
        }

        // prepare items
        this.num_items = 0;
        this.num_items = num_items;
        this.items = new LASitem[num_items];

        // setup items
        int i;
        for (i = 0; i < this.items.length; i++)
        {
            this.items[i] = items[i];
        }

        return true;
    }

    boolean setup(char[] num_items, LASitem[][] items, byte u_point_type, char point_size, char compressor)
    {
        boolean compatible = FALSE;
        boolean have_point14 = FALSE;
        boolean have_gps_time = FALSE;
        boolean have_rgb = FALSE;
        boolean have_nir = FALSE;
        boolean have_wavepacket = FALSE;
        int extra_bytes_number = 0;

        // turns on LAS 1.4 compatibility mode 

        if ((options & 1) != 0) compatible = TRUE;

        // switch over the point types we know
        switch (u_point_type)
        {
            case 0:
                extra_bytes_number = (int)point_size - 20;
                break;
            case 1:
                have_gps_time = TRUE;
                extra_bytes_number = (int)point_size - 28;
                break;
            case 2:
                have_rgb = TRUE;
                extra_bytes_number = (int)point_size - 26;
                break;
            case 3:
                have_gps_time = TRUE;
                have_rgb = TRUE;
                extra_bytes_number = (int)point_size - 34;
                break;
            case 4:
                have_gps_time = TRUE;
                have_wavepacket = TRUE;
                extra_bytes_number = (int)point_size - 57;
                break;
            case 5:
                have_gps_time = TRUE;
                have_rgb = TRUE;
                have_wavepacket = TRUE;
                extra_bytes_number = (int)point_size - 63;
                break;
            case 6:
                have_point14 = TRUE;
                extra_bytes_number = (int)point_size - 30;
                break;
            case 7:
                have_point14 = TRUE;
                have_rgb = TRUE;
                extra_bytes_number = (int)point_size - 36;
                break;
            case 8:
                have_point14 = TRUE;
                have_rgb = TRUE;
                have_nir = TRUE;
                extra_bytes_number = (int)point_size - 38;
                break;
            case 9:
                have_point14 = TRUE;
                have_wavepacket = TRUE;
                extra_bytes_number = (int)point_size - 59;
                break;
            case 10:
                have_point14 = TRUE;
                have_rgb = TRUE;
                have_nir = TRUE;
                have_wavepacket = TRUE;
                extra_bytes_number = (int)point_size - 67;
                break;
            default:
                {
                    String error = String.format("point type %d unknown", u_point_type);
                    return return_error(error);
                }
        }

        if (extra_bytes_number < 0)
        {
            //    char error[64];
            //    sprintf(error, "point size %d too small for point type %d by %d bytes", point_size, point_type, -extra_bytes_number);
            //    return return_error(error);
            fprintf(stderr, "WARNING: point size %d too small by %d bytes for point type %d. assuming point_size of %d\n", point_size, -extra_bytes_number, u_point_type, point_size-extra_bytes_number);
            extra_bytes_number = 0;
        }

        // maybe represent new LAS 1.4 as corresponding LAS 1.3 points plus extra bytes for compatibility
        if (have_point14 && compatible)
        {
            // we need 4 extra bytes for the new point attributes
            extra_bytes_number += 5;
            // we store the GPS time separately
            have_gps_time = TRUE;
            // we do not use the point14 item
            have_point14 = FALSE;
            // if we have NIR ...
            if (have_nir)
            {
                // we need another 2 extra bytes 
                extra_bytes_number += 2;
                // we do not use the NIR item
                have_nir = FALSE;
            }
        }

        // create item description

        num_items[0] = (char) (1 + asInt(have_gps_time) + asInt(have_rgb) + asInt(have_wavepacket) + asInt(extra_bytes_number != 0));
        items[0] = new LASitem[num_items[0]];
        for (int i = 0; i < items[0].length; i++) {
            items[0][i] = new LASitem();
        }

        int i = 1;
        if (have_point14)
        {
            items[0][0].type = POINT14;
            items[0][0].size = 30;
            items[0][0].version = 0;
        }
        else
        {
            items[0][0].type = POINT10;
            items[0][0].size = 20;
            items[0][0].version = 0;
        }
        if (have_gps_time)
        {
            items[0][i].type = GPSTIME11;
            items[0][i].size = 8;
            items[0][i].version = 0;
            i++;
        }
        if (have_rgb)
        {
          if (have_point14)
            {
            if (have_nir)
            {
              items[0][i].type = RGBNIR14;
              items[0][i].size = 8;
              items[0][i].version = 0;
            }
                else
            {
              items[0][i].type = RGB14;
              items[0][i].size = 6;
              items[0][i].version = 0;
            }
          }
          else
          {
            items[0][i].type = RGB12;
            items[0][i].size = 6;
            items[0][i].version = 0;
          }
          i++;
        }
        if (have_wavepacket)
        {
          if (have_point14)
            {
            items[0][i].type = WAVEPACKET14;
            items[0][i].size = 29;
            items[0][i].version = 0;
          }
          else
            {
            items[0][i].type = WAVEPACKET13;
            items[0][i].size = 29;
            items[0][i].version = 0;
          }
          i++;
        }
        if (extra_bytes_number != 0)
        {
          if (have_point14)
            {
            items[0][i].type = BYTE14;
            items[0][i].size = (char)extra_bytes_number;
            items[0][i].version = 0;
          }
          else
            {
            items[0][i].type = BYTE;
            items[0][i].size = (char)extra_bytes_number;
            items[0][i].version = 0;
          }
          i++;
        }
      
        if (compressor != 0) request_version((char) 2);
        assert(i == num_items[0]);
        return true;
    }

    public boolean set_chunk_size(int u_chunk_size)
    {
        if (num_items == 0) return return_error("call setup() before setting chunk size");
        if (this.compressor == LASZIP_COMPRESSOR_POINTWISE_CHUNKED)
        {
            this.chunk_size = u_chunk_size;
            return true;
        }
        return false;
    }

    public boolean request_version(char requested_version)
    {
        if (num_items == 0) return return_error("call setup() before requesting version");
        if (compressor == LASZIP_COMPRESSOR_NONE)
        {
            if (requested_version > 0) return return_error("without compression version is always 0");
        }
        else
        {
            if (requested_version < 1) return return_error("with compression version is at least 1");
            if (requested_version > 4) return return_error("version larger than 4 not supported");
        }
        char i;
        for (i = 0; i < num_items; i++)
        {
            switch (items[i].type)
            {
                case POINT10:
                case POINT14:
                case GPSTIME11:
                case RGB12:
                case RGB14:
                case BYTE:
                case BYTE14:
                case WAVEPACKET14:
                    items[i].version = requested_version;
                    break;
                case WAVEPACKET13:
                    items[i].version = 1; // no version 2
                    break;
                default:
                    return return_error("item type not supported");
            }
        }
        return true;
    }

    public boolean is_standard(byte[] point_type, char[] record_length)
    {
        return is_standard(num_items, items, point_type, record_length);
    }

   public void updateHeader(LASheader header, boolean useInventory, boolean updateExtraBytes) throws Exception {
    if (header == null) {
        throw new IllegalArgumentException("ERROR: header pointer is null");
    }

    if (stream == null) {
        throw new IllegalArgumentException("ERROR: stream pointer is null");
    }

    if (!stream.isSeekable()) {
        System.out.println("WARNING: stream not seekable. cannot update header.");
        return;
    }

    if (useInventory) {
        updateInventory(header);
    } else {
        updateHeaderValues(header);
    }

    stream.seekEnd();

    if (updateExtraBytes) {
        // Code for updating extra bytes
    }
}

private void updateInventory(LASheader header) throws Exception {
    if (header.version_minor >= 4 && inventory.extended_number_of_point_records > toUnsignedLong(U32_MAX)) {
        throw new Exception("WARNING: too many points in LAS file. limit is " + toUnsignedLong(U32_MAX));
    }

    stream.seek(header_start_position + 107);
    stream.put32bitsLE(getUpdatedNumber(inventory.extended_number_of_point_records, header.version_minor >= 4));
    npoints = inventory.extended_number_of_point_records;

    for (int i = 0; i < 5; i++) {
        stream.put32bitsLE(getUpdatedNumber(inventory.extended_number_of_points_by_return[i + 1], header.version_minor >= 4));
    }

    // Update other values in inventory
}

private void updateHeaderValues(LASheader header) throwsException {
    int number = header.number_of_point_records;

    if (header.point_data_format >= 6) {
        number = 0;
    }

    stream.seek(header_start_position + 107);
    stream.put32bitsLE(number);
    npoints = number;

    for (int i = 0; i < 5; i++) {
        number = header.number_of_points_by_return[i];
        if (header.point_data_format >= 6) {
            number = 0;
        }
        stream.put32bitsLE(number);
    }

    // Update other values in header

    if (header.version_minor >= 3) {
        // Handle LAS 1.3 or higher
    }

    if (header.version_minor >= 4) {
        // Handle LAS 1.4 or higher
    }
}

private int getUpdatedNumber(long value, boolean condition) {
    if (condition) {
        return 0;
    }
    return (int) value;
}


    private static int asInt(boolean b) {
        return b ? 1 : 0;
    }

}
