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

class LAScontextPOINT14
{
  private boolean unused;

  private PointDataRecordPoint14 Last_item = null;
  private int[] Last_intensity = new int[8];
  private StreamingMedian5[] Last_X_diff_median5 = StreamingMedian5.newStreamingMedian5(12);
  private StreamingMedian5[] Last_Y_diff_median5 = StreamingMedian5.newStreamingMedian5(12);
  private long[] Last_Z = new long[8];

  private boolean initialized = false;
  private ArithmeticModel[] M_changed_Values = new ArithmeticModel[8];
  private ArithmeticModel M_scanner_Channel;
  private ArithmeticModel[] M_number_of_returns = new ArithmeticModel[16];
  private ArithmeticModel M_return_number_gps_same;
  private ArithmeticModel[] M_return_number = new ArithmeticModel[16];
  private IntegerCompressor ic_DX;
  private IntegerCompressor ic_DY;
  private IntegerCompressor ic_DZ;

  private ArithmeticModel[] M_classification = new ArithmeticModel[64];

  private ArithmeticModel[] M_flags = new ArithmeticModel[64];

  private ArithmeticModel[] M_user_data = new ArithmeticModel[64];

  private IntegerCompressor ic_Intensity;

  private IntegerCompressor ic_Scan_Angle;

  private IntegerCompressor ic_Point_Source_ID;

  // GPS time stuff
  private int last;
  private int next;
  private long[] Last_Gpstime = new long[4];
  private int[] Last_Gpstime_Diff = new int[4];
  private int[] Multi_Extreme_Counter = new int[4];

  private ArithmeticModel M_gpstime_Multi;
  private ArithmeticModel M_gpstime_0diff;
  private IntegerCompressor Ic_gpstime;
}
