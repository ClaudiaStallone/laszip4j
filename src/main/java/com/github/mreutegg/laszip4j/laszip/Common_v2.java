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

private final class Common_v2 {

    static final byte[][] number_return_map =
    {
        { 15, 14, 13, 12, 11, 10,  9,  8 },
        { 14,  0,  1,  3,  6, 10, 10,  9 },
        { 13,  1,  2,  4,  7, 11, 11, 10 },
        { 12,  3,  4,  5,  8, 12, 12, 11 },
        { 11,  6,  7,  8,  9, 13, 13, 12 },
        { 10, 10, 11, 12, 13, 14, 14, 13 },
        {  9, 10, 11, 12, 13, 14, 15, 14 },
        {  8,  9, 10, 11, 12, 13, 14, 15 }
    };

    static final byte[][] number_return_level =
    {
        {  0,  1,  2,  3,  4,  5,  6,  7 },
        {  1,  0,  1,  2,  3,  4,  5,  6 },
        {  2,  1,  0,  1,  2,  3,  4,  5 },
        {  3,  2,  1,  0,  1,  2,  3,  4 },
        {  4,  3,  2,  1,  0,  1,  2,  3 },
        {  5,  4,  3,  2,  1,  0,  1,  2 },
        {  6,  5,  4,  3,  2,  1,  0,  1 },
        {  7,  6,  5,  4,  3,  2,  1,  0 }
    };
}
