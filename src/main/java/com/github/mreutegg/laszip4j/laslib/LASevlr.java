/*
 * Copyright 2005-2014, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laslib;

public class LASevlr {

    private static final char RESERVED = 'X'; 
    private static final byte[] USER_ID = new byte[16]; 
    private static final char Record_Id = 'Y';
    private static final long record_length_after_header = -1;
    public byte[] description = new byte[32];
    public byte[] data;
}
