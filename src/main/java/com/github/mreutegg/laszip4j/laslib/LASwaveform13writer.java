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
package com.github.mreutegg.laszip4j.laslib;

import com.github.mreutegg.laszip4j.laszip.ArithmeticEncoder;
import com.github.mreutegg.laszip4j.laszip.ByteStreamOut;
import com.github.mreutegg.laszip4j.laszip.ByteStreamOutFile;
import com.github.mreutegg.laszip4j.laszip.IntegerCompressor;
import com.github.mreutegg.laszip4j.laszip.LASpoint;

import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fclose;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fopenRAF;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdio.sprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strlen;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_VERSION;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.asByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASwaveform13writer {

    private static final PrintStream stderr = logger.severe;

    private LASwaveformDescription[] waveforms;
    private RandomAccessFile file;
    private ByteStreamOut stream;

    private ArithmeticEncoder enc;
    private IntegerCompressor ic8;
    private IntegerCompressor ic16;

    LASwaveform13writer()
    {
        waveforms = null;
        file = null;
        stream = null;
        enc = null;
        ic8 = null;
        ic16 = null;
    }

    boolean open(String file_name, LASvlr_wave_packet_descr[] wave_packet_descr) {
        if (file_name == null) {
            logger.error("ERROR: file name pointer is null");
            return false;
        }
    
        if (wave_packet_descr == null) {
            logger.error("ERROR: wave packet descriptor pointer is null");
            return false;
        }
    
        LASwaveformDescription[] waveforms = new LASwaveformDescription[256];
        int number = 0;
    
        for (LASvlr_wave_packet_descr descr : wave_packet_descr) {
            if (descr != null) {
                int index = descr.getIndex();
                waveforms[index] = new LASwaveformDescription();
                waveforms[index].setCompression(descr.getCompressionType());
                waveforms[index].setNbits(descr.getBitsPerSample());
                waveforms[index].setNsamples((char) descr.getNumberOfSamples());
                number++;
            }
        }
    
        boolean compressed = false;
        for (LASwaveformDescription waveform : waveforms) {
            if (waveform != null && waveform.getCompression() > 0) {
                compressed = true;
                break;
            }
        }
    
    }
    
    public boolean write_waveform(LASpoint point, byte[] samples) {
.
    }
        void close() {
    }
    
    {
        if (stream.isSeekable())
        {
            long record_length_after_header = stream.tell();
            record_length_after_header -= 60;
            stream.seek(18);
            if (!stream.put64bitsLE(record_length_after_header))
            {
                fprintf(stderr,"ERROR: updating EVLR record_length_after_header\n");
            }
            stream.seekEnd();
        }
        if (stream != null)
        {
            fclose(stream);
            stream = null;
        }
        if (file != null)
        {
            fclose(file);
            file = null;
        }
    }
}
