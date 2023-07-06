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
package com.github.mreutegg.laszip4j.laszip;

public class LASreadItemCompressed_POINT10_v1 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private PointDataRecordPoint10 last_item = null;

    private int[] last_x_diff = new int[3];
    private int[] last_y_diff = new int[3];
    private int last_incr;
    private IntegerCompressor ic_dx;
    private IntegerCompressor ic_dy;
    private IntegerCompressor ic_z;
    private IntegerCompressor ic_intensity;
    private IntegerCompressor ic_scan_angle_rank;
    private IntegerCompressor ic_point_source_ID;
    private ArithmeticModel m_changed_values;
    private ArithmeticModel[] m_bit_byte = new ArithmeticModel[256];
    private ArithmeticModel[] m_classification = new ArithmeticModel[256];
    private ArithmeticModel[] m_user_data = new ArithmeticModel[256];
    
    public LASreadItemCompressed_POINT10_v1(ArithmeticDecoder dec) {
        int i;

        /* set decoder */
        assert(dec != null);
        this.dec = dec;

        /* create models and integer compressors */
        ic_dx = new IntegerCompressor(dec, 32);  // 32 bits, 1 context
        ic_dy = new IntegerCompressor(dec, 32, 20); // 32 bits, 20 contexts
        ic_z = new IntegerCompressor(dec, 32, 20);  // 32 bits, 20 contexts
        ic_intensity = new IntegerCompressor(dec, 16);
        ic_scan_angle_rank = new IntegerCompressor(dec, 8, 2);
        ic_point_source_ID = new IntegerCompressor(dec, 16);
        m_changed_values = dec.createSymbolModel(64);
        for (i = 0; i < 256; i++)
        {
            m_bit_byte[i] = null;
            m_classification[i] = null;
            m_user_data[i] = null;
        }
    }

    @Override
    public void init(PointDataRecord seedItem, int notUsed) {

        int i;

        /* init state */
        last_x_diff[0] = last_x_diff[1] = last_x_diff[2] = 0;
        last_y_diff[0] = last_y_diff[1] = last_y_diff[2] = 0;
        last_incr = 0;

        /* init models and integer compressors */
        ic_dx.initDecompressor();
        ic_dy.initDecompressor();
        ic_z.initDecompressor();
        ic_intensity.initDecompressor();
        ic_scan_angle_rank.initDecompressor();
        ic_point_source_ID.initDecompressor();
        dec.initSymbolModel(m_changed_values);
        for (i = 0; i < 256; i++)
        {
            if (m_bit_byte[i] != null) dec.initSymbolModel(m_bit_byte[i]);
            if (m_classification[i] != null) dec.initSymbolModel(m_classification[i]);
            if (m_user_data[i] != null) dec.initSymbolModel(m_user_data[i]);
        }

        last_item = new PointDataRecordPoint10((PointDataRecordPoint10)seedItem);
    }

    @Override
   public PointDataRecord read(int notUsed) {
    int median_x = findMedian(last_x_diff);
    int median_y = findMedian(last_y_diff);

    int x_diff = ic_dx.decompress(median_x);
    last_item.X += x_diff;

    int k_bits = ic_dx.getK();
    int y_diff = ic_dy.decompress(median_y, Math.min(k_bits, 19));
    last_item.Y += y_diff;
    k_bits = (k_bits + ic_dy.getK()) / 2;
    last_item.Z = ic_z.decompress((int) last_item.Z, Math.min(k_bits, 19));

    int changed_values = dec.decodeSymbol(m_changed_values);

    if (changed_values != 0) {
        if ((changed_values & 32) != 0) {
            last_item.Intensity = (char) ic_intensity.decompress(last_item.Intensity);
        }

        if ((changed_values & 16) != 0) {
            initializeSymbolModelIfNull(m_bit_byte, last_item.Flags, 256);
            last_item.Flags = (byte) dec.decodeSymbol(m_bit_byte[last_item.Flags]);
        }

        if ((changed_values & 8) != 0) {
            initializeSymbolModelIfNull(m_classification, last_item.Classification, 256);
            last_item.Classification = (short) dec.decodeSymbol(m_classification[last_item.Classification]);
        }

        if ((changed_values & 4) != 0) {
            last_item.ScanAngleRank = (byte) ic_scan_angle_rank.decompress(last_item.ScanAngleRank, Math.min(k_bits, 3));
        }

        if ((changed_values & 2) != 0) {
            initializeSymbolModelIfNull(m_user_data, last_item.UserData, 256);
            last_item.UserData = (short) dec.decodeSymbol(m_user_data[last_item.UserData]);
        }

        if ((changed_values & 1) != 0) {
            last_item.PointSourceID = (char) ic_point_source_ID.decompress(last_item.PointSourceID);
        }
    }

    last_x_diff[last_incr] = x_diff;
    last_y_diff[last_incr] = y_diff;
    last_incr = (last_incr + 1) % 3;

    PointDataRecordPoint10 result = new PointDataRecordPoint10(last_item);
    return result;
}

private int findMedian(int[] array) {
    Arrays.sort(array);
    return array[1];
}

private void initializeSymbolModelIfNull(Object[] array, int index, int size) {
    if (array[index] == null) {
        array[index] = dec.createSymbolModel(size);
        dec.initSymbolModel(array[index]);
    }
}


    @Override	
    public boolean chunk_sizes() {	
        return false;	
    }
}
