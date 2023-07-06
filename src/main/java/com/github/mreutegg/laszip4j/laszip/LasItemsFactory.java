/*
 * Copyright 2022 Marcel Reutegger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mreutegg.laszip4j.laszip;

import java.security.InvalidParameterException;
import java.util.ArrayList;

public class LasItemsFactory {
  
   public static LASitem[] getItems(byte pointFormat, char pointRecordSize, char compressorType) {
    ArrayList<LASitem> itemsList = new ArrayList<LASitem>();
    int extraByteSize = 0;
    switch (pointFormat) {
        case 0:
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
            itemsList.add(LASitem.Point10(compressorType == 0 ? 0 : 2));
            if (pointFormat >= 1) {
                itemsList.add(LASitem.GpsTime11(compressorType == 0 ? 0 : 2));
            }
            if (pointFormat >= 2) {
                itemsList.add(LASitem.Rgb12(compressorType == 0 ? 0 : 2));
            }
            if (pointFormat >= 4) {
                itemsList.add(LASitem.WavePacket13(compressorType == 0 ? 0 : 1));
            }
            extraByteSize = pointRecordSize - (20 + 8 * pointFormat);
            break;
        case 6:
        case 7:
        case 8:
        case 9:
        case 10:
            itemsList.add(LASitem.Point14(compressorType == 0 ? 0 : 3));
            if (pointFormat >= 7) {
                itemsList.add(LASitem.Rgb14(compressorType == 0 ? 0 : 3));
            }
            if (pointFormat >= 8) {
                itemsList.add(LASitem.RgbNIR14(compressorType == 0 ? 0 : 3));
            }
            if (pointFormat >= 9) {
                itemsList.add(LASitem.WavePacket14(compressorType == 0 ? 0 : 3));
            }
            extraByteSize = pointRecordSize - (30 + 6 * (pointFormat - 6));
            break;
        default:
            throw new InvalidParameterException("Unknown point format");
    }

    if (extraByteSize > 0) {
        if (pointFormat < 6) {
            itemsList.add(LASitem.ExtraBytes(extraByteSize, compressorType == 0 ? 0 : (pointFormat > 5 ? 3 : 2)));
        } else {
            itemsList.add(LASitem.ExtraBytes14(extraByteSize, compressorType == 0 ? 0 : (pointFormat > 5 ? 3 : 2)));
        }
    }

    return itemsList.toArray(new LASitem[0]);
}

}
