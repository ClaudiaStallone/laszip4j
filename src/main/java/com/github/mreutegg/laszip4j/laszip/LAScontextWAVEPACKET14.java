/*
  COPYRIGHT:

    (c) 2007-2022, rapidlasso GmbH - fast tools to catch reality

    This is free software; you can redistribute and/or modify it under the
    terms of the Apache Public License 2.0 published by the Apache Software
    Foundation. See the COPYING file for more information.

    This software is distributed WITHOUT ANY WARRANTY and without even the
    implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

class LAScontextWAVEPACKET14
{
  private boolean Unused;

  private PointDataRecordWavepacket Last_Item;
  private int last_Diff_32;
  private int sym_Last_Offset_diff;

  private ArithmeticModel M_packet_index;
  private ArithmeticModel[] M_offset_diff = new ArithmeticModel[4];
  private IntegerCompressor iC_ofFset_diff;
  private IntegerCompressor ic_paCket_size;
  private IntegerCompressor ic_Return_point;
  private IntegerCompressor ic_XYZ;
}
