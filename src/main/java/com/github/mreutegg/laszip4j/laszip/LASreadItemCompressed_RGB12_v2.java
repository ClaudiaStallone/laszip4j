/*
 * Copyright 2007-2012, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import static com.github.mreutegg.laszip4j.laszip.MyDefs.U8_CLAMP;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U8_FOLD;

public class LASreadItemCompressed_RGB12_v2 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private PointDataRecordRGB last_item = null;

    private ArithmeticModel m_byte_used;
    private ArithmeticModel m_rgb_diff_0;
    private ArithmeticModel m_rgb_diff_1;
    private ArithmeticModel m_rgb_diff_2;
    private ArithmeticModel m_rgb_diff_3;
    private ArithmeticModel m_rgb_diff_4;
    private ArithmeticModel m_rgb_diff_5;

    public LASreadItemCompressed_RGB12_v2(ArithmeticDecoder dec)
    {
        /* set decoder */
        assert(dec != null);
        this.dec = dec;

        /* create models and integer compressors */
        m_byte_used = dec.createSymbolModel(128);
        m_rgb_diff_0 = dec.createSymbolModel(256);
        m_rgb_diff_1 = dec.createSymbolModel(256);
        m_rgb_diff_2 = dec.createSymbolModel(256);
        m_rgb_diff_3 = dec.createSymbolModel(256);
        m_rgb_diff_4 = dec.createSymbolModel(256);
        m_rgb_diff_5 = dec.createSymbolModel(256);
    }

    @Override
    public void init(PointDataRecord seedItem, int notUsed)
    {
        /* init state */

        /* init models and integer compressors */
        dec.initSymbolModel(m_byte_used);
        dec.initSymbolModel(m_rgb_diff_0);
        dec.initSymbolModel(m_rgb_diff_1);
        dec.initSymbolModel(m_rgb_diff_2);
        dec.initSymbolModel(m_rgb_diff_3);
        dec.initSymbolModel(m_rgb_diff_4);
        dec.initSymbolModel(m_rgb_diff_5);

        last_item = new PointDataRecordRGB((PointDataRecordRGB)seedItem);
    }

    @Override
    public PointDataRecord read(int notUsed) {
    int corr;
    int diff = 0;
    int sym = dec.decodeSymbol(m_byte_used);

    PointDataRecordRGB result = new PointDataRecordRGB();

    if ((sym & 1) != 0) {
        corr = dec.decodeSymbol(m_rgb_diff_0);
        result.R = (char) (U8_FOLD(corr + (last_item.R & 255)) & 0xFFFF);
    } else {
        result.R = (char) (last_item.R & 0xFF);
    }
    if ((sym & 2) != 0) {
        corr = dec.decodeSymbol(m_rgb_diff_1);
        result.R |= ((char) U8_FOLD(corr + (last_item.R >>> 8))) << 8;
    } else {
        result.R |= (last_item.R & 0xFF00);
    }
    if ((sym & 64) != 0) {
        diff = (result.R & 0xFF) - (last_item.R & 0xFF);
        if ((sym & 4) != 0) {
            corr = dec.decodeSymbol(m_rgb_diff_2);
            result.G = (char) U8_FOLD(corr + U8_CLAMP(diff + (last_item.G & 255)));
        } else {
            result.G = (char) (last_item.G & 0xFF);
        }
        if ((sym & 16) != 0) {
            corr = dec.decodeSymbol(m_rgb_diff_4);
            diff = (diff + ((result.G & 0xFF) - (last_item.G & 0xFF))) / 2;
            result.B = (char) U8_FOLD(corr + U8_CLAMP(diff + (last_item.B & 255)));
        } else {
            result.B = (char) (last_item.B & 0xFF);
        }
        diff = (result.R >>> 8) - (last_item.R >>> 8);
        if ((sym & 8) != 0) {
            corr = dec.decodeSymbol(m_rgb_diff_3);
            result.G |= ((char) U8_FOLD(corr + U8_CLAMP(diff + (last_item.G >>> 8)))) << 8;
        } else {
            result.G |= (last_item.G & 0xFF00);
        }
        if ((sym & 32) != 0) {
            corr = dec.decodeSymbol(m_rgb_diff_5);
            diff = (diff + ((result.G >>> 8) - (last_item.G >>> 8))) / 2;
            result.B |= ((char) U8_FOLD(corr + U8_CLAMP(diff + (last_item.B >>> 8)))) << 8;
        } else {
            result.B |= (last_item.B & 0xFF00);
        }
    } else {
        result.G = result.R;
        result.B = result.R;
    }

    last_item.R = result.R;
    last_item.G = result.G;
    last_item.B = result.B;

    return result;
}


    @Override
    public boolean chunk_sizes() {
        return false;
    }
}
